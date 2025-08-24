package com.bmaster.createrns.capability.depositindex;

import com.bmaster.createrns.CreateRNS;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.HolderSetCodec;
import net.minecraft.resources.RegistryFixedCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraftforge.registries.ForgeRegistries;

public record DepositSpec(Item yield, HolderSet<Structure> structures) {
    public static final Codec<HolderSet<Structure>> STRUCTURE_SET_CODEC = HolderSetCodec.create(
            Registries.STRUCTURE, RegistryFixedCodec.create(Registries.STRUCTURE), false);

    public static final Codec<DepositSpec> CODEC = RecordCodecBuilder.create(i -> i.group(
            ForgeRegistries.ITEMS.getCodec().fieldOf("yield").forGetter(DepositSpec::yield),
            STRUCTURE_SET_CODEC.fieldOf("structures").forGetter(DepositSpec::structures)
    ).apply(i, DepositSpec::new));

    public static final ResourceKey<Registry<DepositSpec>> REGISTRY_KEY =
            ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(CreateRNS.MOD_ID, "deposit_spec"));
}
