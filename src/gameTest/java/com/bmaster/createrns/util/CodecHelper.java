package com.bmaster.createrns.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.RegistryOps;

public class CodecHelper {
    public static DynamicOps<JsonElement> json() {
        return JsonOps.INSTANCE;
    }

    public static DynamicOps<JsonElement> registries(GameTestHelper helper) {
        return RegistryOps.create(JsonOps.INSTANCE, helper.getLevel().registryAccess());
    }

    public static <T> T assertParses(
            GameTestHelper helper, Codec<T> codec, DynamicOps<JsonElement> ops, String json, String valueName
    ) {
        var result = parse(codec, ops, json);
        var error = result.error().orElse(null);
        helper.assertTrue(error == null,
                "Expected " + valueName + " parse success, got: " + errorMessage(result));
        var parsed = result.result().orElse(null);
        helper.assertTrue(parsed != null,
                "Expected " + valueName + " parse success, got: " + errorMessage(result));
        return parsed;
    }

    public static <T> String assertFails(
            GameTestHelper helper, Codec<T> codec, DynamicOps<JsonElement> ops, String json, String expectedMessagePart
    ) {
        var message = "";

        try {
            var result = parse(codec, ops, json);
            var error = result.error().orElse(null);
            helper.assertTrue(error != null, "Expected parse failure, but parsing succeeded");
            message = error.message();
        } catch (RuntimeException e) {
            message = findMessage(e);
        }

        var finalMessage = message;
        helper.assertTrue(finalMessage.contains(expectedMessagePart),
                "Expected error containing '" + expectedMessagePart + "', got: " + finalMessage);
        return message;
    }

    public static <T> DataResult<T> parse(Codec<T> codec, DynamicOps<JsonElement> ops, String json) {
        return codec.parse(ops, parseJson(json));
    }

    public static JsonElement parseJson(String json) {
        return JsonParser.parseString(json);
    }

    public static <T> T assertInstanceOf(GameTestHelper helper, Class<T> expectedType, Object value, String valueName) {
        helper.assertTrue(expectedType.isInstance(value),
                "Expected " + valueName + " to be " + expectedType.getSimpleName() + ", but was "
                        + ((value != null) ? value.getClass().getSimpleName() : "null"));
        return expectedType.cast(value);
    }

    public static void assertSame(GameTestHelper helper, Object expected, Object actual, String valueName) {
        helper.assertTrue(actual == expected,
                "Expected " + valueName + " to be " + expected + ", but was " + actual);
    }

    public static void assertFloat(GameTestHelper helper, float actual, float expected, String valueName) {
        helper.assertTrue(Math.abs(actual - expected) < 0.0001f,
                "Expected " + valueName + " to be " + expected + ", but was " + actual);
    }

    public static void assertValueEqual(GameTestHelper helper, Object actual, Object expected, String valueName) {
        helper.assertTrue((actual == null) ? expected == null : actual.equals(expected),
                "Expected " + valueName + " to be " + expected + ", but was " + actual);
    }

    private static String errorMessage(DataResult<?> result) {
        var error = result.error().orElse(null);
        return (error != null) ? error.message() : "unknown codec error";
    }

    private static String findMessage(Throwable throwable) {
        var current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return String.valueOf(current.getMessage());
    }
}
