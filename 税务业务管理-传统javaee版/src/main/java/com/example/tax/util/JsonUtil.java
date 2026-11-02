package com.example.tax.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

public final class JsonUtil {

    private static final Gson GSON = new GsonBuilder()
            .disableHtmlEscaping()
            .serializeNulls()
            .create();

    private JsonUtil() {
    }

    public static String toJson(Object value) {
        return GSON.toJson(value);
    }

    public static Map<String, String> parseFlatObject(String json) {
        Map<String, String> result = new LinkedHashMap<>();
        if (json == null || json.trim().isEmpty()) {
            return result;
        }
        try {
            JsonElement element = JsonParser.parseString(json);
            if (!element.isJsonObject()) {
                return result;
            }
            JsonObject object = element.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                JsonElement value = entry.getValue();
                result.put(entry.getKey(), value.isJsonNull() ? "" : value.getAsString());
            }
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("表单数据 JSON 格式不合法", e);
        }
        return result;
    }

    public static BigDecimal decimalOrZero(String value) {
        if (value == null || value.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(value.trim());
    }

    /** JSON for a &lt;script type="application/json"&gt; block; Gson escapes &lt; &gt; &amp; =. */
    public static String safeJson(Object value) {
        return toJson(value)
                .replace("<", "\\u003c")
                .replace(">", "\\u003e")
                .replace("&", "\\u0026");
    }
}
