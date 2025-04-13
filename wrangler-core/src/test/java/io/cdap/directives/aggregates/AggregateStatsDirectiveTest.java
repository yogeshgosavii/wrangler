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

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import io.cdap.wrangler.api.Arguments;
import io.cdap.wrangler.api.ExecutorContext;
import io.cdap.wrangler.api.Row;
import io.cdap.wrangler.api.TransientStore;
import io.cdap.wrangler.api.TransientVariableScope;
import io.cdap.wrangler.api.parser.ColumnName;
import io.cdap.wrangler.api.parser.Text;
import io.cdap.wrangler.api.parser.TokenType;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AggregateStatsDirectiveTest {
    private AggregateStatsDirective directive;
    private ExecutorContext context;
    private TransientStore store;

    @Before
    public void setup() {
        directive = new AggregateStatsDirective();
        context = mock(ExecutorContext.class);
        store = new TransientStoreMock();
        when(context.getTransientStore()).thenReturn(store);
    }

    @Test
    public void testTotalAggregation() throws Exception {
        Arguments args = mock(Arguments.class);

        // Create appropriate Token objects
        ColumnName sizeColumn = new ColumnName("size");
        ColumnName timeColumn = new ColumnName("duration");
        ColumnName sizeOutput = new ColumnName("totalSize");
        ColumnName timeOutput = new ColumnName("totalTime");
        Text aggregation = new Text("total");

        // Mock args.value() to return the correct Token types
        when(args.value("size_column")).thenReturn(sizeColumn);
        when(args.value("time_column")).thenReturn(timeColumn);
        when(args.value("size_output")).thenReturn(sizeOutput);
        when(args.value("time_output")).thenReturn(timeOutput);
        when(args.contains("size_unit")).thenReturn(false);
        when(args.contains("time_unit")).thenReturn(false);
        when(args.contains("aggregation")).thenReturn(true);
        when(args.value("aggregation")).thenReturn(aggregation);

        directive.initialize(args);

        // Set up the input data
        List<Row> inputRows = Arrays.asList(
                new Row("size", "10MB").add("duration", "10s"),
                new Row("size", "15MB").add("duration", "5s"));

        // Execute the directive
        directive.execute(inputRows, context);
        List<Row> result = directive.finalize(context);

        // Assertions
        assertEquals(1, result.size());
        Row output = result.get(0);

        // Verify that the result is aggregated correctly
        assertEquals(25.0, output.getValue("totalSize"));
        assertEquals(15.0, output.getValue("totalTime"));
    }


    @Test
    public void testAverageAggregation() throws Exception {
        // Mock the Arguments object
        Arguments args = mock(Arguments.class);

        // Define tokens with correct column names
        ColumnName sizeColumn = new ColumnName("size");
        ColumnName timeColumn = new ColumnName("duration");
        ColumnName sizeOutput = new ColumnName("totalSize");
        ColumnName timeOutput = new ColumnName("totalTime");
        Text aggregation = new Text("average");  // Change to "average" for correct aggregation

        // Mock the values returned by the Arguments
        when(args.value("size_column")).thenReturn(sizeColumn);
        when(args.value("time_column")).thenReturn(timeColumn);
        when(args.value("aggregation")).thenReturn(aggregation);
        when(args.value("size_output")).thenReturn(sizeOutput);
        when(args.value("time_output")).thenReturn(timeOutput);
        when(args.contains("aggregation")).thenReturn(true);
        when(args.contains("size_unit")).thenReturn(false);
        when(args.contains("time_unit")).thenReturn(false);

        // Initialize the directive with the mocked arguments
        directive.initialize(args);

        // Sample input with "size" in MB and "duration" in seconds
        List<Row> inputRows = Arrays.asList(
            new Row("size", "8MB").add("duration", "4s"),
            new Row("size", "12MB").add("duration", "2s")
        );

        // Execute directive logic
        directive.execute(inputRows, context);
        List<Row> result = directive.finalize(context);

        // Assert the output
        assertEquals(1, result.size());
        Row output = result.get(0);

        // Assert that the calculated averages are correct
        assertEquals(10.0, (double) output.getValue("totalSize"), 0.001);  // (8 + 12) / 2 = 10
        assertEquals(3.0, (double) output.getValue("totalTime"), 0.001);   // (4 + 2) / 2 = 3
    }

    // Mock TransientStore implementation
    private static class TransientStoreMock implements TransientStore {
        private final Map<String, Object> store = new HashMap<>();

        @Override
        public void set(TransientVariableScope scope, String key, Object value) {
            store.put(key, value);
        }

        @Override
        public Object get(String key) {
            return store.get(key);
        }

        @Override
        public void increment(TransientVariableScope scope, String key, long delta) {
            store.put(key, ((Number) store.getOrDefault(key, 0L)).longValue() + delta);
        }

        @Override
        public void reset(TransientVariableScope scope) {
            throw new UnsupportedOperationException("Unimplemented method 'reset'");
        }

        @Override
        public Set<String> getVariables() {
            throw new UnsupportedOperationException("Unimplemented method 'getVariables'");
        }
    }

    public class CustomToken extends ColumnName {
        private final String value;
    
        public CustomToken(String value) {
            super(value); // Pass value to the parent constructor
            this.value = value;
        }
    
        public String getValue() {
            return value;
        }
    
        @Override
        public TokenType type() {
            return TokenType.TEXT; // Adjust as needed
        }
    
        @Override
        public JsonElement toJson() {
            return new JsonPrimitive(value);
        }
    }
}
