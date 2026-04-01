package com.bmaster.createrns.util;

import com.bmaster.createrns.content.deposit.mining.MiningProcess;
import com.bmaster.createrns.content.deposit.mining.contraption.ContraptionMiningBehaviour;
import com.bmaster.createrns.content.deposit.mining.contraption.MinerBearingBlockEntity;
import com.bmaster.createrns.infrastructure.ServerConfig;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.contraptions.MountedStorageManager;
import com.simibubi.create.content.contraptions.glue.SuperGlueEntity;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.fluids.FluidStack;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;

public class MinerSetup {
    private static final Direction MINER_DIRECTION = Direction.DOWN;

    private final GameTestHelper helper;
    private final BlockPos bearingPos;
    private final BlockPos headPos;
    private final Map<BlockPos, BlockState> parts;
    private final Set<BlockPos> deposits;

    private final MinerBearingBlockEntity bearing;

    MinerSetup(
            GameTestHelper helper, BlockPos bearingPos, BlockPos headPos, Map<BlockPos, BlockState> parts, Set<BlockPos> deposits
    ) {
        this.helper = helper;
        this.bearingPos = bearingPos;
        this.headPos = headPos;
        this.parts = parts;
        this.deposits = deposits;

        var be = helper.getLevel().getBlockEntity(helper.absolutePos(bearingPos));
        if (!(be instanceof MinerBearingBlockEntity bbe)) {
            throw new IllegalStateException("Expected miner bearing block entity at " + helper.absolutePos(bearingPos));
        }
        this.bearing = bbe;
    }

    public BlockPos bearingPos() {
        return bearingPos;
    }

    public BlockPos headPos() {
        return headPos;
    }

    public BlockPos motorPos() {
        return bearingPos.above();
    }

    public MinerBearingBlockEntity bearing() {
        return bearing;
    }

    public ContraptionMiningBehaviour behavior() {
        return bearing.miningBehaviour;
    }

    public @Nullable MiningProcess process() {
        return bearing.miningBehaviour.getProcess();
    }

    public MountedStorageManager storage() {
        return bearing.getMovedContraption().getContraption().getStorage();
    }

    public boolean findInStorage(ItemStack expectedStack, boolean exact) {
        var inv = storage().getAllItems();
        var es = expectedStack.copy();
        for (int i = 0; i < inv.getSlots(); ++i) {
            var stack = inv.getStackInSlot(i);
            if (stack.equals(ItemStack.EMPTY)) continue;
            if (stack.is(es.getItem())) {
                if (stack.getCount() > es.getCount()) return !exact;
                es.shrink(stack.getCount());
            } else if (exact) {
                return false;
            }
        }
        return es.getCount() == 0;
    }

    public boolean findInStorage(FluidStack expectedStack, boolean exact) {
        var inv = storage().getFluids();
        var es = expectedStack.copy();
        for (int i = 0; i < inv.getTanks(); ++i) {
            var stack = inv.getFluidInTank(i);
            if (stack.equals(FluidStack.EMPTY)) continue;
            if (stack.isFluidEqual(es)) {
                if (stack.getAmount() > es.getAmount()) return !exact;
                es.shrink(stack.getAmount());
            } else if (exact) {
                return false;
            }
        }
        return es.getAmount() == 0;
    }

    public boolean isRunning() {
        return bearing().isRunning();
    }

    public float speed() {
        return bearing.getSpeed();
    }

    public OptionalInt ticksToMine(int nDepositBlocks) {
        if (Mth.equal(speed(), 0)) return OptionalInt.empty();
        if (nDepositBlocks <= 0) return OptionalInt.empty();
        if (ServerConfig.MINING_SPEED.get() == 0) return OptionalInt.empty();
        return OptionalInt.of((int) Math.ceil(MiningProcess.BASE_PROGRESS /
                (ServerConfig.MINING_SPEED.get() * Math.abs(speed()) * nDepositBlocks)));
    }

    public int contraptionItemCount(Item item) {
        var contraptionEntity = bearing().getMovedContraption();
        if (contraptionEntity == null) return 0;

        var inventory = contraptionEntity.getContraption().getStorage().getAllItems();
        int count = 0;
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            var stack = inventory.getStackInSlot(slot);
            if (stack.is(item)) count += stack.getCount();
        }

        return count;
    }

    public void assemble(int rpm) {
        helper.setBlock(motorPos(), AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, MINER_DIRECTION));

        var blockEntity = helper.getLevel().getBlockEntity(helper.absolutePos(motorPos()));
        if (!(blockEntity instanceof CreativeMotorBlockEntity motor)) {
            throw new IllegalStateException("Expected creative motor block entity at " + helper.absolutePos(motorPos()));
        }

        // Facing down, so rpm will be negative
        motor.generatedSpeed.setValue(-rpm);
    }

    public void disassemble() {
        helper.setBlock(motorPos(), Blocks.AIR.defaultBlockState());
    }

    public void destroy() {
        disassemble();
        helper.setBlock(bearingPos, Blocks.AIR.defaultBlockState());
        helper.setBlock(headPos, Blocks.AIR.defaultBlockState());
        parts.keySet().forEach(pos -> helper.setBlock(pos, Blocks.AIR.defaultBlockState()));
        deposits.forEach(pos -> helper.setBlock(pos, Blocks.AIR.defaultBlockState()));

        var bounds = getAbsoluteBounds().inflate(1);
        for (var glue : helper.getLevel().getEntitiesOfClass(SuperGlueEntity.class, bounds)) {
            glue.discard();
        }
    }

    void placeGlue() {
        var gluedBlocks = new HashSet<>(parts.keySet());
        gluedBlocks.add(headPos);

        for (var pos : gluedBlocks) {
            for (var direction : Direction.values()) {
                if (direction.getAxisDirection() == Direction.AxisDirection.NEGATIVE) continue;

                var neighbor = pos.relative(direction);
                if (!gluedBlocks.contains(neighbor)) continue;

                var glue = new SuperGlueEntity(
                        helper.getLevel(),
                        SuperGlueEntity.span(helper.absolutePos(pos), helper.absolutePos(neighbor))
                );
                helper.getLevel().addFreshEntity(glue);
            }
        }
    }

    private AABB getAbsoluteBounds() {
        var allPositions = new HashSet<BlockPos>();
        allPositions.add(bearingPos);
        allPositions.add(motorPos());
        allPositions.add(headPos);
        allPositions.addAll(parts.keySet());
        allPositions.addAll(deposits);

        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;

        for (var pos : allPositions) {
            var absolutePos = helper.absolutePos(pos);
            minX = Math.min(minX, absolutePos.getX());
            minY = Math.min(minY, absolutePos.getY());
            minZ = Math.min(minZ, absolutePos.getZ());
            maxX = Math.max(maxX, absolutePos.getX());
            maxY = Math.max(maxY, absolutePos.getY());
            maxZ = Math.max(maxZ, absolutePos.getZ());
        }

        return new AABB(minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1);
    }
}
