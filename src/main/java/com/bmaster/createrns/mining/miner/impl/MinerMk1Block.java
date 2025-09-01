package com.bmaster.createrns.mining.miner.impl;

import com.bmaster.createrns.RNSContent;
import com.bmaster.createrns.mining.miner.MinerBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

public class MinerMk1Block extends MinerBlock<MinerMk1BlockEntity> {
    public MinerMk1Block(Properties props) {
        super(props);
    }

    @ParametersAreNonnullByDefault
    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new MinerMk1BlockEntity(pPos, pState);
    }

    @Override
    public Class<MinerMk1BlockEntity> getBlockEntityClass() {
        return MinerMk1BlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends MinerMk1BlockEntity> getBlockEntityType() {
        return RNSContent.MINER_MK1_BE.get();
    }
}
