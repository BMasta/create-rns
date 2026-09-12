package com.bmaster.createrns.content.deposit.mining;

import com.bmaster.createrns.CreateRNS;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CreateRNS.ID)
@PrefixGameTestTemplate(false)
public class MiningStatsGameTest {
    @GameTest(template = "empty16x16", timeoutTicks = MiningStats.UPTIME_GRACE_PERIOD_TICKS + 5)
    public void uptimeStartsOnFirstTickAfterGracePeriod(GameTestHelper helper) {
        var level = helper.getLevel();
        var stats = new MiningStats();
        stats.start(level);

        helper.runAtTickTime(MiningStats.UPTIME_GRACE_PERIOD_TICKS - 1, () -> {
            helper.assertFalse(stats.updateUptimeTicks(level, true),
                    "Uptime should not change during the grace period");
            helper.assertValueEqual(stats.getElapsedTicks(level), 0L,
                    "Elapsed time should not include the grace period");
            helper.assertValueEqual(stats.getUptimeTicks(), 0L,
                    "Uptime should not include the grace period");
        });

        helper.runAtTickTime(MiningStats.UPTIME_GRACE_PERIOD_TICKS, () -> {
            helper.assertTrue(stats.updateUptimeTicks(level, true),
                    "Uptime should change on the first tick after the grace period");
            helper.assertValueEqual(stats.getElapsedTicks(level), 0L,
                    "No full tick should have elapsed since the grace period ended");
            helper.assertValueEqual(stats.getUptimeTicks(), 1L,
                    "The first mining tick after the grace period should be recorded");
            helper.succeed();
        });
    }
}
