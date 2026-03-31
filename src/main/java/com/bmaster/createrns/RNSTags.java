package com.bmaster.createrns;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.Structure;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class RNSTags {
    public static class Item {
    }

    public static class RNSBlockTags {
        public static final TagKey<Block> DEPOSIT_BLOCKS = TagKey.create(Registries.BLOCK,
                CreateRNS.asResource("deposit_blocks"));
        public static final TagKey<Block> MINER_ATTACHMENTS = TagKey.create(Registries.BLOCK,
                CreateRNS.asResource("miner_attachments"));
        public static final TagKey<Block> RESONATOR_ATTACHMENTS = TagKey.create(Registries.BLOCK,
                CreateRNS.asResource("resonator_attachments"));
        public static final TagKey<Block> RES_BUFFER_ATTACHMENTS = TagKey.create(Registries.BLOCK,
                CreateRNS.asResource("resonance_buffer_attachments"));
    }

    public static class RNSStructureTags {
        public static final TagKey<Structure> DEPOSITS =
                TagKey.create(Registries.STRUCTURE, CreateRNS.asResource("deposits"));
    }

    public static void register() {
    }
}
