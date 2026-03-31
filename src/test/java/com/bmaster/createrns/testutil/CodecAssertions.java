package com.bmaster.createrns.testutil;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import org.junit.jupiter.api.Assertions;

public class CodecAssertions {
    public static <T> T assertParses(Codec<T> codec, DynamicOps<JsonElement> ops, String json) {
        var result = parse(codec, ops, json);
        result.error().ifPresent(error -> Assertions.fail("Expected parse success, got: " + error.message()));
        return result.result().orElseThrow();
    }

    public static <T> String assertFails(Codec<T> codec, DynamicOps<JsonElement> ops, String json, String expectedMessagePart) {
        var message = "";

        try {
            var result = parse(codec, ops, json);
            var error = result.error().orElse(null);
            if (error == null) Assertions.fail("Expected parse failure, but parsing succeeded");
            message = error.message();
        } catch (RuntimeException e) {
            message = findMessage(e);
        }

        var finalMessage = message;
        Assertions.assertTrue(finalMessage.contains(expectedMessagePart),
                () -> "Expected error containing '" + expectedMessagePart + "', got: " + finalMessage);
        return message;
    }

    public static <T> DataResult<T> parse(Codec<T> codec, DynamicOps<JsonElement> ops, String json) {
        return codec.parse(ops, parseJson(json));
    }

    public static JsonElement parseJson(String json) {
        return JsonParser.parseString(json);
    }

    private static String findMessage(Throwable throwable) {
        var current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return String.valueOf(current.getMessage());
    }
}
