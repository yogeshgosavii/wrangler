
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

package io.cdap.wrangler.api.parser;

 
import org.junit.Assert;
import org.junit.Test;

public class ByteSizeTest {

    @Test
    public void testByteSizeParsing() throws Exception {
        // Test various valid formats
        assertParsing("10KB", 10240L, "KB");
        assertParsing("1.5MB", 1572864L, "MB");
        assertParsing("2GB", 2147483648L, "GB");
        assertParsing("0.5TB", 549755813888L, "TB");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidByteSize() {
        new ByteSize("10XB"); // Invalid unit
    }

    private void assertParsing(String input, long expectedBytes, String expectedUnit) {
        ByteSize byteSize = new ByteSize(input);
        Assert.assertEquals(expectedBytes, byteSize.getBytes());
        Assert.assertEquals(expectedUnit, byteSize.getOriginalUnit());
    }
}

