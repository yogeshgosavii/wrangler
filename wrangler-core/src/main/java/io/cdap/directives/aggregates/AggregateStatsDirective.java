/*
 * Copyright © 2017-2019 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package io.cdap.directives.aggregates;

import io.cdap.cdap.api.annotation.Description;
import io.cdap.cdap.api.annotation.Name;
import io.cdap.wrangler.api.Arguments;
import io.cdap.wrangler.api.Directive;
import io.cdap.wrangler.api.DirectiveExecutionException;
import io.cdap.wrangler.api.DirectiveParseException;
import io.cdap.wrangler.api.ErrorRowException;
import io.cdap.wrangler.api.ExecutorContext;
import io.cdap.wrangler.api.Row;
import io.cdap.wrangler.api.TransientVariableScope;
import io.cdap.wrangler.api.parser.ByteSize;
import io.cdap.wrangler.api.parser.ColumnName;
import io.cdap.wrangler.api.parser.Text;
import io.cdap.wrangler.api.parser.TimeDuration;
import io.cdap.wrangler.api.parser.TokenType;
import io.cdap.wrangler.api.parser.UsageDefinition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

 
 
/**
 * This class represents the AggregateStatsDirective for processing aggregates in the data pipeline.
 */

@Name("aggregate-stats") 
@Description("Performs aggregation statistics")
 public class AggregateStatsDirective implements Directive {
     private String sizeColumn;
     private String timeColumn;
     private String sizeOutputColumn;
     private String timeOutputColumn;
     private String sizeOutputUnit = "MB";
     private String timeOutputUnit = "s";
     private String aggregationType = "total";
    private static final String AGGREGATE_STATS_NAME = "aggregate-stats";
 
     @Override
     public UsageDefinition define() {
         UsageDefinition.Builder builder = UsageDefinition.builder(AGGREGATE_STATS_NAME);
         builder.define("size_column", TokenType.COLUMN_NAME);
         builder.define("time_column", TokenType.COLUMN_NAME);
         builder.define("size_output", TokenType.COLUMN_NAME);
         builder.define("time_output", TokenType.COLUMN_NAME);
         
         builder.define("size_unit", TokenType.TEXT);
         builder.define("time_unit", TokenType.TEXT); 
         builder.define("aggregation", TokenType.TEXT);
         
         return builder.build();
     }
 
     @Override
     public void initialize(Arguments args) throws DirectiveParseException {
         this.sizeColumn = ((ColumnName) args.value("size_column")).value();
         this.timeColumn = ((ColumnName) args.value("time_column")).value();
         this.sizeOutputColumn = ((ColumnName) args.value("size_output")).value();
         this.timeOutputColumn = ((ColumnName) args.value("time_output")).value();

         if (args.contains("size_unit")) {
             this.sizeOutputUnit = ((Text) args.value("size_unit")).value();
             validateSizeUnit(this.sizeOutputUnit);
         }
         if (args.contains("time_unit")) {
             this.timeOutputUnit = ((Text) args.value("time_unit")).value();
             validateTimeUnit(this.timeOutputUnit);
         }
         if (args.contains("aggregation")) {
             this.aggregationType = ((Text) args.value("aggregation")).value();
             if (!"total".equalsIgnoreCase(aggregationType) && !"average".equalsIgnoreCase(aggregationType)) {
                 throw new DirectiveParseException(
                     "Invalid aggregation type. Must be 'total' or 'average'");
             }
         }
     }
 
     @Override
     public List<Row> execute(List<Row> rows, ExecutorContext context) 
         throws DirectiveExecutionException, ErrorRowException {
         
         // Initialize
         if (context.getTransientStore().get("total_bytes") == null) {
             context.getTransientStore().set(TransientVariableScope.LOCAL, "total_bytes", 0L);
             context.getTransientStore().set(TransientVariableScope.LOCAL, "total_nanos", 0L);
             context.getTransientStore().set(TransientVariableScope.LOCAL, "row_count", 0);
           
         }
     
         // Process each row
         for (Row row : rows) {
             try {
                 // Handle size value (e.g. "10MB")
                 Object sizeValue = row.getValue(sizeColumn);
                 if (sizeValue != null) {
                     ByteSize byteSize = new ByteSize(sizeValue.toString());
                     
                     // Auto-detect unit from first value if not specified
                     if (sizeOutputUnit == null) {
                         sizeOutputUnit = byteSize.getOriginalUnit(); // Preserve input unit
                     }
                     
                     context.getTransientStore().increment(TransientVariableScope.LOCAL, 
                        "total_bytes",
                        byteSize.getBytes());
                 }
     
                 // Handle time value (e.g. "100ms")
                 Object timeValue = row.getValue(timeColumn);
                 if (timeValue != null) {
                     TimeDuration timeDuration = new TimeDuration(timeValue.toString());
                     
                     if (timeOutputUnit == null) {
                         timeOutputUnit = timeDuration.getOriginalUnit(); // Preserve input unit
                     }
                     
                     context.getTransientStore().increment(TransientVariableScope.LOCAL, 
                        "total_nanos", 
                        timeDuration.getNanos());
                 }
                 
                 context.getTransientStore().increment(TransientVariableScope.LOCAL, "row_count", 1);
                 
             } catch (IllegalArgumentException e) {
                 throw new ErrorRowException(AGGREGATE_STATS_NAME, "Invalid value format: " + e.getMessage(), 1);
             }
         }
         
         return new ArrayList<>();
     }

     public List<Row> finalize(ExecutorContext context) throws DirectiveExecutionException {
         // Get final totals
         long totalBytes = (Long) context.getTransientStore().get("total_bytes");
         long totalNanos = (Long) context.getTransientStore().get("total_nanos");
         long rowCount = (Long) context.getTransientStore().get("row_count");
 
         // Calculate averages if needed
         if ("average".equalsIgnoreCase(aggregationType)) {
             totalBytes = rowCount > 0 ? totalBytes / rowCount : 0;
             totalNanos = rowCount > 0 ? totalNanos / rowCount : 0;
         }
 
         // Convert to requested units
         double convertedSize = convertBytes(totalBytes, sizeOutputUnit);
         double convertedTime = convertNanos(totalNanos, timeOutputUnit);
 
         // Create result row
         Row result = new Row();
         result.add(sizeOutputColumn, convertedSize);
         result.add(timeOutputColumn, convertedTime);
 
         return Collections.singletonList(result);
     }
 
     private double convertBytes(long bytes, String unit) {
         switch (unit.toUpperCase()) {
             case "B": return bytes;
             case "KB": return bytes / 1024.0;
             case "MB": return bytes / (1024.0 * 1024);
             case "GB": return bytes / (1024.0 * 1024 * 1024);
             case "TB": return bytes / (1024.0 * 1024 * 1024 * 1024);
             case "PB": return bytes / (1024.0 * 1024 * 1024 * 1024 * 1024);
             default: throw new IllegalArgumentException("Invalid byte unit: " + unit);
         }
     }
 
     private double convertNanos(long nanos, String unit) {
         switch (unit.toLowerCase()) {
             case "ns": return nanos;
             case "μs": case "us": return nanos / 1000.0;
             case "ms": return nanos / 1_000_000.0;
             case "s": return nanos / 1_000_000_000.0;
             case "m": return nanos / (60.0 * 1_000_000_000);
             case "h": return nanos / (3600.0 * 1_000_000_000);
             case "d": return nanos / (86400.0 * 1_000_000_000);
             default: throw new IllegalArgumentException("Invalid time unit: " + unit);
         }
     }
 
     private void validateSizeUnit(String unit) throws DirectiveParseException {
         try {
             convertBytes(1L, unit);
         } catch (IllegalArgumentException e) {
             throw new DirectiveParseException(
                 "Invalid size unit '" + unit + "'. Valid units are: B, KB, MB, GB, TB, PB");
         }
     }
 
     private void validateTimeUnit(String unit) throws DirectiveParseException {
         try {
             convertNanos(1L, unit);
         } catch (IllegalArgumentException e) {
             throw new DirectiveParseException(
                 "Invalid time unit '" + unit + "'. Valid units are: ns, μs/us, ms, s, m, h, d");
         }
     }
 
     @Override
     public void destroy() {
         // Clean up resources if needed
     }
 }

