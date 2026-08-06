package com.bmaster.createrns.compat.kubejs;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.latvian.mods.kubejs.client.LangKubeEvent;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
final class RNSKubeJSBuilderTestAssertions {
    private RNSKubeJSBuilderTestAssertions() {
    }

    static void assertArrayStrings(RNSKubeJSBuilderTestHelper helper, JsonElement element, String label, String... expected) {
        helper.assertTrue(element.isJsonArray(), "Expected JSON array for " + label);

        var array = element.getAsJsonArray();
        helper.assertValueEqual(array.size(), expected.length, label + " size");
        for (var i = 0; i < expected.length; i++) {
            helper.assertValueEqual(array.get(i).getAsString(), expected[i], label + "[" + i + "]");
        }
    }

    static void assertStructureEntry(
            RNSKubeJSBuilderTestHelper helper, JsonArray structures, int index, String id, int weight, String processorId
    ) {
        var structure = structures.get(index).getAsJsonObject();
        helper.assertValueEqual(structure.get("id").getAsString(), id, "structure id " + index);
        helper.assertValueEqual(structure.get("weight").getAsInt(), weight, "structure weight " + index);
        helper.assertValueEqual(structure.get("processor").getAsString(), processorId, "structure processor " + index);
    }

    static void assertStructureSetEntry(
            RNSKubeJSBuilderTestHelper helper, JsonArray structures, int index, String id, int weight
    ) {
        var structure = structures.get(index).getAsJsonObject();
        helper.assertValueEqual(structure.get("structure").getAsString(), id, "structure-set id " + index);
        helper.assertValueEqual(structure.get("weight").getAsInt(), weight, "structure-set weight " + index);
    }

    static void assertLangValue(
            RNSKubeJSBuilderTestHelper helper, LangKubeEvent event, String namespace, String langId, String key,
            String expectedValue
    ) {
        helper.assertValueEqual(findLangValue(helper, event.map(), namespace, langId, key), expectedValue, "lang value");
    }

    static void assertThrows(
            RNSKubeJSBuilderTestHelper helper, Class<? extends Throwable> exceptionClass, String messagePart, Runnable action
    ) {
        try {
            action.run();
        } catch (Throwable throwable) {
            helper.assertTrue(exceptionClass.isInstance(throwable),
                    "Expected " + exceptionClass.getSimpleName() + " but got "
                            + throwable.getClass().getSimpleName());
            helper.assertTrue(throwable.getMessage() != null && throwable.getMessage().contains(messagePart),
                    "Expected exception message containing '" + messagePart + "' but got: " + throwable.getMessage());
            return;
        }

        helper.assertTrue(false, "Expected " + exceptionClass.getSimpleName() + " to be thrown");
    }

    static ResourceLocation depositSpecPath(ResourceLocation structureId) {
        var path = structureId.getPath().startsWith("deposit_")
                ? structureId.getPath().substring("deposit_".length())
                : structureId.getPath();
        return ResourceLocation.fromNamespaceAndPath(structureId.getNamespace(), "create_rns/deposit_spec/" + path);
    }

    static ResourceLocation processorPath(ResourceLocation structureId) {
        return ResourceLocation.fromNamespaceAndPath(structureId.getNamespace(),
                "worldgen/processor_list/replace_with_" + structureId.getNamespace() + "_"
                        + structureId.getPath().replace('/', '_'));
    }

    static ResourceLocation structurePath(ResourceLocation structureId) {
        return ResourceLocation.fromNamespaceAndPath(structureId.getNamespace(), "worldgen/structure/" + structureId.getPath());
    }

    static ResourceLocation structureSetPath(String path) {
        return ResourceLocation.fromNamespaceAndPath("create_rns", "worldgen/structure_set/" + path);
    }

    private static String findLangValue(
            RNSKubeJSBuilderTestHelper helper,
            Map<LangKubeEvent.Key, String> langEntries,
            String namespace,
            String langId,
            String key
    ) {
        for (var entry : langEntries.entrySet()) {
            var langKey = entry.getKey();
            if (!langKey.namespace().equals(namespace)) continue;
            if (!langKey.lang().equals(langId)) continue;
            if (!langKey.key().equals(key)) continue;
            return entry.getValue();
        }

        helper.assertTrue(false, "Missing lang key " + namespace + ":" + langId + ":" + key);
        throw new IllegalStateException("Unreachable");
    }
}
