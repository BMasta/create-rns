package com.bmaster.createrns.content.deposit.mining.display;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.RNSDisplaySources;
import com.bmaster.createrns.content.deposit.mining.MiningStats;
import com.bmaster.createrns.util.MinerSetup;
import com.bmaster.createrns.util.MinerSetupBuilder;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllDisplayTargets;
import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkBlock;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkBlockEntity;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Locale;

@GameTestHolder(CreateRNS.ID)
@PrefixGameTestTemplate(false)
public class MinerRatesDisplaySourceGameTest {
    private static final int TEST_RPM = 128;
    private static final int TOO_SLOW_RPM = 1;
    private static final int ESTIMATED_TYPE = 0;
    private static final int ACTUAL_TYPE = 1;
    private static final int ACTUAL_RATE_SAMPLE_DELAY_TICKS = 20;
    private static final long ACTUAL_MINED_ITEMS = 1;
    private static final BlockPos BEARING_POS = new BlockPos(5, 5, 5);
    private static final BlockPos DISPLAY_LINK_POS = BEARING_POS.east();

    @GameTest(template = "empty16x16", timeoutTicks = 8)
    public void estimatedTypeFormatsRunningAndStoppedMiningRates(GameTestHelper helper) {
        var miner = createMiner(helper);
        var displayLink = attachDisplayLink(helper);

        var sources = DisplaySource.getAll(helper.getLevel(), helper.absolutePos(BEARING_POS));
        helper.assertTrue(sources.contains(RNSDisplaySources.ESTIMATED_MINER_RATES.get()),
                "Miner bearing should advertise mining rate display support");

        miner.assemble(TEST_RPM);
        helper.runAtTickTime(2, () -> {
            var process = miner.process();
            helper.assertTrue(process != null, "Mining process should be initialized");
            var stats = miner.behavior().stats;
            process.innerProcesses.forEach(innerProcess -> {
                var catalystStats = stats.catStats.ofInnerProcess(innerProcess);
                helper.assertTrue(catalystStats.isComputed(),
                        "Catalyst statistics should be initialized for estimated rates");
            });

            var rates = stats.getEstimatedRates(process, miner.behavior().getCurrentProgressIncrement());
            helper.assertValueEqual(rates.size(), 1, "estimated mining rate count");
            helper.assertTrue(rates.containsKey(Items.COBBLESTONE),
                    "Depleted deposit should report cobblestone production");

            String rate = String.format(Locale.ROOT, "%.1f", rates.get(Items.COBBLESTONE));
            assertType(helper, displayLink, ESTIMATED_TYPE, expectedLines(rate), "estimated rate");

            stopMiner(helper, miner);
        });

        helper.runAtTickTime(5, () -> {
            helper.assertFalse(miner.bearing().isSpeedRequirementFulfilled(),
                    "Miner RPM should not fulfill its speed requirement");
            helper.assertFalse(miner.behavior().isMining(), "Miner should not be mining below its required RPM");
            assertType(helper, displayLink, ESTIMATED_TYPE, expectedLines("0.0"), "stopped estimated rate");
            helper.succeed();
        });
    }

    @GameTest(
            template = "empty16x16",
            timeoutTicks = MiningStats.UPTIME_GRACE_PERIOD_TICKS + ACTUAL_RATE_SAMPLE_DELAY_TICKS + 10
    )
    public void actualTypeUsesMinedItemsAndElapsedTime(GameTestHelper helper) {
        var miner = createMiner(helper);
        var displayLink = attachDisplayLink(helper);
        miner.assemble(TEST_RPM);

        helper.runAtTickTime(2, () -> {
            var process = miner.process();
            helper.assertTrue(process != null, "Mining process should be initialized");

            stopMiner(helper, miner);
        });

        int assertionTick = MiningStats.UPTIME_GRACE_PERIOD_TICKS + ACTUAL_RATE_SAMPLE_DELAY_TICKS;
        helper.runAtTickTime(assertionTick, () -> {
            var stats = miner.behavior().stats;
            stats.minedItems.addTo(Items.COBBLESTONE, ACTUAL_MINED_ITEMS);
            long elapsedTicks = stats.getElapsedTicks(helper.getLevel());
            helper.assertTrue(elapsedTicks > 0, "Actual rates should have a positive measurement period");

            double elapsedHours = elapsedTicks
                    / (double) (SharedConstants.TICKS_PER_MINUTE * 60);
            String rate = String.format(Locale.ROOT, "%.1f", ACTUAL_MINED_ITEMS / elapsedHours);
            assertType(helper, displayLink, ACTUAL_TYPE, expectedLines(rate), "actual rate");
            assertType(helper, displayLink, ESTIMATED_TYPE, expectedLines("0.0"), "estimated rate selection");
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

    private static void stopMiner(GameTestHelper helper, MinerSetup miner) {
        var motor = helper.getLevel().getBlockEntity(helper.absolutePos(miner.motorPos()));
        helper.assertTrue(motor instanceof CreativeMotorBlockEntity,
                "Expected a Creative Motor block entity above the miner bearing");
        ((CreativeMotorBlockEntity) motor).generatedSpeed.setValue(-TOO_SLOW_RPM);
    }

    private static List<String> expectedLines(String rate) {
        String itemName = Items.COBBLESTONE.getDescription().getString();
        return List.of(itemName + ": " + rate + "/hr");
    }

    private static DisplayLinkBlockEntity attachDisplayLink(GameTestHelper helper) {
        helper.setBlock(DISPLAY_LINK_POS, AllBlocks.DISPLAY_LINK.getDefaultState()
                .setValue(DisplayLinkBlock.FACING, Direction.EAST));
        var blockEntity = helper.getLevel().getBlockEntity(helper.absolutePos(DISPLAY_LINK_POS));
        helper.assertTrue(blockEntity instanceof DisplayLinkBlockEntity,
                "Expected a Display Link block entity next to the miner bearing");
        return (DisplayLinkBlockEntity) blockEntity;
    }

    private static void assertType(
            GameTestHelper helper, DisplayLinkBlockEntity displayLink, int type, List<String> expectedLines,
            String description
    ) {
        var config = new CompoundTag();
        config.putInt("type", type);
        displayLink.setSourceConfig(config);

        var context = new DisplayLinkContext(helper.getLevel(), displayLink);
        var targetStats = new DisplayTargetStats(expectedLines.size(), 128, AllDisplayTargets.NIXIE_TUBE.get());
        var source = RNSDisplaySources.ESTIMATED_MINER_RATES.get();
        var text = source.provideText(context, targetStats);

        helper.assertValueEqual(text.size(), expectedLines.size(), description + " output row count");
        for (int row = 0; row < expectedLines.size(); row++) {
            helper.assertValueEqual(text.get(row).getString(), expectedLines.get(row),
                    description + " output text at row " + row);
        }
        MinerDisplaySourceGameTestAssertions.assertTwoColumnFlapText(
                helper, context, source.provideFlapDisplayText(context, targetStats), expectedLines, description);
    }
}
