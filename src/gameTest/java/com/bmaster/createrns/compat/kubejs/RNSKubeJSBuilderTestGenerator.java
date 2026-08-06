package com.bmaster.createrns.compat.kubejs;

import dev.latvian.mods.kubejs.generator.DataJsonGenerator;
import dev.latvian.mods.kubejs.script.data.GeneratedData;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.LinkedHashMap;
import java.util.Map;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class RNSKubeJSBuilderTestGenerator extends DataJsonGenerator {
    private final Map<ResourceLocation, GeneratedData> generatedById;

    public RNSKubeJSBuilderTestGenerator() {
        this(new LinkedHashMap<>());
    }

    private RNSKubeJSBuilderTestGenerator(Map<ResourceLocation, GeneratedData> generatedById) {
        super(generatedById);
        this.generatedById = generatedById;
    }

    Map<ResourceLocation, GeneratedData> generatedById() {
        return Map.copyOf(generatedById);
    }
}
