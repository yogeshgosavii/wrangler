package io.cdap.wrangler.api.parser;

import org.junit.Assert;
import org.junit.Test;

/*
 * Copyright 2025 Your Organization.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */



 
// TimeDurationTest.java
public class TimeDurationTest {

    @Test
    public void testTimeDurationParsing() throws Exception {
        // Test various valid formats
        assertParsing("5ms", 5000000L, "ms");
        assertParsing("2.1s", 2100000000L, "s");
        assertParsing("100μs", 100000L, "μs");
        assertParsing("1.5m", 90000000000L, "m");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidTimeDuration() {
        new TimeDuration("10xs"); // Invalid unit
    }

    private void assertParsing(String input, long expectedNanos, String expectedUnit) {
        TimeDuration duration = new TimeDuration(input);
        Assert.assertEquals(expectedNanos, duration.getNanos());
        Assert.assertEquals(expectedUnit, duration.getOriginalUnit());
    }
}
