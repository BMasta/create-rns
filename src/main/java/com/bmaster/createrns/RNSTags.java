package com.bmaster.createrns;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;

public class RNSTags {
    public static class Item {
    }

    public static class Block {
        public static final TagKey<net.minecraft.world.level.block.Block> DEPOSIT_BLOCKS = TagKey.create(Registries.BLOCK,
                CreateRNS.asResource("deposit_blocks"));
    }

    public static void register() {
    }
}
