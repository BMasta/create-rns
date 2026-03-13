package com.bmaster.createrns;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class RNSTags {
    public static class Item {
    }

    public static class RNSBlockTags {
        public static final TagKey<net.minecraft.world.level.block.Block> DEPOSIT_BLOCKS = TagKey.create(Registries.BLOCK,
                CreateRNS.asResource("deposit_blocks"));
    }

    public static class RNSStructureTags {
        public static final TagKey<net.minecraft.world.level.levelgen.structure.Structure> DEPOSITS =
                TagKey.create(Registries.STRUCTURE, CreateRNS.asResource("deposits"));
    }

    public static void register() {
    }
}
