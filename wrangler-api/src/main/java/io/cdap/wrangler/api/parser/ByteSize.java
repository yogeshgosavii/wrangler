package io.cdap.wrangler.api.parser;

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

 

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/**
 * Token representing a byte size (e.g., 1MB, 2GB, etc.).
 */

public class ByteSize implements Token {
    private final long bytes;
    String unit;

    public ByteSize(String value) {
        String num = value.replaceAll("[^0-9.]", "");
        unit = value.replaceAll("[0-9.]", "").toUpperCase();
        
        double number = Double.parseDouble(num);
        this.bytes = (long) (number * getMultiplier(unit));
    }

    private long getMultiplier(String unit) {
        switch(unit) {
            case "B": return 1L;
            case "KB": return 1024L;
            case "MB": return 1024L * 1024;
            case "GB": return 1024L * 1024 * 1024;
            case "TB": return 1024L * 1024 * 1024 * 1024;
            case "PB": return 1024L * 1024 * 1024 * 1024 * 1024;
            default: throw new IllegalArgumentException("Invalid byte unit: " + unit);
        }
    }

    public long getBytes() {
        return bytes;
    }

    @Override
    public Object value() {
        return bytes;
    }

    @Override
    public TokenType type() {
        return TokenType.BYTE_SIZE;
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(bytes);
    }

    @Override
    public String toString() {
        return "ByteSize{" + "bytes=" + bytes + '}';
    }

    public String getOriginalUnit() {
        return unit;
    }
}
