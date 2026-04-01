package com.bmaster.createrns.util;

import com.bmaster.createrns.RNSBlocks;
import com.bmaster.createrns.RNSDeposits;
import com.bmaster.createrns.content.deposit.DepositBlock;
import com.bmaster.createrns.content.deposit.mining.contraption.MinerBearingBlock;
import com.bmaster.createrns.content.deposit.mining.contraption.attachment.minehead.MineHeadBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Map;

public class MinerSetupBuilder {
    private static final Direction MINER_DIRECTION = Direction.DOWN;

    public static MinerSetupBuilder create(GameTestHelper helper) {
        return new MinerSetupBuilder(helper);
    }

    private final GameTestHelper helper;
    private final Map<BlockPos, BlockState> parts = new HashMap<>();
    private final Map<BlockPos, Block> deposits = new HashMap<>();

    private BlockPos bearingPos;
    private BlockPos headPos;

    private MinerSetupBuilder(GameTestHelper helper) {
        this.helper = helper;
    }

    public MinerSetupBuilder bearing(int x, int y, int z) {
        bearingPos = new BlockPos(x, y, z);
        return this;
    }

    public MinerSetupBuilder head(int x, int y, int z) {
        headPos = new BlockPos(x, y, z);
        return this;
    }

    public MinerSetupBuilder part(BlockState state, int x, int y, int z) {
        parts.put(new BlockPos(x, y, z), state);
        return this;
    }

    public MinerSetupBuilder part(BlockState state, int x1, int y1, int z1, int x2, int y2, int z2) {
        int minX = Math.min(x1, x2);
        int maxX = Math.max(x1, x2);
        int minY = Math.min(y1, y2);
        int maxY = Math.max(y1, y2);
        int minZ = Math.min(z1, z2);
        int maxZ = Math.max(z1, z2);

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    part(state, x, y, z);
                }
            }
        }

        return this;
    }

    public MinerSetupBuilder deposit(int x, int y, int z, DepositBlock deposit) {
        deposits.put(new BlockPos(x, y, z), deposit);
        return this;
    }

    public MinerSetupBuilder deposit(int x, int y, int z) {
        return deposit(x, y, z, RNSDeposits.DEPLETED_DEPOSIT.get());
    }

    public MinerSetupBuilder deposit(int x1, int y1, int z1, int x2, int y2, int z2, DepositBlock deposit) {
        int minX = Math.min(x1, x2);
        int maxX = Math.max(x1, x2);
        int minY = Math.min(y1, y2);
        int maxY = Math.max(y1, y2);
        int minZ = Math.min(z1, z2);
        int maxZ = Math.max(z1, z2);

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    deposit(x, y, z, deposit);
                }
            }
        }

        return this;
    }

    public MinerSetupBuilder deposit(int x1, int y1, int z1, int x2, int y2, int z2) {
        return deposit(x1, y1, z1, x2, y2, z2, RNSDeposits.DEPLETED_DEPOSIT.get());
    }

    public MinerSetup place() {
        if (bearingPos == null) throw new IllegalStateException("Bearing position must be set");
        if (headPos == null) throw new IllegalStateException("Mine head position must be set");
        if (parts.isEmpty()) throw new IllegalStateException("At least one contraption part must be set");
        if (deposits.isEmpty()) throw new IllegalStateException("At least one deposit position must be set");

        helper.absolutePos(bearingPos.above());

        helper.setBlock(bearingPos, RNSBlocks.MINER_BEARING.getDefaultState()
                .setValue(MinerBearingBlock.FACING, MINER_DIRECTION));
        parts.forEach(helper::setBlock);
        helper.setBlock(headPos, MineHeadBlock.withConnectedDirection(
                RNSBlocks.MINE_HEAD.getDefaultState(), MINER_DIRECTION));
        deposits.forEach((pos, b) -> helper.setBlock(pos, b.defaultBlockState()));

        var setup = new MinerSetup(helper, bearingPos, headPos, Map.copyOf(parts), deposits.keySet());
        setup.placeGlue();
        return setup;
    }
}
