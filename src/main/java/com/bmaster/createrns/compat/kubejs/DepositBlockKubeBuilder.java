package com.bmaster.createrns.compat.kubejs;

import com.bmaster.createrns.RNSTags.RNSBlockTags;
import com.bmaster.createrns.content.deposit.DepositBlock;
import dev.latvian.mods.kubejs.block.BlockBuilder;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class DepositBlockKubeBuilder extends BlockBuilder {
    private static final ResourceLocation[] DEFAULT_BLOCK_TAGS = {
            BlockTags.MINEABLE_WITH_PICKAXE.location(),
            BlockTags.NEEDS_DIAMOND_TOOL.location(),
            RNSBlockTags.DEPOSIT_BLOCKS.location()
    };

    public DepositBlockKubeBuilder(ResourceLocation id) {
        super(id);
        for (var tag : DEFAULT_BLOCK_TAGS) {
            tagBlock(tag);
        }
    }

    @Override
    public Block createObject() {
        return new DepositBlock(DepositBlock.defaultProperties());
    }
}
