package com.bmaster.createrns.content.deposit.mining.display;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.RNSDisplaySources;
import com.bmaster.createrns.util.CodecHelper;
import com.bmaster.createrns.util.MinerSetupBuilder;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllDisplayTargets;
import com.simibubi.create.api.behaviour.display.DisplaySource;
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
import net.minecraft.world.item.Items;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Locale;

@GameTestHolder(CreateRNS.ID)
@PrefixGameTestTemplate(false)
public class EstimatedMinerProductionRatesDisplaySourceGameTest {
    private static final int TEST_RPM = 128;
    private static final int TOO_SLOW_RPM = 1;
    private static final int STYLE_COUNT = 4;
    private static final BlockPos BEARING_POS = new BlockPos(5, 5, 5);
    private static final BlockPos DISPLAY_LINK_POS = BEARING_POS.east();

    @GameTest(template = "empty16x16", timeoutTicks = 8)
    public void everyStyleFormatsRunningAndStoppedProductionRates(GameTestHelper helper) {
        var miner = MinerSetupBuilder.create(helper)
                .bearing(BEARING_POS.getX(), BEARING_POS.getY(), BEARING_POS.getZ())
                .part(AllBlocks.ANDESITE_CASING.getDefaultState(), 5, 4, 5)
                .head(5, 3, 5)
                .deposit(5, 2, 5)
                .place();
        var displayLink = attachDisplayLink(helper);

        var sources = DisplaySource.getAll(helper.getLevel(), helper.absolutePos(BEARING_POS));
        helper.assertTrue(sources.contains(RNSDisplaySources.ESTIMATED_MINER_PRODUCTION_RATES.get()),
                "Miner bearing should advertise estimated production rate display support");

        miner.assemble(TEST_RPM);
        helper.runAtTickTime(2, () -> {
            var process = miner.process();
            helper.assertTrue(process != null, "Mining process should be initialized");
            process.innerProcesses.forEach(innerProcess -> innerProcess.catalystHandler.useCatalysts(true));

            var rates = process.getEstimatedRates(miner.behavior().getCurrentProgressIncrement());
            CodecHelper.assertValueEqual(helper, rates.size(), 1, "estimated production rate count");
            helper.assertTrue(rates.containsKey(Items.COBBLESTONE),
                    "Depleted deposit should report cobblestone production");

            String rate = String.format(Locale.ROOT, "%.1f", rates.getFloat(Items.COBBLESTONE));
            assertStyles(helper, displayLink, expectedLines(rate));

            var motor = helper.getLevel().getBlockEntity(helper.absolutePos(miner.motorPos()));
            helper.assertTrue(motor instanceof CreativeMotorBlockEntity,
                    "Expected a Creative Motor block entity above the miner bearing");
            ((CreativeMotorBlockEntity) motor).generatedSpeed.setValue(-TOO_SLOW_RPM);
        });

        helper.runAtTickTime(5, () -> {
            helper.assertFalse(miner.bearing().isSpeedRequirementFulfilled(),
                    "Miner RPM should not fulfill its speed requirement");
            helper.assertFalse(miner.behavior().isMining(), "Miner should not be mining below its required RPM");
            assertStyles(helper, displayLink, expectedLines("0.0"));
            helper.succeed();
        });
    }

    private static List<List<String>> expectedLines(String rate) {
        String itemName = Items.COBBLESTONE.getDescription().getString();
        return List.of(
                List.of(rate + "/hr"),
                List.of(rate + " " + itemName + "/hr"),
                List.of(rate + "/hr", ""),
                List.of(rate + " " + itemName + "/hr", "")
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
        var text = RNSDisplaySources.ESTIMATED_MINER_PRODUCTION_RATES.get().provideText(context, targetStats);

        CodecHelper.assertValueEqual(helper, text.size(), expectedLines.size(), "estimated production rate output row count");
        for (int row = 0; row < expectedLines.size(); row++) {
            CodecHelper.assertValueEqual(helper, text.get(row).getString(), expectedLines.get(row),
                    "estimated production rate output text at row " + row);
        }
    }

    private static void assertStyles(
            GameTestHelper helper, DisplayLinkBlockEntity displayLink, List<List<String>> expectedLines
    ) {
        for (int style = 0; style < STYLE_COUNT; style++) {
            assertStyle(helper, displayLink, style, expectedLines.get(style));
        }
    }
}
