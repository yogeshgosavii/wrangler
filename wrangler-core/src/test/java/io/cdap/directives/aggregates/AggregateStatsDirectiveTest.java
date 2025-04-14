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
import io.cdap.wrangler.api.parser.Token;
import io.cdap.wrangler.api.parser.TokenType;

import org.junit.Before;
import org.junit.Test;


import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
        // Use actual token objects
        Map<String, Object> params = new HashMap<>();
        params.put("size_column", new ColumnName("size"));
        params.put("time_column", new ColumnName("duration"));
        params.put("size_output", new ColumnName("totalSize"));
        params.put("time_output", new ColumnName("totalTime"));
        params.put("aggregation", new Text("total"));

        Arguments args = new TestArguments(params);
        directive.initialize(args);

        List<Row> inputRows = Arrays.asList(
            new Row("size", "10MB").add("duration", "10s"),
            new Row("size", "15MB").add("duration", "5s")
        );

        directive.execute(inputRows, context);
        List<Row> result = directive.finalize(context);

        assertEquals(1, result.size());
        Row output = result.get(0);

        assertEquals(25.0, output.getValue("totalSize"));
        assertEquals(15.0, output.getValue("totalTime"));
    }

    @Test
    public void testAverageAggregation() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("size_column", new ColumnName("size"));
        params.put("time_column", new ColumnName("duration"));
        params.put("size_output", new ColumnName("totalSize"));
        params.put("time_output", new ColumnName("totalTime"));
        params.put("aggregation", new Text("average"));

        Arguments args = new TestArguments(params);
        directive.initialize(args);

        List<Row> inputRows = Arrays.asList(
            new Row("size", "8MB").add("duration", "4s"),
            new Row("size", "12MB").add("duration", "2s")
        );

        directive.execute(inputRows, context);
        List<Row> result = directive.finalize(context);

        assertEquals(1, result.size());
        Row output = result.get(0);

        assertEquals(10.0, (double) output.getValue("totalSize"), 0.001);
        assertEquals(3.0, (double) output.getValue("totalTime"), 0.001);
    }

    // Custom Arguments stub to avoid mocking
    private static class TestArguments implements Arguments {
        private final Map<String, Token> values;
    
        public TestArguments(Map<String, Object> rawValues) {
            this.values = new HashMap<>();
            for (Map.Entry<String, Object> entry : rawValues.entrySet()) {
                Object value = entry.getValue();
                if (value instanceof Token) {
                    values.put(entry.getKey(), (Token) value);
                } else if (value instanceof String) {
                    values.put(entry.getKey(), new Text((String) value));
                } else {
                    throw new IllegalArgumentException("Unsupported token type: " + value.getClass());
                }
            }
        }
    
        @Override
        public boolean contains(String name) {
            return values.containsKey(name);
        }
    
        @Override
        public <T extends Token> T value(String name) {
            return (T) values.get(name);
        }
    
    
        @Override
        public int size() {
            return values.size();
        }
    
        @Override
        public TokenType type(String name) {
            Token token = values.get(name);
            return token != null ? token.type() : null;
        }
    
        @Override
        public int line() {
            return 0;
        }
    
        @Override
        public int column() {
            return 0;
        }
    
        @Override
        public String source() {
            return "TestArguments";
        }
    
        @Override
        public JsonElement toJson() {
            return null; // not needed for the test
        }
    }
    
    // Simple mock of TransientStore
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
            super(value);
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        @Override
        public TokenType type() {
            return TokenType.TEXT;
        }

        @Override
        public JsonElement toJson() {
            return new JsonPrimitive(value);
        }
    }
}

