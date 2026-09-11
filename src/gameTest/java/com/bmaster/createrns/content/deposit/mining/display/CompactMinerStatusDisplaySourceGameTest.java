package com.bmaster.createrns.content.deposit.mining.display;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.RNSDeposits;
import com.bmaster.createrns.RNSDisplaySources;
import com.bmaster.createrns.util.CodecHelper;
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
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Set;

@GameTestHolder(CreateRNS.ID)
@PrefixGameTestTemplate(false)
public class CompactMinerStatusDisplaySourceGameTest {
    private static final int TEST_RPM = 128;
    private static final int TOO_SLOW_RPM = 1;
    private static final int UPTIME_STYLE = 1;
    private static final int MINED_RESOURCES_STYLE = 2;
    private static final int REMAINING_RESOURCES_STYLE = 3;
    private static final int MINED_PERCENTAGE_STYLE = 4;
    private static final long FINITE_REMAINING_RESOURCES = 9;
    private static final BlockPos BEARING_POS = new BlockPos(5, 5, 5);
    private static final BlockPos DISPLAY_LINK_POS = BEARING_POS.east();
    private static final BlockPos STATIONARY_INTERFACE_POS = new BlockPos(10, 4, 5);

    private static final List<String> EXPECTED_LINES = List.of(
            "Not Assembled",
            "Not Assembled (100% uptime)",
            "Not Assembled (0 mined)",
            "Not Assembled (0 remaining)",
            "Not Assembled (100% mined)"
    );

    @GameTest(template = "empty16x16", timeoutTicks = 5)
    public void everyStyleProducesOneCompactLine(GameTestHelper helper) {
        createMiner(helper);
        var displayLink = attachDisplayLink(helper);
        var context = new DisplayLinkContext(helper.getLevel(), displayLink);
        var stats = new DisplayTargetStats(1, 128, AllDisplayTargets.NIXIE_TUBE.get());
        var source = RNSDisplaySources.COMPACT_MINER_STATUS.get();

        for (int style = 0; style < EXPECTED_LINES.size(); style++) {
            var config = new CompoundTag();
            config.putInt("style", style);
            displayLink.setSourceConfig(config);

            var text = source.provideText(context, stats);
            CodecHelper.assertValueEqual(helper, text.size(), 1, "output row count for style " + style);
            CodecHelper.assertValueEqual(helper, text.get(0).getString(), EXPECTED_LINES.get(style),
                    "output text for style " + style);
        }

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
        CodecHelper.assertValueEqual(helper, text.size(), 1, "labeled compact status output row count");
        CodecHelper.assertValueEqual(helper, text.get(0).getString(), "Miner A: Not Assembled",
                "labeled compact miner status");
        helper.succeed();
    }

    @GameTest(template = "empty16x16", timeoutTicks = 5)
    public void assembledMinerWithoutClaimsReportsOutOfResources(GameTestHelper helper) {
        var miner = createMiner(helper);
        var displayLink = attachDisplayLink(helper);
        miner.assemble(TEST_RPM);

        helper.runAtTickTime(2, () -> {
            miner.behavior().setClaimedDepositBlocks(Set.of());
            helper.assertTrue(miner.isRunning(), "Miner should be assembled");
            helper.assertTrue(miner.behavior().getClaimedDepositBlocks().isEmpty(),
                    "Miner should not have any claimed deposit blocks");
            assertSimpleStatus(helper, displayLink, "Out of Resources");
            helper.succeed();
        });
    }

    @GameTest(template = "empty16x16", timeoutTicks = 5)
    public void assembledMinerWithInfiniteResourcesReportsInfiniteRemaining(GameTestHelper helper) {
        var miner = createMiner(helper);
        var displayLink = attachDisplayLink(helper);
        miner.assemble(TEST_RPM);

        helper.runAtTickTime(2, () -> {
            miner.behavior().stats.remainingResources = 0;
            assertStatus(helper, displayLink, REMAINING_RESOURCES_STYLE, "Mining (infinite)");
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
            assertStatus(helper, displayLink, UPTIME_STYLE, "Mining (100% uptime)");

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
    public void infiniteResourcesAreExcludedFromRemainingCountAndPercentage(GameTestHelper helper) {
        var miner = createMixedResourceMiner(helper);
        var displayLink = attachDisplayLink(helper);
        miner.assemble(TEST_RPM);

        helper.runAtTickTime(2, () -> {
            var process = miner.process();
            helper.assertTrue(process != null, "Mining process should be initialized");
            CodecHelper.assertValueEqual(helper, process.innerProcesses.size(), 2, "mixed resource process count");

            var infiniteProcess = process.innerProcesses.stream()
                    .filter(innerProcess ->
                            innerProcess.recipe.getDepositBlock() == RNSDeposits.DEPLETED_DEPOSIT.get())
                    .findFirst()
                    .orElseThrow();
            var finiteProcess = process.innerProcesses.stream()
                    .filter(innerProcess -> innerProcess.recipe.getDepositBlock() == RNSDeposits.IRON_DEPOSIT.get())
                    .findFirst()
                    .orElseThrow();
            infiniteProcess.remainingUses = 0;
            finiteProcess.remainingUses = FINITE_REMAINING_RESOURCES;

            infiniteProcess.progress = infiniteProcess.maxProgress;
            miner.behavior().collect();

            CodecHelper.assertValueEqual(helper, miner.behavior().stats.minedResources, 1L,
                    "total mined resources after mining an infinite resource");
            CodecHelper.assertValueEqual(helper, miner.behavior().stats.consumedResources, 0L,
                    "consumed resources after mining an infinite resource");
            assertStatus(helper, displayLink, MINED_RESOURCES_STYLE, "Mining (1 mined)");
            assertStatus(helper, displayLink, REMAINING_RESOURCES_STYLE, "Mining (9 remaining)");
            assertStatus(helper, displayLink, MINED_PERCENTAGE_STYLE, "Mining (0% mined)");

            finiteProcess.progress = finiteProcess.maxProgress;
            miner.behavior().collect();

            CodecHelper.assertValueEqual(helper, miner.behavior().stats.minedResources, 2L,
                    "total mined resources after mining a finite resource");
            CodecHelper.assertValueEqual(helper, miner.behavior().stats.consumedResources, 1L,
                    "consumed resources after mining a finite resource");
            assertStatus(helper, displayLink, REMAINING_RESOURCES_STYLE, "Mining (8 remaining)");
            assertStatus(helper, displayLink, MINED_PERCENTAGE_STYLE, "Mining (11% mined)");
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
        CodecHelper.assertValueEqual(helper, text.size(), 1, "compact status output row count");
        CodecHelper.assertValueEqual(helper, text.get(0).getString(), expectedStatus, "compact miner status");
    }
}
