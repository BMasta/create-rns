package com.bmaster.createrns.mining;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.deposit.IDepositBlockClaimer.ClaimingAreaSpec;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

public record MinerSpec(Block minerBlock, int tier, float minesPerHour, ClaimingAreaSpec miningArea) {

    public static final Codec<MinerSpec> CODEC = RecordCodecBuilder.create(i -> i.group(
            BuiltInRegistries.BLOCK.byNameCodec().fieldOf("miner_block").forGetter(MinerSpec::minerBlock),
            Codec.INT.fieldOf("tier").forGetter(MinerSpec::tier),
            Codec.FLOAT.fieldOf("mines_per_hour").forGetter(MinerSpec::minesPerHour),
            ClaimingAreaSpec.CODEC.fieldOf("mining_area").forGetter(MinerSpec::miningArea)
    ).apply(i, MinerSpec::new));

    public static final ResourceKey<Registry<MinerSpec>> REGISTRY_KEY =
            ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(CreateRNS.MOD_ID, "miner_spec"));
}
