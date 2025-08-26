package com.bmaster.createrns;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class RNSTags {
    public static class Item {
        public static final TagKey<net.minecraft.world.item.Item> DEPOSIT_BLOCKS = TagKey.create(Registries.ITEM,
                ResourceLocation.fromNamespaceAndPath(CreateRNS.MOD_ID, "deposit_blocks"));
    }

    public static class Block {
        public static final TagKey<net.minecraft.world.level.block.Block> DEPOSIT_BLOCKS = TagKey.create(Registries.BLOCK,
                ResourceLocation.fromNamespaceAndPath(CreateRNS.MOD_ID, "deposit_blocks"));
    }

    public static void register() {
    }
}
