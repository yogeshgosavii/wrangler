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
 * Token representing a time duration (e.g., 5s, 10min, 2h).
 */

public class TimeDuration implements Token {
    private final long nanos;
    String unit;

    public TimeDuration(String value) {
        String num = value.replaceAll("[^0-9.]", "");
        unit = value.replaceAll("[0-9.]", "").toLowerCase();
        
        double number = Double.parseDouble(num);
        this.nanos = (long) (number * getMultiplier(unit));
    }

    private long getMultiplier(String unit) {
        switch(unit) {
            case "ns": return 1L;
            case "μs": case "us": return 1000L;
            case "ms": return 1_000_000L;
            case "s": return 1_000_000_000L;
            case "m": return 60L * 1_000_000_000;
            case "h": return 3600L * 1_000_000_000;
            case "d": return 86400L * 1_000_000_000;
            default: throw new IllegalArgumentException("Invalid time unit: " + unit);
        }
    }

    public long getNanos() {
        return nanos;
    }

    @Override
    public Object value() {
        return nanos;
    }

    @Override
    public TokenType type() {
        return TokenType.TIME_DURATION;
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(nanos);
    }

    @Override
    public String toString() {
        return "TimeDuration{" + "nanos=" + nanos + '}';
    }

    public String getOriginalUnit() {
       return unit;
    }
}
