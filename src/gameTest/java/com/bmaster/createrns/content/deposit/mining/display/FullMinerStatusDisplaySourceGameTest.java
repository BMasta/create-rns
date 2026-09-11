package com.bmaster.createrns.content.deposit.mining.display;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.RNSDisplaySources;
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
public class FullMinerStatusDisplaySourceGameTest {
    private static final int TEST_RPM = 128;
    private static final long ELAPSED_TICKS = 10;
    private static final long UPTIME_TICKS = 8;
    private static final long MINED_RESOURCES = 12;
    private static final long CONSUMED_RESOURCES = 3;
    private static final long REMAINING_RESOURCES = 7;
    private static final BlockPos BEARING_POS = new BlockPos(5, 5, 5);
    private static final BlockPos DISPLAY_LINK_POS = BEARING_POS.east();

    private static final List<String> EXPECTED_LINES = List.of(
            "Status: Mining",
            "Resources mined: 12 (30%)",
            "Resources remaining: 7",
            "Uptime: 80%"
    );

    @GameTest(template = "empty16x16", timeoutTicks = 5)
    public void fullStatusProvidesFourUnlabeledRows(GameTestHelper helper) {
        var miner = MinerSetupBuilder.create(helper)
                .bearing(BEARING_POS.getX(), BEARING_POS.getY(), BEARING_POS.getZ())
                .part(AllBlocks.ANDESITE_CASING.getDefaultState(), 5, 4, 5)
                .head(5, 3, 5)
                .deposit(5, 2, 5)
                .place();
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

            var context = new DisplayLinkContext(level, displayLink);
            var targetStats = new DisplayTargetStats(4, 128, AllDisplayTargets.NIXIE_TUBE.get());
            var text = RNSDisplaySources.FULL_MINER_STATUS.get().provideText(context, targetStats);

            helper.assertValueEqual(text.size(), EXPECTED_LINES.size(), "full status output row count");
            for (int row = 0; row < EXPECTED_LINES.size(); row++) {
                helper.assertValueEqual(text.get(row).getString(), EXPECTED_LINES.get(row),
                        "full status output text at row " + row);
            }
            helper.succeed();
        });
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
