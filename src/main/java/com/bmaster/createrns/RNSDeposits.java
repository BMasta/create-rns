package com.bmaster.createrns;

import com.bmaster.createrns.content.deposit.DepositBlock;
import com.bmaster.createrns.data.pack.DynamicDatapackDepositEntry;
import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.nullness.NonNullFunction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.MapColor;

import static com.simibubi.create.foundation.data.TagGen.pickaxeOnly;

public class RNSDeposits {
    public static final BlockEntry<DepositBlock> IRON_DEPOSIT = DynamicDatapackDepositEntry
            .create("iron")
            .depth(8)
            .weight(10)
            .nbt(DynamicDatapackDepositEntry.DEP_MEDIUM, 70)
            .nbt(DynamicDatapackDepositEntry.DEP_LARGE, 30)
            .block("iron_deposit_block")
            .transform(depositBlock(MapColor.RAW_IRON))
            .register();

    public static final BlockEntry<DepositBlock> COPPER_DEPOSIT = DynamicDatapackDepositEntry
            .create("copper")
            .depth(8)
            .weight(5)
            .nbt(DynamicDatapackDepositEntry.DEP_MEDIUM, 70)
            .nbt(DynamicDatapackDepositEntry.DEP_LARGE, 30)
            .block("copper_deposit_block")
            .transform(depositBlock(MapColor.COLOR_ORANGE))
            .register();

    public static final BlockEntry<DepositBlock> ZINC_DEPOSIT = DynamicDatapackDepositEntry
            .create("zinc")
            .depth(8)
            .weight(2)
            .nbt(DynamicDatapackDepositEntry.DEP_SMALL, 70)
            .nbt(DynamicDatapackDepositEntry.DEP_MEDIUM, 28)
            .nbt(DynamicDatapackDepositEntry.DEP_LARGE, 2)
            .block("zinc_deposit_block")
            .transform(depositBlock(MapColor.GLOW_LICHEN))
            .register();

    public static final BlockEntry<DepositBlock> GOLD_DEPOSIT = DynamicDatapackDepositEntry
            .create("gold")
            .depth(12)
            .weight(2)
            .nbt(DynamicDatapackDepositEntry.DEP_SMALL, 70)
            .nbt(DynamicDatapackDepositEntry.DEP_MEDIUM, 28)
            .nbt(DynamicDatapackDepositEntry.DEP_LARGE, 2)
            .block("gold_deposit_block")
            .transform(depositBlock(MapColor.GOLD))
            .register();

    public static final BlockEntry<DepositBlock> REDSTONE_DEPOSIT = DynamicDatapackDepositEntry
            .create("redstone")
            .depth(12)
            .weight(2)
            .nbt(DynamicDatapackDepositEntry.DEP_SMALL, 70)
            .nbt(DynamicDatapackDepositEntry.DEP_MEDIUM, 28)
            .nbt(DynamicDatapackDepositEntry.DEP_LARGE, 2)
            .block("redstone_deposit_block")
            .transform(depositBlock(MapColor.FIRE))
            .register();

    public static final BlockEntry<DepositBlock> DEPLETED_DEPOSIT = DynamicDatapackDepositEntry
            .blockOnly("depleted_deposit_block")
            .transform(depositBlock(MapColor.COLOR_BLACK))
            .register();

    public static final BlockEntry<DepositBlock> URANIUM_DEPOSIT = DynamicDatapackDepositEntry
            .create("uranium")
            .depth(12)
            .weight(2)
            .nbt(DynamicDatapackDepositEntry.DEP_SMALL, 70)
            .nbt(DynamicDatapackDepositEntry.DEP_MEDIUM, 28)
            .nbt(DynamicDatapackDepositEntry.DEP_LARGE, 2)
            .requireMod("createnuclear")
            .block("uranium_deposit_block")
            .transform(depositBlock(MapColor.COLOR_GREEN))
            .register();

    public static <T extends Block, P> NonNullFunction<BlockBuilder<T, P>, BlockBuilder<T, P>> depositBlock(MapColor mapColor) {
        return b -> b
                .initialProperties(() -> Blocks.RAW_IRON_BLOCK)
                .properties(p -> p
                        .mapColor(mapColor)
                        .strength(50.0F, 1200f)
                        .noLootTable())
                .transform(pickaxeOnly())
                .tag(BlockTags.NEEDS_DIAMOND_TOOL)
                .tag(RNSTags.RNSBlockTags.DEPOSIT_BLOCKS)
                .item()
                .build();
    }

    public static void register() {
    }
}
