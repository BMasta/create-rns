package com.bmaster.createrns.content.deposit.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;

import java.util.List;

record WeightedStructure(ResourceLocation id, int weight, Holder<StructureProcessorList> processor) {
    private static final Holder<StructureProcessorList> EMPTY_PROCESSOR_LIST = Holder.direct(new StructureProcessorList(List.of()));

    public static final Codec<WeightedStructure> CODEC = RecordCodecBuilder.create(i -> i.group(
                    ResourceLocation.CODEC.fieldOf("id")
                            .forGetter(WeightedStructure::id),
                    Codec.intRange(1, 150).fieldOf("weight")
                            .forGetter(WeightedStructure::weight),
                    StructureProcessorType.LIST_CODEC.optionalFieldOf("processor", EMPTY_PROCESSOR_LIST)
                            .forGetter(WeightedStructure::processor)
            )
            .apply(i, WeightedStructure::new));

    public static final Codec<List<WeightedStructure>> LIST_CODEC = CODEC.listOf()
            .validate(structures -> structures.isEmpty()
                    ? DataResult.error(() -> "Deposit structures list must not be empty")
                    : DataResult.success(structures));

    public static WeightedStructure pick(WorldgenRandom random, List<WeightedStructure> structures) {
        int totalWeight = 0;
        for (var structure : structures) {
            totalWeight += structure.weight();
        }

        var choice = random.nextInt(totalWeight);
        for (var structure : structures) {
            choice -= structure.weight();
            if (choice < 0) return structure;
        }

        throw new IllegalStateException("Failed to select a deposit structure");
    }
}
