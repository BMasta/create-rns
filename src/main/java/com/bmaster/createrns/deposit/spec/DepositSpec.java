package com.bmaster.createrns.deposit.spec;

import com.bmaster.createrns.CreateRNS;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryFixedCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraftforge.registries.ForgeRegistries;

public record DepositSpec(Item yield, Block depositBlock, Holder<Structure> structure) {
    public static final Codec<DepositSpec> CODEC = RecordCodecBuilder.create(i -> i.group(
            ForgeRegistries.ITEMS.getCodec().fieldOf("yield").forGetter(DepositSpec::yield),
            ForgeRegistries.BLOCKS.getCodec().fieldOf("depositBlock").forGetter(DepositSpec::depositBlock),
            RegistryFixedCodec.create(Registries.STRUCTURE).fieldOf("structure").forGetter(DepositSpec::structure)
    ).apply(i, DepositSpec::new));

    public static final ResourceKey<Registry<DepositSpec>> REGISTRY_KEY =
            ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(CreateRNS.MOD_ID, "deposit_spec"));
}
