package com.bmaster.createrns.content.deposit.mining.display;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.RNSDisplaySources;
import com.bmaster.createrns.util.MinerSetup;
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
import java.util.Set;

@GameTestHolder(CreateRNS.ID)
@PrefixGameTestTemplate(false)
public class FullMinerStatusDisplaySourceGameTest {
    private static final int TEST_RPM = 128;
    private static final long ELAPSED_TICKS = 10;
    private static final long UPTIME_TICKS = 8;
    private static final long MINED_RESOURCES = 12;
    private static final long CONSUMED_RESOURCES = 3;
    private static final long REMAINING_RESOURCES = 7;
    private static final BlockPos BEARING_POS = new BlockPos(5, 5, 5);
    private static final BlockPos DISPLAY_LINK_POS = BEARING_POS.east();

    private static final List<String> FINITE_RESOURCE_LINES = List.of(
            "Status: Mining",
            "Uptime: 80%",
            "Mined Resources: 12 (30%)",
            "Remaining Resources: 7"
    );
    private static final List<String> INFINITE_RESOURCE_LINES = List.of(
            "Status: Mining",
            "Uptime: 100%",
            "Mined Resources: 0 (100%)",
            "Remaining Resources: Infinite"
    );
    private static final List<String> OUT_OF_RESOURCES_LINES = List.of(
            "Status: Out of Resources",
            "Uptime: 100%",
            "Mined Resources: 0",
            "Remaining Resources: 0"
    );

    @GameTest(template = "empty16x16", timeoutTicks = 5)
    public void fullStatusProvidesFourUnlabeledRows(GameTestHelper helper) {
        var miner = createMiner(helper);
        var displayLink = attachDisplayLink(helper);
        var config = new CompoundTag();
        config.putString("Label", "Miner A:");
        displayLink.setSourceConfig(config);

        var sources = DisplaySource.getAll(helper.getLevel(), helper.absolutePos(BEARING_POS));
        helper.assertTrue(sources.contains(RNSDisplaySources.FULL_MINER_STATUS.get()),
                "Miner bearing should advertise full status display support");

        miner.assemble(TEST_RPM);
        helper.runAtTickTime(2, () -> {
            var level = helper.getLevel();
            var statsTag = new CompoundTag();
            statsTag.putLong("start_time", level.getGameTime() - ELAPSED_TICKS);
            statsTag.putLong("uptime", UPTIME_TICKS);
            statsTag.putLong("mined_resources", MINED_RESOURCES);
            statsTag.putLong("consumed_resources", CONSUMED_RESOURCES);
            statsTag.putLong("remaining_resources", REMAINING_RESOURCES);
            miner.behavior().stats.deserializeNBT(level.registryAccess(), statsTag);

            assertFullStatus(helper, displayLink, FINITE_RESOURCE_LINES);
            helper.succeed();
        });
    }

    @GameTest(template = "empty16x16", timeoutTicks = 5)
    public void fullStatusDistinguishesInfiniteAndExhaustedResources(GameTestHelper helper) {
        var miner = createMiner(helper);
        var displayLink = attachDisplayLink(helper);
        miner.assemble(TEST_RPM);

        helper.runAtTickTime(2, () -> {
            miner.behavior().stats.remainingResources = 0;
            assertFullStatus(helper, displayLink, INFINITE_RESOURCE_LINES);

            miner.behavior().setClaimedDepositBlocks(Set.of(), false);
            assertFullStatus(helper, displayLink, OUT_OF_RESOURCES_LINES);
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

    private static void assertFullStatus(
            GameTestHelper helper, DisplayLinkBlockEntity displayLink, List<String> expectedLines
    ) {
        var context = new DisplayLinkContext(helper.getLevel(), displayLink);
        var targetStats = new DisplayTargetStats(4, 128, AllDisplayTargets.NIXIE_TUBE.get());
        var text = RNSDisplaySources.FULL_MINER_STATUS.get().provideText(context, targetStats);

        helper.assertValueEqual(text.size(), expectedLines.size(), "full status output row count");
        for (int row = 0; row < expectedLines.size(); row++) {
            helper.assertValueEqual(text.get(row).getString(), expectedLines.get(row),
                    "full status output text at row " + row);
        }
    }

    private static DisplayLinkBlockEntity attachDisplayLink(GameTestHelper helper) {
        helper.setBlock(DISPLAY_LINK_POS, AllBlocks.DISPLAY_LINK.getDefaultState()
                .setValue(DisplayLinkBlock.FACING, Direction.EAST));
        var blockEntity = helper.getLevel().getBlockEntity(helper.absolutePos(DISPLAY_LINK_POS));
        helper.assertTrue(blockEntity instanceof DisplayLinkBlockEntity,
                "Expected a Display Link block entity next to the miner bearing");
        return (DisplayLinkBlockEntity) blockEntity;
    }
}
