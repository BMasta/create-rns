package com.bmaster.createrns.serialization;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.RNSDeposits;
import com.bmaster.createrns.content.deposit.info.DepositDurabilityManager;
import com.bmaster.createrns.content.deposit.mining.MiningProcess;
import com.bmaster.createrns.infrastructure.ServerConfig;
import com.bmaster.createrns.util.CodecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.Tag;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.Set;

@GameTestHolder(CreateRNS.ID)
@PrefixGameTestTemplate(false)
public class MiningProcessSerialization {
    private static final BlockPos DEPOSIT_POS = new BlockPos(1, 1, 1);
    private static final long REMAINING_USES = 55L;
    private static final int PROGRESS = 77;

    @GameTest(template = "empty16x16")
    public void miningProcessDiskSerializationPreservesFiniteRemainingUses(GameTestHelper helper) {
        withInfiniteDeposits(false, () -> assertDiskSerialization(helper, REMAINING_USES, true));
        helper.succeed();
    }

    @GameTest(template = "empty16x16")
    public void miningProcessDiskSerializationPreservesInfiniteRemainingUses(GameTestHelper helper) {
        withInfiniteDeposits(true, () -> assertDiskSerialization(helper, 0L, false));
        helper.succeed();
    }

    @GameTest(template = "empty16x16")
    public void miningProcessClientPacketSerializationPreservesFiniteRemainingUses(GameTestHelper helper) {
        withInfiniteDeposits(false, () -> assertClientPacketSerialization(helper, REMAINING_USES, true));
        helper.succeed();
    }

    @GameTest(template = "empty16x16")
    public void miningProcessClientPacketSerializationPreservesInfiniteRemainingUses(GameTestHelper helper) {
        withInfiniteDeposits(true, () -> assertClientPacketSerialization(helper, 0L, false));
        helper.succeed();
    }

    private static void assertDiskSerialization(
            GameTestHelper helper, long expectedRemainingUses, boolean expectDurabilitySeeded
    ) {
        var level = helper.getLevel();
        var absoluteDepositPos = helper.absolutePos(DEPOSIT_POS);

        helper.setBlock(DEPOSIT_POS, RNSDeposits.IRON_DEPOSIT.get().defaultBlockState());
        boolean durabilitySeeded = DepositDurabilityManager.setDepositBlockDurability(level, absoluteDepositPos, REMAINING_USES);
        CodecHelper.assertValueEqual(helper, durabilitySeeded, expectDurabilitySeeded,
                "whether deposit durability seeding should succeed");

        var original = new MiningProcess(level, Set.of(), Set.of(absoluteDepositPos));
        helper.assertTrue(original.isPossible(), "Mining process should initialize for a valid deposit block");
        firstInnerProcess(original).progress = PROGRESS;

        var serialized = original.write(false);
        helper.assertTrue(serialized != null, "Disk serialization should produce a mining process tag");

        var serializedInnerProcess = serialized.getList("inner_processes", Tag.TAG_COMPOUND).getCompound(0);
        helper.assertTrue(serializedInnerProcess.contains("progress"), "Disk serialization should persist progress");
        CodecHelper.assertValueEqual(helper, serializedInnerProcess.getLong("remaining_uses"), expectedRemainingUses,
                "serialized remaining uses");
        helper.assertFalse(serializedInnerProcess.contains("catalyst_stats"),
                "Disk serialization should not persist catalyst stats");

        var restored = new MiningProcess(level, Set.of(), Set.of(absoluteDepositPos));
        restored.read(serialized, false);
        var restoredInnerProcess = firstInnerProcess(restored);

        CodecHelper.assertValueEqual(helper, restoredInnerProcess.progress, PROGRESS, "restored mining progress");
        CodecHelper.assertValueEqual(helper, restoredInnerProcess.remainingUses, expectedRemainingUses,
                "restored remaining uses");
        helper.assertFalse(restoredInnerProcess.catStats.isChancesComputed(),
                "Disk deserialization should not restore client-only catalyst stats");
    }

    private static void assertClientPacketSerialization(
            GameTestHelper helper, long expectedRemainingUses, boolean expectDurabilitySeeded
    ) {
        var level = helper.getLevel();
        var absoluteDepositPos = helper.absolutePos(DEPOSIT_POS);

        helper.setBlock(DEPOSIT_POS, RNSDeposits.IRON_DEPOSIT.get().defaultBlockState());
        boolean durabilitySeeded = DepositDurabilityManager.setDepositBlockDurability(level, absoluteDepositPos, REMAINING_USES);
        CodecHelper.assertValueEqual(helper, durabilitySeeded, expectDurabilitySeeded,
                "whether deposit durability seeding should succeed");

        var original = new MiningProcess(level, Set.of(), Set.of(absoluteDepositPos));
        helper.assertTrue(original.isPossible(), "Mining process should initialize for a valid deposit block");
        firstInnerProcess(original).progress = PROGRESS;

        var serialized = original.write(true);
        helper.assertTrue(serialized != null, "Client sync serialization should produce a mining process tag");

        var serializedInnerProcess = serialized.getList("inner_processes", Tag.TAG_COMPOUND).getCompound(0);
        helper.assertFalse(serializedInnerProcess.contains("progress"),
                "Client sync serialization should not persist progress");
        CodecHelper.assertValueEqual(helper, serializedInnerProcess.getLong("remaining_uses"), expectedRemainingUses,
                "client sync serialized remaining uses");
        helper.assertTrue(serializedInnerProcess.contains("catalyst_stats"),
                "Client sync serialization should include catalyst stats");

        var restored = new MiningProcess(level, Set.of(), Set.of(absoluteDepositPos));
        restored.read(serialized, true);
        var restoredInnerProcess = firstInnerProcess(restored);

        CodecHelper.assertValueEqual(helper, restoredInnerProcess.progress, 0, "client-synced mining progress");
        CodecHelper.assertValueEqual(helper, restoredInnerProcess.remainingUses, expectedRemainingUses,
                "client-synced remaining uses");
        helper.assertTrue(restoredInnerProcess.catStats.isChancesComputed(),
                "Client sync deserialization should restore catalyst stats");
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

    private static MiningProcess.InnerProcess firstInnerProcess(MiningProcess process) {
        return process.innerProcesses.stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("Mining process did not contain any inner processes"));
    }
}
