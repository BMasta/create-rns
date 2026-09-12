package com.bmaster.createrns.content.deposit.mining.display;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.RNSDeposits;
import com.bmaster.createrns.RNSDisplaySources;
import com.bmaster.createrns.content.deposit.mining.MiningStats;
import com.bmaster.createrns.infrastructure.ServerConfig;
import com.bmaster.createrns.util.MinerSetup;
import com.bmaster.createrns.util.MinerSetupBuilder;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllDisplayTargets;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.actors.psi.PortableStorageInterfaceBlock;
import com.simibubi.create.content.contraptions.actors.psi.PortableStorageInterfaceMovement;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkBlock;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkBlockEntity;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Set;

@GameTestHolder(CreateRNS.ID)
@PrefixGameTestTemplate(false)
public class CompactMinerStatusDisplaySourceGameTest {
    private static final int TEST_RPM = 128;
    private static final int TOO_SLOW_RPM = 1;
    private static final int UPTIME_STYLE = 1;
    private static final int MINED_STYLE = 2;
    private static final int CONSUMED_STYLE = 3;
    private static final int REMAINING_STYLE = 4;
    private static final long FINITE_REMAINING_RESOURCES = 9;
    private static final BlockPos BEARING_POS = new BlockPos(5, 5, 5);
    private static final BlockPos DISPLAY_LINK_POS = BEARING_POS.east();
    private static final BlockPos STATIONARY_INTERFACE_POS = new BlockPos(10, 4, 5);

    private static final List<String> EXPECTED_LINES = List.of(
            "Not Assembled",
            "Not Assembled (... uptime)",
            "Not Assembled (0 mined)",
            "Not Assembled (0 consumed)",
            "Not Assembled (0 remaining)"
    );

    @GameTest(template = "empty16x16", timeoutTicks = 5)
    public void everyStyleProducesOneCompactLine(GameTestHelper helper) {
        createMiner(helper);
        var displayLink = attachDisplayLink(helper);
        var context = new DisplayLinkContext(helper.getLevel(), displayLink);
        var stats = new DisplayTargetStats(1, 128, AllDisplayTargets.NIXIE_TUBE.get());
        var source = RNSDisplaySources.COMPACT_MINER_STATUS.get();

        withInfiniteDeposits(false, () -> {
            for (int style = 0; style < EXPECTED_LINES.size(); style++) {
                var config = new CompoundTag();
                config.putInt("style", style);
                displayLink.setSourceConfig(config);

                var text = source.provideText(context, stats);
                helper.assertValueEqual(text.size(), 1, "output row count for style " + style);
                helper.assertValueEqual(text.getFirst().getString(), EXPECTED_LINES.get(style),
                        "output text for style " + style);
            }
        });

        helper.succeed();
    }

    @GameTest(template = "empty16x16", timeoutTicks = 5)
    public void labelPrefixesCompactStatus(GameTestHelper helper) {
        createMiner(helper);
        var displayLink = attachDisplayLink(helper);
        var config = new CompoundTag();
        config.putString("Label", "Miner A:");
        displayLink.setSourceConfig(config);

        var context = new DisplayLinkContext(helper.getLevel(), displayLink);
        var stats = new DisplayTargetStats(1, 128, AllDisplayTargets.NIXIE_TUBE.get());
        var text = RNSDisplaySources.COMPACT_MINER_STATUS.get().provideText(context, stats);
        helper.assertValueEqual(text.size(), 1, "labeled compact status output row count");
        helper.assertValueEqual(text.getFirst().getString(), "Miner A: Not Assembled",
                "labeled compact miner status");
        helper.succeed();
    }

    @GameTest(template = "empty16x16", timeoutTicks = 5)
    public void assembledMinerWithoutClaimsReportsOutOfResources(GameTestHelper helper) {
        var miner = createMiner(helper);
        var displayLink = attachDisplayLink(helper);
        miner.assemble(TEST_RPM);

        helper.runAtTickTime(2, () -> {
            miner.behavior().setClaimedDepositBlocks(Set.of(), false);
            helper.assertTrue(miner.isRunning(), "Miner should be assembled");
            helper.assertTrue(miner.behavior().getClaimedDepositBlocks().isEmpty(),
                    "Miner should not have any claimed deposit blocks");
            assertSimpleStatus(helper, displayLink, "Out of Resources");
            helper.succeed();
        });
    }

    @GameTest(template = "empty16x16", timeoutTicks = 5)
    public void finiteResourceStylesAreHiddenWhenDepositsAreInfinite(GameTestHelper helper) {
        var miner = createMiner(helper);
        var displayLink = attachDisplayLink(helper);
        miner.assemble(TEST_RPM);

        helper.runAtTickTime(2, () -> {
            withInfiniteDeposits(true, () -> {
                assertStatus(helper, displayLink, CONSUMED_STYLE, "Mining");
                assertStatus(helper, displayLink, REMAINING_STYLE, "Mining");
            });
            helper.succeed();
        });
    }

    @GameTest(template = "empty16x16", timeoutTicks = 8)
    public void assembledMinerBelowRequiredRpmReportsTooSlow(GameTestHelper helper) {
        var miner = createMiner(helper);
        var displayLink = attachDisplayLink(helper);
        miner.assemble(TEST_RPM);

        helper.runAtTickTime(2, () -> {
            assertSimpleStatus(helper, displayLink, "Mining");
            assertStatus(helper, displayLink, UPTIME_STYLE, "Mining (... uptime)");

            var blockEntity = helper.getLevel().getBlockEntity(helper.absolutePos(miner.motorPos()));
            helper.assertTrue(blockEntity instanceof CreativeMotorBlockEntity,
                    "Expected a Creative Motor block entity above the miner bearing");
            ((CreativeMotorBlockEntity) blockEntity).generatedSpeed.setValue(-TOO_SLOW_RPM);
        });

        helper.runAtTickTime(5, () -> {
            helper.assertTrue(miner.isRunning(), "Miner should remain assembled at a nonzero speed");
            helper.assertTrue(Math.abs(miner.speed()) > 0, "Miner RPM should remain above zero");
            helper.assertFalse(miner.bearing().isSpeedRequirementFulfilled(),
                    "Miner RPM should not fulfill its speed requirement");
            assertSimpleStatus(helper, displayLink, "Too Slow");
            assertStatus(helper, displayLink, UPTIME_STYLE, "Too Slow (... uptime)");
            helper.assertValueEqual(miner.behavior().stats.getUptimeTicks(), 0L,
                    "Uptime should be ignored during the grace period");
            helper.succeed();
        });
    }

    @GameTest(template = "empty16x16", timeoutTicks = MiningStats.UPTIME_GRACE_PERIOD_TICKS + 10)
    public void uptimeGracePeriodEventuallyEnds(GameTestHelper helper) {
        var miner = createMiner(helper);
        miner.assemble(TEST_RPM);

        helper.runAtTickTime(MiningStats.UPTIME_GRACE_PERIOD_TICKS - 1, () -> {
            var stats = miner.behavior().stats;
            helper.assertValueEqual(stats.getElapsedTicks(helper.getLevel()), 0L,
                    "Elapsed time should be ignored during the grace period");
            helper.assertValueEqual(stats.getUptimeTicks(), 0L,
                    "Uptime should be ignored during the grace period");
        });

        helper.runAtTickTime(MiningStats.UPTIME_GRACE_PERIOD_TICKS + 5, () -> {
            var stats = miner.behavior().stats;
            helper.assertTrue(stats.getElapsedTicks(helper.getLevel()) > 0,
                    "Elapsed time should be tracked after the grace period");
            helper.assertTrue(stats.getUptimeTicks() > 0,
                    "Mining uptime should be tracked after the grace period");
            helper.succeed();
        });
    }

    @GameTest(template = "empty16x16", timeoutTicks = 5)
    public void assembledNonMiningMinerFallsBackToStalled(GameTestHelper helper) {
        var miner = createMiner(helper);
        var displayLink = attachDisplayLink(helper);
        miner.assemble(TEST_RPM);

        helper.runAtTickTime(2, () -> {
            assertSimpleStatus(helper, displayLink, "Mining");
            miner.process().innerProcesses.clear();

            helper.assertFalse(miner.behavior().getClaimedDepositBlocks().isEmpty(),
                    "Stalled miner should retain claimed deposit blocks");
            helper.assertTrue(miner.bearing().isSpeedRequirementFulfilled(),
                    "Stalled miner should fulfill its speed requirement");
            assertSimpleStatus(helper, displayLink, "Stalled");
            helper.succeed();
        });
    }

    @GameTest(template = "empty16x16", timeoutTicks = 5)
    public void portableInterfaceConnectionReportsTransferring(GameTestHelper helper) {
        var miner = createInterfaceMiner(helper);
        var displayLink = attachDisplayLink(helper);
        miner.assemble(TEST_RPM);

        helper.runAtTickTime(2, () -> {
            var movedContraption = miner.bearing().getMovedContraption();
            helper.assertTrue(movedContraption != null, "Miner contraption should be assembled");
            movedContraption.setAngle(0);

            var actor = movedContraption.getContraption().getActors().stream()
                    .filter(candidate -> MovementBehaviour.REGISTRY.get(candidate.getLeft().state())
                            instanceof PortableStorageInterfaceMovement)
                    .findFirst()
                    .orElseThrow();
            var context = actor.getRight();
            helper.assertTrue(context != null, "Portable interface movement context should be initialized");

            helper.setBlock(STATIONARY_INTERFACE_POS, AllBlocks.PORTABLE_STORAGE_INTERFACE.getDefaultState()
                    .setValue(PortableStorageInterfaceBlock.FACING, Direction.WEST));
            context.data.put("WorkingPos", NbtUtils.writeBlockPos(helper.absolutePos(STATIONARY_INTERFACE_POS)));
            context.stall = true;
            movedContraption.tickActors();

            helper.assertTrue(movedContraption.isStalled(), "Portable interface should stall the miner contraption");
            helper.assertTrue(miner.behavior().isTransferring(),
                    "Mining behavior should identify the portable interface stall");
            assertSimpleStatus(helper, displayLink, "Transferring");
            helper.succeed();
        });
    }

    @GameTest(template = "empty16x16", timeoutTicks = 5)
    public void infiniteResourcesAreExcludedFromFiniteResourceCounts(GameTestHelper helper) {
        var miner = createMixedResourceMiner(helper);
        var displayLink = attachDisplayLink(helper);
        miner.assemble(TEST_RPM);

        helper.runAtTickTime(2, () -> {
            var process = miner.process();
            helper.assertTrue(process != null, "Mining process should be initialized");
            helper.assertValueEqual(process.innerProcesses.size(), 2, "mixed resource process count");

            var infiniteProcess = process.innerProcesses.stream()
                    .filter(innerProcess ->
                            innerProcess.recipe.getDepositBlock() == RNSDeposits.DEPLETED_DEPOSIT.get())
                    .findFirst()
                    .orElseThrow();
            var finiteProcess = process.innerProcesses.stream()
                    .filter(innerProcess -> innerProcess.recipe.getDepositBlock() == RNSDeposits.IRON_DEPOSIT.get())
                    .findFirst()
                    .orElseThrow();
            var remainingResources = miner.behavior().stats.remainingResources;
            remainingResources.put(infiniteProcess.recipe.getDepositBlock(), 0);
            remainingResources.put(finiteProcess.recipe.getDepositBlock(), FINITE_REMAINING_RESOURCES);

            withInfiniteDeposits(false, () -> {
                infiniteProcess.progress = infiniteProcess.maxProgress;
                miner.behavior().collect();

                helper.assertValueEqual(miner.behavior().stats.minedResources, 1L,
                        "total mined resources after mining an infinite resource");
                helper.assertValueEqual(miner.behavior().stats.consumedResources, 0L,
                        "consumed resources after mining an infinite resource");
                assertStatus(helper, displayLink, MINED_STYLE, "Mining (1 mined)");
                assertStatus(helper, displayLink, CONSUMED_STYLE, "Mining (0 consumed)");
                assertStatus(helper, displayLink, REMAINING_STYLE, "Mining (9 remaining)");

                finiteProcess.progress = finiteProcess.maxProgress;
                miner.behavior().collect();

                helper.assertValueEqual(miner.behavior().stats.minedResources, 2L,
                        "total mined resources after mining a finite resource");
                helper.assertValueEqual(miner.behavior().stats.consumedResources, 1L,
                        "consumed resources after mining a finite resource");
                assertStatus(helper, displayLink, MINED_STYLE, "Mining (2 mined)");
                assertStatus(helper, displayLink, CONSUMED_STYLE, "Mining (1 consumed)");
                assertStatus(helper, displayLink, REMAINING_STYLE, "Mining (8 remaining)");
            });
            helper.succeed();
        });
    }

    private static MinerSetup createMiner(GameTestHelper helper) {
        return MinerSetupBuilder.create(helper)
                .bearing(BEARING_POS.getX(), BEARING_POS.getY(), BEARING_POS.getZ())
                .part(AllBlocks.ANDESITE_CASING.getDefaultState(), 5, 4, 5)
                .head(5, 3, 5)
                .deposit(5, 2, 5)
                .place();
    }

    private static MinerSetup createMixedResourceMiner(GameTestHelper helper) {
        return MinerSetupBuilder.create(helper)
                .bearing(BEARING_POS.getX(), BEARING_POS.getY(), BEARING_POS.getZ())
                .part(AllBlocks.ANDESITE_CASING.getDefaultState(), 5, 4, 5)
                .head(5, 3, 5)
                .deposit(5, 2, 5)
                .deposit(6, 2, 5, RNSDeposits.IRON_DEPOSIT.get())
                .place();
    }

    private static MinerSetup createInterfaceMiner(GameTestHelper helper) {
        return MinerSetupBuilder.create(helper)
                .bearing(BEARING_POS.getX(), BEARING_POS.getY(), BEARING_POS.getZ())
                .part(AllBlocks.ANDESITE_CASING.getDefaultState(), 5, 4, 5)
                .part(AllBlocks.PORTABLE_STORAGE_INTERFACE.getDefaultState()
                        .setValue(PortableStorageInterfaceBlock.FACING, Direction.EAST), 6, 4, 5)
                .head(5, 3, 5)
                .deposit(5, 2, 5)
                .place();
    }

    private static DisplayLinkBlockEntity attachDisplayLink(GameTestHelper helper) {
        helper.setBlock(DISPLAY_LINK_POS, AllBlocks.DISPLAY_LINK.getDefaultState()
                .setValue(DisplayLinkBlock.FACING, Direction.EAST));
        var blockEntity = helper.getLevel().getBlockEntity(helper.absolutePos(DISPLAY_LINK_POS));
        helper.assertTrue(blockEntity instanceof DisplayLinkBlockEntity,
                "Expected a Display Link block entity next to the miner bearing");
        return (DisplayLinkBlockEntity) blockEntity;
    }

    private static void withInfiniteDeposits(boolean infiniteDeposits, Runnable action) {
        boolean previousValue = ServerConfig.INFINITE_DEPOSITS.get();
        ServerConfig.INFINITE_DEPOSITS.set(infiniteDeposits);
        try {
            action.run();
        } finally {
            ServerConfig.INFINITE_DEPOSITS.set(previousValue);
        }
    }

    private static void assertSimpleStatus(
            GameTestHelper helper, DisplayLinkBlockEntity displayLink, String expectedStatus
    ) {
        assertStatus(helper, displayLink, 0, expectedStatus);
    }

    private static void assertStatus(
            GameTestHelper helper, DisplayLinkBlockEntity displayLink, int style, String expectedStatus
    ) {
        var config = new CompoundTag();
        config.putInt("style", style);
        displayLink.setSourceConfig(config);

        var context = new DisplayLinkContext(helper.getLevel(), displayLink);
        var stats = new DisplayTargetStats(1, 128, AllDisplayTargets.NIXIE_TUBE.get());
        var text = RNSDisplaySources.COMPACT_MINER_STATUS.get().provideText(context, stats);
        helper.assertValueEqual(text.size(), 1, "compact status output row count");
        helper.assertValueEqual(text.getFirst().getString(), expectedStatus, "compact miner status");
    }
}
