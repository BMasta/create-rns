package com.bmaster.createrns.comprehensive;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.RNSBlocks;
import com.bmaster.createrns.content.deposit.mining.contraption.MinerContraption;
import com.bmaster.createrns.content.deposit.mining.contraption.attachment.minehead.MineHeadBlock;
import com.bmaster.createrns.content.deposit.mining.contraption.attachment.minehead.MineHeadSize;
import com.bmaster.createrns.util.MinerSetup;
import com.bmaster.createrns.util.MinerSetupBuilder;
import com.simibubi.create.content.contraptions.Contraption;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.HashSet;
import java.util.Set;

@GameTestHolder(CreateRNS.ID)
@PrefixGameTestTemplate(false)
public class MinerContraptionGameTest {
    private static final int ASSEMBLY_RPM = 1;
    private static final int EXPECTED_BUFFER_COUNT = 1;
    private static final int EXPECTED_HUGE_MINE_HEAD_BLOCK_COUNT = 48;
    private static final int EXPECTED_LARGE_MINE_HEAD_BLOCK_COUNT = 10;
    private static final int EXPECTED_RESONATOR_COUNT = 3;
    private static final int EXPECTED_SMALL_MINE_HEAD_BLOCK_COUNT = 1;

    private static final BlockPos BEARING_POS = new BlockPos(8, 8, 8);
    private static final BlockPos DEPOSIT_POS = new BlockPos(8, 1, 8);
    private static final BlockPos SERIALIZATION_HEAD_POS = new BlockPos(8, 5, 8);
    private static final BlockPos SMALL_HEAD_POS = new BlockPos(8, 6, 8);
    private static final BlockPos LARGE_CONTROLLER_POS = new BlockPos(8, 7, 8);
    private static final BlockPos LARGE_TIP_POS = new BlockPos(8, 6, 8);
    private static final BlockPos HUGE_CONTROLLER_POS = new BlockPos(8, 7, 8);
    private static final BlockPos HUGE_TIP_POS = new BlockPos(8, 4, 8);

    @GameTest(template = "empty16x16", timeoutTicks = 10)
    public void minerContraptionRoundTripsSubtypeAndState(GameTestHelper helper) {
        var miner = MinerSetupBuilder.create(helper)
                .bearing(BEARING_POS.getX(), BEARING_POS.getY(), BEARING_POS.getZ())
                .part(RNSBlocks.RESONANCE_BUFFER.getDefaultState(), 8, 7, 8)
                .part(RNSBlocks.RESONATOR.getDefaultState(), 8, 6, 8)
                .part(RNSBlocks.SHATTERING_RESONATOR.getDefaultState(), 7, 7, 8)
                .part(RNSBlocks.STABILIZING_RESONATOR.getDefaultState(), 9, 7, 8)
                .head(SERIALIZATION_HEAD_POS.getX(), SERIALIZATION_HEAD_POS.getY(), SERIALIZATION_HEAD_POS.getZ())
                .deposit(DEPOSIT_POS.getX(), DEPOSIT_POS.getY(), DEPOSIT_POS.getZ())
                .place();
        miner.assemble(ASSEMBLY_RPM);

        helper.runAtTickTime(1, () -> {
            helper.assertTrue(miner.isRunning(), "Miner did not assemble");
            var original = miner.bearing().getMovedContraption().getContraption();
            var tag = original.writeNBT(helper.getLevel().registryAccess(), false);
            var restored = Contraption.fromNBT(helper.getLevel(), tag, false);

            helper.assertTrue(restored instanceof MinerContraption,
                    "Expected a MinerContraption after the NBT round trip, got " + restored.getClass().getName());
            var restoredMiner = (MinerContraption) restored;
            helper.assertValueEqual(restoredMiner.mineHeadPos, helper.absolutePos(SERIALIZATION_HEAD_POS),
                    "mine head position");
            helper.assertValueEqual(restoredMiner.bufferCount, EXPECTED_BUFFER_COUNT, "resonance buffer count");
            helper.assertValueEqual(restoredMiner.resonatorCount, EXPECTED_RESONATOR_COUNT, "resonator count");
            helper.succeed();
        });
    }

    @GameTest(template = "empty16x16", timeoutTicks = 10)
    public void smallMineHeadAssemblyStateLifecycle(GameTestHelper helper) {
        var miner = MinerSetupBuilder.create(helper)
                .bearing(BEARING_POS.getX(), BEARING_POS.getY(), BEARING_POS.getZ())
                .part(Blocks.IRON_BLOCK.defaultBlockState(), 8, 7, 8)
                .head(SMALL_HEAD_POS.getX(), SMALL_HEAD_POS.getY(), SMALL_HEAD_POS.getZ())
                .deposit(DEPOSIT_POS.getX(), DEPOSIT_POS.getY(), DEPOSIT_POS.getZ())
                .place();

        assertAssemblyStateLifecycle(helper, miner, MineHeadSize.SMALL, SMALL_HEAD_POS, Set.of(SMALL_HEAD_POS));
    }

    @GameTest(template = "empty16x16", timeoutTicks = 10)
    public void largeMineHeadAssemblyStateLifecycle(GameTestHelper helper) {
        var builder = MinerSetupBuilder.create(helper)
                .bearing(BEARING_POS.getX(), BEARING_POS.getY(), BEARING_POS.getZ())
                .head(LARGE_TIP_POS.getX(), LARGE_TIP_POS.getY(), LARGE_TIP_POS.getZ())
                .deposit(DEPOSIT_POS.getX(), DEPOSIT_POS.getY(), DEPOSIT_POS.getZ());
        addIronLayer(builder, LARGE_CONTROLLER_POS, 1, true);
        var miner = builder.place();

        var occupied = squareLayer(LARGE_CONTROLLER_POS, 1, true);
        occupied.add(LARGE_TIP_POS);
        assertAssemblyStateLifecycle(helper, miner, MineHeadSize.LARGE, LARGE_CONTROLLER_POS, occupied);
    }

    @GameTest(template = "empty16x16", timeoutTicks = 10)
    public void hugeMineHeadAssemblyStateLifecycle(GameTestHelper helper) {
        var builder = MinerSetupBuilder.create(helper)
                .bearing(BEARING_POS.getX(), BEARING_POS.getY(), BEARING_POS.getZ())
                .head(HUGE_TIP_POS.getX(), HUGE_TIP_POS.getY(), HUGE_TIP_POS.getZ())
                .deposit(DEPOSIT_POS.getX(), DEPOSIT_POS.getY(), DEPOSIT_POS.getZ());
        addIronLayer(builder, HUGE_CONTROLLER_POS, 2, false);
        addIronLayer(builder, HUGE_CONTROLLER_POS.below(), 2, false);
        addIronLayer(builder, HUGE_CONTROLLER_POS.below(2), 1, false);
        var miner = builder.place();

        var occupied = squareLayer(HUGE_CONTROLLER_POS, 2, false);
        occupied.addAll(squareLayer(HUGE_CONTROLLER_POS.below(), 2, false));
        occupied.addAll(squareLayer(HUGE_CONTROLLER_POS.below(2), 1, false));
        occupied.add(HUGE_TIP_POS);
        assertAssemblyStateLifecycle(helper, miner, MineHeadSize.HUGE, HUGE_CONTROLLER_POS, occupied);
    }

    private static void assertAssemblyStateLifecycle(
            GameTestHelper helper, MinerSetup miner, MineHeadSize expectedSize, BlockPos controllerPos,
            Set<BlockPos> occupiedPositions
    ) {
        int expectedBlockCount = switch (expectedSize) {
            case SMALL -> EXPECTED_SMALL_MINE_HEAD_BLOCK_COUNT;
            case LARGE -> EXPECTED_LARGE_MINE_HEAD_BLOCK_COUNT;
            case HUGE -> EXPECTED_HUGE_MINE_HEAD_BLOCK_COUNT;
        };
        helper.assertValueEqual(occupiedPositions.size(), expectedBlockCount, "expected mine head block count");
        assertMineHeadInWorld(helper, expectedSize, controllerPos, occupiedPositions, false);
        miner.assemble(ASSEMBLY_RPM);

        helper.runAtTickTime(1, () -> {
            helper.assertTrue(miner.isRunning(), "Miner did not assemble");
            var contraption = miner.bearing().getMovedContraption().getContraption();
            helper.assertTrue(contraption instanceof MinerContraption,
                    "Expected the assembled contraption to be a MinerContraption");
            assertMineHeadInContraption(helper, contraption, expectedSize, occupiedPositions);

            miner.bearing().disassemble();
            helper.assertFalse(miner.isRunning(), "Miner remained assembled after disassembly");
            assertMineHeadInWorld(helper, expectedSize, controllerPos, occupiedPositions, false);
            helper.succeed();
        });
    }

    private static void assertMineHeadInWorld(
            GameTestHelper helper, MineHeadSize expectedSize, BlockPos controllerPos,
            Set<BlockPos> occupiedPositions, boolean assembled
    ) {
        for (var pos : occupiedPositions) {
            var state = helper.getLevel().getBlockState(helper.absolutePos(pos));
            if (pos.equals(controllerPos)) {
                helper.assertTrue(state.is(RNSBlocks.MINE_HEAD.get()),
                        "Expected mine head controller at " + pos + ", got " + state);
                helper.assertValueEqual(state.getValue(MineHeadBlock.SIZE), expectedSize,
                        "mine head size at " + pos);
            } else {
                helper.assertTrue(state.is(RNSBlocks.MINE_HEAD_PART.get()),
                        "Expected mine head part at " + pos + ", got " + state);
            }
            helper.assertValueEqual(state.getValue(MineHeadBlock.ASSEMBLED), assembled,
                    "assembled state at " + pos);
        }
    }

    private static void assertMineHeadInContraption(
            GameTestHelper helper, Contraption contraption, MineHeadSize expectedSize, Set<BlockPos> occupiedPositions
    ) {
        var actualPositions = new HashSet<BlockPos>();
        int controllers = 0;
        for (var entry : contraption.getBlocks().entrySet()) {
            var state = entry.getValue().state();
            if (!state.is(RNSBlocks.MINE_HEAD.get()) && !state.is(RNSBlocks.MINE_HEAD_PART.get())) continue;

            actualPositions.add(entry.getKey());
            helper.assertTrue(state.getValue(MineHeadBlock.ASSEMBLED),
                    "Mine head block was not marked assembled at local position " + entry.getKey());
            if (state.is(RNSBlocks.MINE_HEAD.get())) {
                controllers++;
                helper.assertValueEqual(state.getValue(MineHeadBlock.SIZE), expectedSize,
                        "assembled mine head size");
            }
        }

        var expectedPositions = new HashSet<BlockPos>();
        for (var pos : occupiedPositions) {
            expectedPositions.add(helper.absolutePos(pos).subtract(contraption.anchor));
        }
        helper.assertValueEqual(actualPositions, expectedPositions, "assembled mine head positions");
        helper.assertValueEqual(controllers, 1, "assembled mine head controller count");
    }

    private static void addIronLayer(
            MinerSetupBuilder builder, BlockPos center, int radius, boolean includeCorners
    ) {
        for (var pos : squareLayer(center, radius, includeCorners)) {
            builder.part(Blocks.IRON_BLOCK.defaultBlockState(), pos.getX(), pos.getY(), pos.getZ());
        }
    }

    private static Set<BlockPos> squareLayer(BlockPos center, int radius, boolean includeCorners) {
        var positions = new HashSet<BlockPos>();
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (!includeCorners && Math.abs(x) == radius && Math.abs(z) == radius) continue;
                positions.add(center.offset(x, 0, z));
            }
        }
        return positions;
    }
}
