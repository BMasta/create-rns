package com.bmaster.createrns.compat.kubejs;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.latvian.mods.kubejs.script.data.GeneratedData;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.ParametersAreNonnullByDefault;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class RNSKubeJSBuilderTestData {
    private final Map<ResourceLocation, GeneratedData> generatedById;

    public RNSKubeJSBuilderTestData(Map<ResourceLocation, GeneratedData> generatedById) {
        this.generatedById = Map.copyOf(new LinkedHashMap<>(generatedById));
    }

    public boolean hasJson(ResourceLocation id) {
        return generatedById.containsKey(normalizeJsonId(id));
    }

    public JsonElement json(ResourceLocation id) {
        return JsonParser.parseString(text(normalizeJsonId(id)));
    }

    public JsonObject jsonObject(ResourceLocation id) {
        return json(id).getAsJsonObject();
    }

    public String text(ResourceLocation id) {
        var generated = generatedById.get(id);
        if (generated == null) {
            throw new IllegalStateException("Missing generated resource: " + id);
        }
        return new String(generated.data().get(), StandardCharsets.UTF_8);
    }

    public int size() {
        return generatedById.size();
    }

    public Set<ResourceLocation> ids() {
        return generatedById.keySet();
    }

    private static ResourceLocation normalizeJsonId(ResourceLocation id) {
        if (id.getPath().endsWith(".json")) return id;
        return ResourceLocation.fromNamespaceAndPath(id.getNamespace(), id.getPath() + ".json");
    }
}
