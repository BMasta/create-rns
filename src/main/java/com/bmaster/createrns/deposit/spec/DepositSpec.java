package com.bmaster.createrns.deposit.spec;

import com.bmaster.createrns.CreateRNS;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;

public record DepositSpec(Item scannerIconItem, ResourceLocation structure) {
    public static final Codec<DepositSpec> CODEC = RecordCodecBuilder.create(i -> i.group(
            ForgeRegistries.ITEMS.getCodec().fieldOf("scanner_icon_item").forGetter(DepositSpec::scannerIconItem),
            ResourceLocation.CODEC.fieldOf("structure").forGetter(DepositSpec::structure)
    ).apply(i, DepositSpec::new));

    public static final ResourceKey<Registry<DepositSpec>> REGISTRY_KEY =
            ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(CreateRNS.MOD_ID, "deposit_spec"));
}
