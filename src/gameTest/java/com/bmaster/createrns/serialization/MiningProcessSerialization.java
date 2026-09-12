package com.bmaster.createrns.serialization;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.RNSDeposits;
import com.bmaster.createrns.content.deposit.info.DepositDurabilityManager;
import com.bmaster.createrns.content.deposit.mining.InnerProcess;
import com.bmaster.createrns.content.deposit.mining.MiningProcess;
import com.bmaster.createrns.content.deposit.mining.MiningStats;
import com.bmaster.createrns.infrastructure.ServerConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.Tag;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import javax.annotation.Nullable;
import java.util.Set;

@GameTestHolder(CreateRNS.ID)
@PrefixGameTestTemplate(false)
public class MiningProcessSerialization {
    private static final BlockPos DEPOSIT_POS = new BlockPos(1, 1, 1);
    private static final long REMAINING_RESOURCES = 55L;
    private static final int PROGRESS = 77;

    @GameTest(template = "empty16x16")
    public void miningProcessInitializesFiniteRemainingResources(GameTestHelper helper) {
        withInfiniteDeposits(false, () -> assertRemainingResourcesInitialization(helper, REMAINING_RESOURCES, true));
        helper.succeed();
    }

    @GameTest(template = "empty16x16")
    public void miningProcessInitializesInfiniteRemainingResources(GameTestHelper helper) {
        withInfiniteDeposits(true, () -> assertRemainingResourcesInitialization(helper, 0L, false));
        helper.succeed();
    }

    @GameTest(template = "empty16x16")
    public void miningProcessDiskSerializationPreservesProgress(GameTestHelper helper) {
        var level = helper.getLevel();
        var provider = level.registryAccess();
        var original = createProcess(helper, null);
        firstInnerProcess(original).progress = PROGRESS;

        var serialized = original.write(provider, false);
        helper.assertTrue(serialized != null, "Disk serialization should produce a mining process tag");

        var serializedInnerProcess = serialized.getList("inner_processes", Tag.TAG_COMPOUND).getCompound(0);
        helper.assertTrue(serializedInnerProcess.contains("progress"), "Disk serialization should persist progress");
        helper.assertFalse(serializedInnerProcess.contains("remaining_uses"),
                "Disk serialization should not persist remaining resources");
        helper.assertFalse(serializedInnerProcess.contains("catalyst_stats"),
                "Disk serialization should not persist catalyst stats");

        var restored = createProcess(helper, null);
        restored.read(serialized, provider, false);
        var restoredInnerProcess = firstInnerProcess(restored);

        helper.assertValueEqual(restoredInnerProcess.progress, PROGRESS, "restored mining progress");
        helper.assertFalse(restoredInnerProcess.catStats.isChancesComputed(),
                "Disk deserialization should not restore client-only catalyst stats");
        helper.succeed();
    }

    @GameTest(template = "empty16x16")
    public void miningProcessClientPacketSerializationPreservesCatalystStats(GameTestHelper helper) {
        var level = helper.getLevel();
        var provider = level.registryAccess();
        var original = createProcess(helper, null);
        firstInnerProcess(original).progress = PROGRESS;

        var serialized = original.write(provider, true);
        helper.assertTrue(serialized != null, "Client sync serialization should produce a mining process tag");

        var serializedInnerProcess = serialized.getList("inner_processes", Tag.TAG_COMPOUND).getCompound(0);
        helper.assertFalse(serializedInnerProcess.contains("progress"),
                "Client sync serialization should not persist progress");
        helper.assertFalse(serializedInnerProcess.contains("remaining_uses"),
                "Client sync serialization should not persist remaining resources");
        helper.assertTrue(serializedInnerProcess.contains("catalyst_stats"),
                "Client sync serialization should include catalyst stats");

        var restored = createProcess(helper, null);
        restored.read(serialized, provider, true);
        var restoredInnerProcess = firstInnerProcess(restored);

        helper.assertValueEqual(restoredInnerProcess.progress, 0, "client-synced mining progress");
        helper.assertTrue(restoredInnerProcess.catStats.isChancesComputed(),
                "Client sync deserialization should restore catalyst stats");
        helper.succeed();
    }

    private static void assertRemainingResourcesInitialization(
            GameTestHelper helper, long expectedRemainingResources, boolean expectDurabilitySeeded
    ) {
        var level = helper.getLevel();
        var absoluteDepositPos = helper.absolutePos(DEPOSIT_POS);
        helper.setBlock(DEPOSIT_POS, RNSDeposits.IRON_DEPOSIT.get().defaultBlockState());

        boolean durabilitySeeded = DepositDurabilityManager.set(level, absoluteDepositPos, REMAINING_RESOURCES);
        helper.assertValueEqual(durabilitySeeded, expectDurabilitySeeded,
                "whether deposit durability seeding should succeed");

        var stats = new MiningStats();
        var process = new MiningProcess(level, Set.of(), Set.of(absoluteDepositPos), null, stats);
        helper.assertTrue(process.isPossible(), "Mining process should initialize for a valid deposit block");
        helper.assertTrue(stats.remainingResources.containsKey(RNSDeposits.IRON_DEPOSIT.get()),
                "Statistics should identify the initialized deposit block");
        helper.assertValueEqual(stats.remainingResources.getLong(RNSDeposits.IRON_DEPOSIT.get()),
                expectedRemainingResources, "initialized remaining resources");
    }

    private static MiningProcess createProcess(GameTestHelper helper, @Nullable MiningStats stats) {
        var absoluteDepositPos = helper.absolutePos(DEPOSIT_POS);
        helper.setBlock(DEPOSIT_POS, RNSDeposits.IRON_DEPOSIT.get().defaultBlockState());
        var process = new MiningProcess(helper.getLevel(), Set.of(), Set.of(absoluteDepositPos), null, stats);
        helper.assertTrue(process.isPossible(), "Mining process should initialize for a valid deposit block");
        return process;
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

    private static InnerProcess firstInnerProcess(MiningProcess process) {
        return process.innerProcesses.stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("Mining process did not contain any inner processes"));
    }
}
