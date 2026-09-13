package com.bmaster.createrns.content.deposit.mining.display;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.RNSDeposits;
import com.bmaster.createrns.RNSDisplaySources;
import com.bmaster.createrns.infrastructure.ServerConfig;
import com.bmaster.createrns.util.MinerSetupBuilder;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllDisplayTargets;
import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkBlock;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkBlockEntity;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

@GameTestHolder(CreateRNS.ID)
@PrefixGameTestTemplate(false)
public class RemainingMinerResourcesDisplaySourceGameTest {
    private static final int TEST_RPM = 128;
    private static final int STYLE_COUNT = 1;
    private static final long IRON_RESOURCES = 1_234;
    private static final long COAL_RESOURCES = 9;
    private static final BlockPos BEARING_POS = new BlockPos(5, 5, 5);
    private static final BlockPos DISPLAY_LINK_POS = BEARING_POS.east();

    @GameTest(template = "empty16x16", timeoutTicks = 5)
    public void defaultStyleFormatsFiniteAndInfiniteResources(GameTestHelper helper) {
        var miner = MinerSetupBuilder.create(helper)
                .bearing(BEARING_POS.getX(), BEARING_POS.getY(), BEARING_POS.getZ())
                .part(AllBlocks.ANDESITE_CASING.getDefaultState(), 5, 4, 5)
                .head(5, 3, 5)
                .deposit(5, 2, 5)
                .place();
        var displayLink = attachDisplayLink(helper);

        var sources = DisplaySource.getAll(helper.getLevel(), helper.absolutePos(BEARING_POS));
        helper.assertTrue(sources.contains(RNSDisplaySources.REMAINING_MINER_RESOURCES.get()),
                "Miner bearing should advertise remaining resource display support");

        miner.assemble(TEST_RPM);
        helper.runAtTickTime(2, () -> {
            var remainingResources = miner.behavior().stats.remainingResources;
            remainingResources.clear();
            remainingResources.put(RNSDeposits.IRON_DEPOSIT.get(), IRON_RESOURCES);
            remainingResources.put(RNSDeposits.COAL_DEPOSIT.get(), COAL_RESOURCES);
            remainingResources.put(RNSDeposits.DEPLETED_DEPOSIT.get(), 0);

            withInfiniteDeposits(false, () -> assertStyles(helper, displayLink, expectedFiniteLines()));
            withInfiniteDeposits(true, () -> assertStyles(helper, displayLink, expectedInfiniteLines()));
            helper.succeed();
        });
    }

    private static List<List<String>> expectedFiniteLines() {
        String ironDeposit = RNSDeposits.IRON_DEPOSIT.get().getName().getString();
        String coalDeposit = RNSDeposits.COAL_DEPOSIT.get().getName().getString();
        String depletedDeposit = RNSDeposits.DEPLETED_DEPOSIT.get().getName().getString();
        return List.of(
                List.of(ironDeposit + ": 1.2k", coalDeposit + ": 9", depletedDeposit + ": ∞")
        );
    }

    private static List<List<String>> expectedInfiniteLines() {
        String coalDeposit = RNSDeposits.COAL_DEPOSIT.get().getName().getString();
        String depletedDeposit = RNSDeposits.DEPLETED_DEPOSIT.get().getName().getString();
        String ironDeposit = RNSDeposits.IRON_DEPOSIT.get().getName().getString();
        return List.of(
                List.of(coalDeposit + ": ∞", depletedDeposit + ": ∞", ironDeposit + ": ∞")
        );
    }

    private static DisplayLinkBlockEntity attachDisplayLink(GameTestHelper helper) {
        helper.setBlock(DISPLAY_LINK_POS, AllBlocks.DISPLAY_LINK.getDefaultState()
                .setValue(DisplayLinkBlock.FACING, Direction.EAST));
        var blockEntity = helper.getLevel().getBlockEntity(helper.absolutePos(DISPLAY_LINK_POS));
        helper.assertTrue(blockEntity instanceof DisplayLinkBlockEntity,
                "Expected a Display Link block entity next to the miner bearing");
        return (DisplayLinkBlockEntity) blockEntity;
    }

    private static void assertStyle(
            GameTestHelper helper, DisplayLinkBlockEntity displayLink, int style, List<String> expectedLines
    ) {
        var config = new CompoundTag();
        config.putInt("style", style);
        displayLink.setSourceConfig(config);

        var context = new DisplayLinkContext(helper.getLevel(), displayLink);
        var targetStats = new DisplayTargetStats(expectedLines.size(), 128, AllDisplayTargets.NIXIE_TUBE.get());
        var source = RNSDisplaySources.REMAINING_MINER_RESOURCES.get();
        var text = source.provideText(context, targetStats);

        helper.assertValueEqual(text.size(), expectedLines.size(), "remaining resource output row count");
        for (int row = 0; row < expectedLines.size(); row++) {
            helper.assertValueEqual(text.get(row).getString(), expectedLines.get(row),
                    "remaining resource output text at row " + row);
        }
        MinerDisplaySourceGameTestAssertions.assertTwoColumnFlapText(
                helper, context, source.provideFlapDisplayText(context, targetStats), expectedLines,
                "remaining resource");
    }

    private static void assertStyles(
            GameTestHelper helper, DisplayLinkBlockEntity displayLink, List<List<String>> expectedLines
    ) {
        for (int style = 0; style < STYLE_COUNT; style++) {
            assertStyle(helper, displayLink, style, expectedLines.get(style));
        }
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
}
