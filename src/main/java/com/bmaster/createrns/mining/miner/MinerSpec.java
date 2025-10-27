package com.bmaster.createrns.mining.miner;

import com.bmaster.createrns.CreateRNS;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

public record MinerSpec(Block minerBlock, int tier, int miningAreaRadius, int miningAreaHeight,
                        int miningAreaVerticalDisplacement) {
    public static final Codec<MinerSpec> CODEC = RecordCodecBuilder.create(i -> i.group(
            BuiltInRegistries.BLOCK.byNameCodec().fieldOf("miner_block").forGetter(MinerSpec::minerBlock),
            Codec.INT.fieldOf("tier").forGetter(MinerSpec::tier),
            Codec.intRange(0, Integer.MAX_VALUE).fieldOf("mining_area_radius").forGetter(MinerSpec::miningAreaRadius),
            Codec.intRange(0, Integer.MAX_VALUE).fieldOf("mining_area_height").forGetter(MinerSpec::miningAreaHeight),
            Codec.INT.fieldOf("mining_area_vertical_displacement").forGetter(MinerSpec::miningAreaVerticalDisplacement)
    ).apply(i, MinerSpec::new));

    public static final ResourceKey<Registry<MinerSpec>> REGISTRY_KEY =
            ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(CreateRNS.MOD_ID, "miner_spec"));
}
