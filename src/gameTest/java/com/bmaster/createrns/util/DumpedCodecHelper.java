package com.bmaster.createrns.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.UnaryOperator;

public class DumpedCodecHelper {
    private static final Path BUILTIN_PACKS_PATH = Path.of("src", "generated", "builtin_packs");

    public static List<Path> findJsonFiles(GameTestHelper helper, String relativePath) {
        var root = builtinPacksRoot().resolve(relativePath);
        helper.assertTrue(Files.isDirectory(root), "Expected dumped resource directory to exist: " + root);

        try (var stream = Files.walk(root)) {
            return stream.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".json"))
                    .sorted(Comparator.comparing(Path::toString))
                    .toList();
        } catch (IOException e) {
            throw new RuntimeException("Failed to list dumped resources under " + root, e);
        }
    }

    public static JsonElement readJson(Path path) {
        try {
            return CodecHelper.parseJson(Files.readString(path));
        } catch (IOException e) {
            throw new RuntimeException("Failed to read dumped resource " + path, e);
        }
    }

    public static <T> void assertRoundTrips(
            GameTestHelper helper, List<Path> files, Codec<T> codec, DynamicOps<JsonElement> ops, String resourceType,
            UnaryOperator<JsonElement> sourceNormalizer
    ) {
        helper.assertTrue(!files.isEmpty(), "Expected at least one dumped " + resourceType + " resource");

        for (var path : files) {
            var sourceJson = readJson(path);
            var parseResult = codec.parse(ops, sourceJson);
            helper.assertTrue(parseResult.error().isEmpty(),
                    "Expected " + resourceType + " parse success for " + path + ", got: " + message(parseResult));
            var parsed = parseResult.result().orElse(null);
            helper.assertTrue(parsed != null,
                    "Expected " + resourceType + " parse success for " + path + ", got: " + message(parseResult));

            var encodeResult = codec.encodeStart(ops, parsed);
            helper.assertTrue(encodeResult.error().isEmpty(),
                    "Expected " + resourceType + " encode success for " + path + ", got: " + message(encodeResult));
            var encodedJson = encodeResult.result().orElse(null);
            helper.assertTrue(encodedJson != null,
                    "Expected " + resourceType + " encode success for " + path + ", got: " + message(encodeResult));

            var normalizedSource = canonicalize(sourceNormalizer.apply(sourceJson.deepCopy()));
            var normalizedEncoded = canonicalize(encodedJson);
            var diff = findFirstDifference(normalizedSource, normalizedEncoded, "$");
            helper.assertTrue(diff == null,
                    "Expected " + resourceType + " round-trip JSON to stay stable for " + path
                            + (diff != null ? " (" + diff + ")" : ""));
        }
    }

    public static JsonElement stripRootField(JsonElement element, String field) {
        if (element.isJsonObject()) {
            element.getAsJsonObject().remove(field);
        }
        return element;
    }

    public static JsonElement stripEmptyFields(JsonElement element, String... fieldNames) {
        var names = List.of(fieldNames);
        stripEmptyFieldsRecursive(element, names);
        return element;
    }

    public static JsonElement identity(JsonElement element) {
        return element;
    }

    public static void assertItemAndTagCandidatesResolve(
            GameTestHelper helper, List<Path> files, String itemField, String tagField, String candidateType
    ) {
        assertItemAndTagCandidatesResolve(helper, files, itemField, tagField, candidateType, Set.of(), Set.of());
    }

    public static void assertItemAndTagCandidatesResolve(
            GameTestHelper helper, List<Path> files, String itemField, String tagField, String candidateType,
            Set<ResourceLocation> allowedUnresolvedItems, Set<ResourceLocation> allowedUnresolvedTags
    ) {
        var itemLookup = helper.getLevel().registryAccess().lookupOrThrow(Registries.ITEM);

        for (var path : files) {
            var root = readJson(path);
            assertItemAndTagCandidatesResolve(helper, itemLookup, root, path, "$", itemField, tagField, candidateType,
                    allowedUnresolvedItems, allowedUnresolvedTags);
        }
    }

    private static Path builtinPacksRoot() {
        var cwd = Path.of("").toAbsolutePath().normalize();
        for (var current = cwd; current != null; current = current.getParent()) {
            var candidate = current.resolve(BUILTIN_PACKS_PATH);
            if (Files.isDirectory(candidate)) return candidate;
        }

        throw new IllegalStateException("Could not locate " + BUILTIN_PACKS_PATH + " from working directory " + cwd);
    }

    private static void assertItemAndTagCandidatesResolve(
            GameTestHelper helper, HolderLookup.RegistryLookup<Item> itemLookup, JsonElement element, Path file,
            String path, String itemField, String tagField, String candidateType,
            Set<ResourceLocation> allowedUnresolvedItems, Set<ResourceLocation> allowedUnresolvedTags
    ) {
        if (element.isJsonObject()) {
            var object = element.getAsJsonObject();

            if (object.has(itemField)) {
                assertItemCandidatesResolve(helper, object.get(itemField), file, path + "." + itemField, candidateType,
                        allowedUnresolvedItems);
            }

            if (object.has(tagField)) {
                assertTagCandidatesResolve(helper, itemLookup, object.get(tagField), file, path + "." + tagField,
                        candidateType, allowedUnresolvedTags);
            }

            for (var entry : object.entrySet()) {
                assertItemAndTagCandidatesResolve(helper, itemLookup, entry.getValue(), file, path + "." + entry.getKey(),
                        itemField, tagField, candidateType, allowedUnresolvedItems, allowedUnresolvedTags);
            }
            return;
        }

        if (!element.isJsonArray()) return;

        var array = element.getAsJsonArray();
        for (int i = 0; i < array.size(); i++) {
            assertItemAndTagCandidatesResolve(helper, itemLookup, array.get(i), file, path + "[" + i + "]",
                    itemField, tagField, candidateType, allowedUnresolvedItems, allowedUnresolvedTags);
        }
    }

    private static void assertItemCandidatesResolve(
            GameTestHelper helper, JsonElement element, Path file, String path, String candidateType,
            Set<ResourceLocation> allowedUnresolvedItems
    ) {
        helper.assertTrue(element.isJsonArray(),
                "Expected " + candidateType + " item candidates to be an array at " + file + " " + path);

        for (var candidate : element.getAsJsonArray()) {
            helper.assertTrue(candidate.isJsonPrimitive() && candidate.getAsJsonPrimitive().isString(),
                    "Expected " + candidateType + " item candidate to be a string at " + file + " " + path);

            var id = parseResourceLocation(candidate.getAsString(), file, path);
            var item = BuiltInRegistries.ITEM.getOptional(id).orElse(Items.AIR);
            if (allowedUnresolvedItems.contains(id)) continue;
            helper.assertTrue(item != Items.AIR,
                    "Expected " + candidateType + " item candidate " + id + " to resolve at " + file + " " + path);
        }
    }

    private static void assertTagCandidatesResolve(
            GameTestHelper helper, HolderLookup.RegistryLookup<Item> itemLookup, JsonElement element, Path file,
            String path, String candidateType, Set<ResourceLocation> allowedUnresolvedTags
    ) {
        helper.assertTrue(element.isJsonArray(),
                "Expected " + candidateType + " tag candidates to be an array at " + file + " " + path);

        for (var candidate : element.getAsJsonArray()) {
            helper.assertTrue(candidate.isJsonPrimitive() && candidate.getAsJsonPrimitive().isString(),
                    "Expected " + candidateType + " tag candidate to be a string at " + file + " " + path);

            var id = parseResourceLocation(candidate.getAsString(), file, path);
            var tag = TagKey.create(Registries.ITEM, id);
            var values = itemLookup.get(tag).orElse(null);
            if (allowedUnresolvedTags.contains(id)) continue;
            helper.assertTrue(values != null && values.stream().findFirst().isPresent(),
                    "Expected " + candidateType + " tag candidate " + id + " to resolve at " + file + " " + path);
        }
    }

    private static ResourceLocation parseResourceLocation(String input, Path file, String path) {
        try {
            return ResourceLocation.parse(input);
        } catch (RuntimeException e) {
            throw new IllegalStateException("Invalid resource location " + input + " at " + file + " " + path, e);
        }
    }

    private static void stripEmptyFieldsRecursive(JsonElement element, List<String> fieldNames) {
        if (element.isJsonObject()) {
            var object = element.getAsJsonObject();
            var toRemove = new ArrayList<String>();

            for (var entry : object.entrySet()) {
                var value = entry.getValue();
                if (fieldNames.contains(entry.getKey()) && value.isJsonArray() && value.getAsJsonArray().isEmpty()) {
                    toRemove.add(entry.getKey());
                    continue;
                }

                stripEmptyFieldsRecursive(value, fieldNames);
            }

            for (var key : toRemove) {
                object.remove(key);
            }
            return;
        }

        if (!element.isJsonArray()) return;

        for (var child : element.getAsJsonArray()) {
            stripEmptyFieldsRecursive(child, fieldNames);
        }
    }

    private static JsonElement canonicalize(JsonElement element) {
        if (element.isJsonObject()) {
            var input = element.getAsJsonObject();
            var output = new JsonObject();
            input.entrySet().stream()
                    .sorted(java.util.Map.Entry.comparingByKey())
                    .forEach(entry -> output.add(entry.getKey(), canonicalize(entry.getValue())));
            return output;
        }

        if (element.isJsonArray()) {
            var output = new JsonArray();
            for (var child : element.getAsJsonArray()) {
                output.add(canonicalize(child));
            }
            return output;
        }

        return element.deepCopy();
    }

    private static String message(DataResult<?> result) {
        var error = result.error().orElse(null);
        return (error != null) ? error.message() : "unknown codec error";
    }

    private static String findFirstDifference(JsonElement source, JsonElement encoded, String path) {
        if (source == null || encoded == null) {
            return (source == encoded) ? null : path + ": one side is null";
        }

        if (source.getClass() != encoded.getClass()) {
            return path + ": type mismatch " + describe(source) + " vs " + describe(encoded);
        }

        if (source.isJsonObject()) {
            var sourceObject = source.getAsJsonObject();
            var encodedObject = encoded.getAsJsonObject();

            for (var entry : sourceObject.entrySet()) {
                var key = entry.getKey();
                if (!encodedObject.has(key)) return path + "." + key + ": missing from encoded JSON";

                var nested = findFirstDifference(entry.getValue(), encodedObject.get(key), path + "." + key);
                if (nested != null) return nested;
            }

            for (var entry : encodedObject.entrySet()) {
                var key = entry.getKey();
                if (!sourceObject.has(key)) return path + "." + key + ": missing from source JSON";
            }

            return null;
        }

        if (source.isJsonArray()) {
            var sourceArray = source.getAsJsonArray();
            var encodedArray = encoded.getAsJsonArray();
            if (sourceArray.size() != encodedArray.size()) {
                return path + ": array length mismatch " + sourceArray.size() + " vs " + encodedArray.size();
            }

            for (int i = 0; i < sourceArray.size(); i++) {
                var nested = findFirstDifference(sourceArray.get(i), encodedArray.get(i), path + "[" + i + "]");
                if (nested != null) return nested;
            }

            return null;
        }

        if (source.isJsonPrimitive()) {
            return comparePrimitives(source.getAsJsonPrimitive(), encoded.getAsJsonPrimitive(), path);
        }

        return source.equals(encoded) ? null : path + ": value mismatch " + source + " vs " + encoded;
    }

    private static String describe(JsonElement element) {
        if (element.isJsonObject()) return "object";
        if (element.isJsonArray()) return "array";
        if (element.isJsonNull()) return "null";
        return "primitive";
    }

    private static String comparePrimitives(JsonPrimitive source, JsonPrimitive encoded, String path) {
        if (source.isNumber() && encoded.isNumber()) {
            return compareNumbers(source, encoded, path);
        }

        return source.equals(encoded) ? null : path + ": value mismatch " + source + " vs " + encoded;
    }

    private static String compareNumbers(JsonPrimitive source, JsonPrimitive encoded, String path) {
        try {
            var sourceValue = source.getAsBigDecimal();
            var encodedValue = encoded.getAsBigDecimal();
            return sourceValue.compareTo(encodedValue) == 0
                    ? null
                    : path + ": value mismatch " + source + " vs " + encoded;
        } catch (NumberFormatException e) {
            return source.equals(encoded) ? null : path + ": value mismatch " + source + " vs " + encoded;
        }
    }
}
