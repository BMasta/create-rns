package com.bmaster.createrns.compat.kubejs;

import dev.latvian.mods.kubejs.generator.KubeDataGenerator;
import dev.latvian.mods.kubejs.script.data.GeneratedData;
import dev.latvian.mods.kubejs.script.data.VirtualDataMapFile;
import dev.latvian.mods.kubejs.util.RegistryAccessContainer;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.datamaps.DataMapType;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class RNSKubeJSBuilderTestGenerator implements KubeDataGenerator {
    private final Map<ResourceLocation, GeneratedData> generatedById = new LinkedHashMap<>();

    @Override
    public RegistryAccessContainer getRegistries() {
        return RegistryAccessContainer.BUILTIN;
    }

    @Override
    public void add(GeneratedData data) {
        generatedById.put(data.id(), data);
    }

    @Override
    public @Nullable GeneratedData getGenerated(ResourceLocation id) {
        return generatedById.get(id);
    }

    @Override
    public <R, T> void dataMap(DataMapType<R, T> type, Consumer<VirtualDataMapFile<R, T>> consumer) {
        throw new UnsupportedOperationException("KubeJS builder emission tests do not support data-map generation");
    }

    Map<ResourceLocation, GeneratedData> generatedById() {
        return Map.copyOf(generatedById);
    }
}
