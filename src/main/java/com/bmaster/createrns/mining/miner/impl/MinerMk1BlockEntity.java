package com.bmaster.createrns.mining.miner.impl;

import com.bmaster.createrns.RNSContent;
import com.bmaster.createrns.mining.MiningLevel;
import com.bmaster.createrns.mining.miner.MinerBlockEntity;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class MinerMk1BlockEntity extends MinerBlockEntity {
    public static final int MINING_AREA_RADIUS = 1;
    public static final int MINING_AREA_DEPTH = 1;
    public static final int MINING_AREA_Y_OFFSET = -1;

    public MinerMk1BlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public MinerMk1BlockEntity(BlockPos pos, BlockState state) {
        super(RNSContent.MINER_MK1_BE.get(), pos, state);
    }

    @Override
    public int getMiningAreaRadius() {
        return MINING_AREA_RADIUS;
    }

    @Override
    public int getMiningAreaDepth() {
        return MINING_AREA_DEPTH;
    }

    @Override
    public int getMiningAreaYOffset() {
        return MINING_AREA_Y_OFFSET;
    }

    @Override
    public MiningLevel getMiningLevel() {
        return MiningLevel.BASIC;
    }

    @Override
    public int getBaseProgress() {
        var ticksPerHour = 60 * SharedConstants.TICKS_PER_MINUTE;
        return (int) (256 * ticksPerHour / 45f);
        // TODO
//        return ServerConfig.minerMk1BaseProgress;
    }
}
