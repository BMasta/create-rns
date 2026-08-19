package com.bmaster.createrns.serialization;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.content.deposit.mining.MiningProcess;
import com.bmaster.createrns.util.CodecHelper;
import com.bmaster.createrns.util.MinerSetup;
import com.bmaster.createrns.util.MinerSetupBuilder;
import com.simibubi.create.AllBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@GameTestHolder(CreateRNS.ID)
@PrefixGameTestTemplate(false)
public class MiningBehaviourSerialization {
    private static final String CLAIMED_BLOCKS_VERSION_KEY = "claimed_blocks_version";
    private static final int TEST_RPM = 128;
    private static final int SAVED_PROGRESS = 100_000;
    private static final int NEGATIVE_CLAIM_VERSION = -1;
    private static final long SYNCHRONIZED_REMAINING_USES = 12_345;

    @GameTest(template = "empty16x16", timeoutTicks = 15)
    public void claimingBehaviourDiskReplacesInitializedClaim(GameTestHelper helper) {
        var miner = createMiner(helper);
        var originalClaim = new AtomicReference<Set<BlockPos>>();
        miner.assemble(TEST_RPM);

        helper.runAtTickTime(2, () -> {
            var behavior = miner.behavior();
            var claim = behavior.getClaimedDepositBlocks();
            helper.assertTrue(claim != null, "Assembled miner should have an exclusive claim");
            originalClaim.set(claim);
            var diskTag = new CompoundTag();
            behavior.write(diskTag, false);

            helper.assertTrue(diskTag.contains("claimer"), "Disk data should contain the exclusive claim");
            helper.assertFalse(diskTag.getCompound("claimer").contains(CLAIMED_BLOCKS_VERSION_KEY),
                    "Disk data should not contain the claimed-blocks version");

            behavior.read(diskTag, false);
            helper.assertTrue(behavior.getClaimedDepositBlocks() == claim,
                    "Disk claim deserialization should remain deferred until the next behavior tick");
        });

        helper.runAtTickTime(3, () -> {
            var restoredClaim = miner.behavior().getClaimedDepositBlocks();
            CodecHelper.assertValueEqual(helper, restoredClaim, originalClaim.get(), "restored disk claim");
            helper.assertFalse(restoredClaim == originalClaim.get(),
                    "Disk data without a claimed-blocks version should replace the claim instance");
            helper.succeed();
        });
    }

    @GameTest(template = "empty16x16", timeoutTicks = 15)
    public void claimingBehaviourClientPacketSynchronizesDifferentClaimForSameVersion(GameTestHelper helper) {
        var miner = createMiner(helper);
        var expectedClaim = new AtomicReference<Set<BlockPos>>();
        miner.assemble(TEST_RPM);

        helper.runAtTickTime(2, () -> {
            var behavior = miner.behavior();
            var originalClaim = behavior.getClaimedDepositBlocks();
            helper.assertTrue(originalClaim != null, "Assembled miner should have a claim");
            expectedClaim.set(Set.copyOf(originalClaim));

            var clientPacket = new CompoundTag();
            behavior.write(clientPacket, true);
            helper.assertTrue(clientPacket.contains("claimer"), "Client packet should contain the claim");
            var claimerTag = clientPacket.getCompound("claimer");
            helper.assertTrue(claimerTag.contains(CLAIMED_BLOCKS_VERSION_KEY),
                    "Client packet should contain the claimed-blocks version");
            int version = claimerTag.getInt(CLAIMED_BLOCKS_VERSION_KEY);

            behavior.setClaimedDepositBlocks(Set.of());
            claimerTag.putInt(CLAIMED_BLOCKS_VERSION_KEY, version + 1);
            clientPacket.remove("process");
            behavior.read(clientPacket, true);

            helper.assertTrue(behavior.getClaimedDepositBlocks().isEmpty(),
                    "Client claim deserialization should remain deferred until the next behavior tick");
        });

        helper.runAtTickTime(3, () -> {
            CodecHelper.assertValueEqual(helper, miner.behavior().getClaimedDepositBlocks(), expectedClaim.get(),
                    "claim deserialized from the client packet on the next behavior tick");
            helper.succeed();
        });
    }

    @GameTest(template = "empty16x16", timeoutTicks = 15)
    public void miningBehaviourDiskLazilyRestoresProcessProgress(GameTestHelper helper) {
        var miner = createMiner(helper);
        var originalProcess = new AtomicReference<MiningProcess>();
        miner.assemble(TEST_RPM);

        helper.runAtTickTime(2, () -> {
            var behavior = miner.behavior();
            var process = miner.process();
            helper.assertTrue(process != null, "Assembled miner should have a mining process");
            originalProcess.set(process);
            process.innerProcesses.forEach(innerProcess -> innerProcess.progress = SAVED_PROGRESS);

            var diskTag = new CompoundTag();
            behavior.write(diskTag, false);
            helper.assertTrue(diskTag.contains("process"), "Disk data should contain mining process state");

            behavior.setClaimedDepositBlocks(null);
            behavior.read(diskTag, false);
            helper.assertTrue(behavior.getClaimedDepositBlocks() == null,
                    "Disk claim deserialization should remain deferred until the next behavior tick");
            helper.assertTrue(miner.process() == null,
                    "Process should not reconstruct before the restored claim is read");
        });

        helper.runAtTickTime(3, () -> {
            var restoredProcess = miner.process();
            helper.assertTrue(restoredProcess != null, "Process should reconstruct after the claim is restored");
            helper.assertFalse(restoredProcess == originalProcess.get(),
                    "Disk read should invalidate the previous process");
            restoredProcess.innerProcesses.forEach(innerProcess -> helper.assertTrue(
                    innerProcess.progress >= SAVED_PROGRESS, "Process should restore saved mining progress"));
            assertProcessMatchesClaim(helper, miner, restoredProcess);
            helper.succeed();
        });
    }

    @GameTest(template = "empty16x16", timeoutTicks = 15)
    public void miningBehaviourClientPacketRetainsProcessForUnchangedClaim(GameTestHelper helper) {
        var miner = createMiner(helper);
        var originalProcess = new AtomicReference<MiningProcess>();
        miner.assemble(TEST_RPM);

        helper.runAtTickTime(2, () -> {
            var behavior = miner.behavior();
            var process = miner.process();
            helper.assertTrue(process != null, "Assembled miner should have a mining process");
            originalProcess.set(process);
            process.innerProcesses.forEach(innerProcess -> innerProcess.progress = SAVED_PROGRESS);

            var clientPacket = new CompoundTag();
            behavior.write(clientPacket, true);
            helper.assertTrue(clientPacket.contains("process"), "Client packet should contain mining process state");
            helper.assertTrue(clientPacket.getCompound("claimer").contains(CLAIMED_BLOCKS_VERSION_KEY),
                    "Client packet should contain the claimed-blocks version");
            clientPacket.getCompound("process").getList("inner_processes", Tag.TAG_COMPOUND)
                    .forEach(tag -> ((CompoundTag) tag).putLong("remaining_uses", SYNCHRONIZED_REMAINING_USES));
            process.innerProcesses.forEach(innerProcess -> innerProcess.remainingUses = -1);

            behavior.read(clientPacket, true);
            helper.assertTrue(miner.process() == process,
                    "Client packet claim deserialization should remain deferred until the next behavior tick");
            process.innerProcesses.forEach(innerProcess -> CodecHelper.assertValueEqual(helper,
                    innerProcess.remainingUses, -1L, "process state should remain deferred with the claim"));
        });

        helper.runAtTickTime(3, () -> {
            var retainedProcess = miner.process();
            helper.assertTrue(retainedProcess == originalProcess.get(),
                    "Client packet with an unchanged claim should retain the mining process");
            retainedProcess.innerProcesses.forEach(innerProcess -> {
                helper.assertTrue(innerProcess.progress >= SAVED_PROGRESS,
                        "Client packet should not synchronize progress");
                CodecHelper.assertValueEqual(helper, innerProcess.remainingUses, SYNCHRONIZED_REMAINING_USES,
                        "remaining uses synchronized without reconstructing the process");
            });
            assertProcessMatchesClaim(helper, miner, retainedProcess);
            helper.succeed();
        });
    }

    @GameTest(template = "empty16x16", timeoutTicks = 15)
    public void miningBehaviourClientPacketReplacesProcessForChangedVersion(GameTestHelper helper) {
        var miner = createMiner(helper);
        var originalProcess = new AtomicReference<MiningProcess>();
        var replacementProcess = new AtomicReference<MiningProcess>();
        var changedVersionPacket = new AtomicReference<CompoundTag>();
        miner.assemble(TEST_RPM);

        helper.runAtTickTime(2, () -> {
            var behavior = miner.behavior();
            var process = miner.process();
            helper.assertTrue(process != null, "Assembled miner should have a mining process");
            originalProcess.set(process);
            process.innerProcesses.forEach(innerProcess -> innerProcess.progress = SAVED_PROGRESS);

            var clientPacket = new CompoundTag();
            behavior.write(clientPacket, true);
            var claimerTag = clientPacket.getCompound("claimer");
            helper.assertTrue(claimerTag.contains(CLAIMED_BLOCKS_VERSION_KEY),
                    "Client packet should contain the claimed-blocks version");
            claimerTag.putInt(CLAIMED_BLOCKS_VERSION_KEY, NEGATIVE_CLAIM_VERSION);
            changedVersionPacket.set(clientPacket);

            behavior.read(clientPacket, true);
            helper.assertTrue(miner.process() == process,
                    "Client packet claim deserialization should remain deferred until the next behavior tick");
        });

        helper.runAtTickTime(3, () -> {
            var restoredProcess = miner.process();
            helper.assertTrue(restoredProcess != null, "Client process should reconstruct after a version change");
            helper.assertFalse(restoredProcess == originalProcess.get(),
                    "Changed claimed-blocks version should invalidate the previous process");
            restoredProcess.innerProcesses.forEach(innerProcess -> helper.assertTrue(
                    innerProcess.progress < SAVED_PROGRESS, "client packet should not synchronize progress"));
            assertProcessMatchesClaim(helper, miner, restoredProcess);
            replacementProcess.set(restoredProcess);

            miner.behavior().read(changedVersionPacket.get(), true);
            helper.assertTrue(miner.process() == restoredProcess,
                    "Repeated client packet deserialization should remain deferred until the next behavior tick");
        });

        helper.runAtTickTime(4, () -> {
            helper.assertTrue(miner.process() == replacementProcess.get(),
                    "A repeated negative claimed-blocks version should retain the replacement process");
            helper.succeed();
        });
    }

    @GameTest(template = "empty16x16", timeoutTicks = 15)
    public void claimingBehaviourClientPacketIncludesVersionWithoutClaim(GameTestHelper helper) {
        var miner = createMiner(helper);
        miner.assemble(TEST_RPM);

        helper.runAtTickTime(2, () -> {
            var behavior = miner.behavior();
            behavior.setClaimedDepositBlocks(null);

            var clientPacket = new CompoundTag();
            behavior.write(clientPacket, true);
            var claimerTag = clientPacket.getCompound("claimer");
            helper.assertFalse(claimerTag.contains("claimed_blocks"),
                    "Client packet should represent the absent claim");
            helper.assertTrue(claimerTag.contains(CLAIMED_BLOCKS_VERSION_KEY),
                    "Client packet without a claim should still contain the claimed-blocks version");
            helper.succeed();
        });
    }

    @GameTest(template = "empty16x16", timeoutTicks = 15)
    public void miningBehaviourLegacyClientPacketReplacesProcess(GameTestHelper helper) {
        var miner = createMiner(helper);
        var originalProcess = new AtomicReference<MiningProcess>();
        miner.assemble(TEST_RPM);

        helper.runAtTickTime(2, () -> {
            var behavior = miner.behavior();
            var process = miner.process();
            helper.assertTrue(process != null, "Assembled miner should have a mining process");
            originalProcess.set(process);
            process.innerProcesses.forEach(innerProcess -> innerProcess.progress = SAVED_PROGRESS);

            var legacyClientPacket = new CompoundTag();
            behavior.write(legacyClientPacket, true);
            helper.assertTrue(legacyClientPacket.getCompound("claimer").contains(CLAIMED_BLOCKS_VERSION_KEY),
                    "Current client packets should contain the claimed-blocks version");
            legacyClientPacket.getCompound("claimer").remove(CLAIMED_BLOCKS_VERSION_KEY);

            behavior.read(legacyClientPacket, true);
            helper.assertTrue(miner.process() == process,
                    "Client packet claim deserialization should remain deferred until the next behavior tick");
        });

        helper.runAtTickTime(3, () -> {
            var restoredProcess = miner.process();
            helper.assertTrue(restoredProcess != null, "Legacy client data should reconstruct the process");
            helper.assertFalse(restoredProcess == originalProcess.get(),
                    "A missing claimed-blocks version should invalidate the previous process");
            restoredProcess.innerProcesses.forEach(innerProcess -> helper.assertTrue(
                    innerProcess.progress < SAVED_PROGRESS, "client packet should not synchronize progress"));
            assertProcessMatchesClaim(helper, miner, restoredProcess);
            helper.succeed();
        });
    }

    @GameTest(template = "empty16x16", timeoutTicks = 15)
    public void miningBehaviourDiskDataReplacesProcessForEqualClaim(GameTestHelper helper) {
        var miner = createMiner(helper);
        var originalProcess = new AtomicReference<MiningProcess>();
        miner.assemble(TEST_RPM);

        helper.runAtTickTime(2, () -> {
            var behavior = miner.behavior();
            var process = miner.process();
            helper.assertTrue(process != null, "Assembled miner should have a mining process");
            originalProcess.set(process);
            process.innerProcesses.forEach(innerProcess -> innerProcess.progress = SAVED_PROGRESS);

            var diskTag = new CompoundTag();
            behavior.write(diskTag, false);
            helper.assertFalse(diskTag.getCompound("claimer").contains(CLAIMED_BLOCKS_VERSION_KEY),
                    "Disk data should not contain the claimed-blocks version");

            behavior.read(diskTag, false);
            helper.assertTrue(miner.process() == process,
                    "Disk claim deserialization should remain deferred until the next behavior tick");
        });

        helper.runAtTickTime(3, () -> {
            var restoredProcess = miner.process();
            helper.assertTrue(restoredProcess != null, "Disk data should reconstruct the process");
            helper.assertFalse(restoredProcess == originalProcess.get(),
                    "Disk data without a claimed-blocks version should invalidate the previous process");
            restoredProcess.innerProcesses.forEach(innerProcess -> helper.assertTrue(
                    innerProcess.progress >= SAVED_PROGRESS, "Process should restore saved mining progress"));
            assertProcessMatchesClaim(helper, miner, restoredProcess);
            helper.succeed();
        });
    }

    private static MinerSetup createMiner(GameTestHelper helper) {
        return MinerSetupBuilder.create(helper)
                .bearing(5, 5, 5)
                .part(AllBlocks.ANDESITE_CASING.getDefaultState(), 5, 4, 5)
                .head(5, 3, 5)
                .deposit(5, 2, 5)
                .place();
    }

    private static void assertProcessMatchesClaim(
            GameTestHelper helper, MinerSetup miner, MiningProcess process
    ) {
        var claimedBlocks = miner.behavior().getClaimedDepositBlocks();
        helper.assertTrue(claimedBlocks != null, "Process initialization should use the restored claim");
        var processTargets = process.innerProcesses.stream()
                .flatMap(innerProcess -> innerProcess.depositPositions.stream())
                .collect(Collectors.toUnmodifiableSet());
        CodecHelper.assertValueEqual(helper, processTargets, claimedBlocks, "process targets");
    }
}
