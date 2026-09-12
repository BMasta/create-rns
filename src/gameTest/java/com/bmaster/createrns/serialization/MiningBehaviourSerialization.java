package com.bmaster.createrns.serialization;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.RNSDisplaySources;
import com.bmaster.createrns.content.deposit.mining.MiningProcess;
import com.bmaster.createrns.content.deposit.mining.MiningStats;
import com.bmaster.createrns.util.CodecHelper;
import com.bmaster.createrns.util.MinerSetup;
import com.bmaster.createrns.util.MinerSetupBuilder;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.api.behaviour.display.DisplaySource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
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
    private static final long SYNCHRONIZED_REMAINING_RESOURCES = 12_345;
    private static final long RESTORED_UPTIME = 1_234;
    private static final long RESTORED_ELAPSED_TICKS = 100;
    private static final long RESTORED_MINED_RESOURCES = 56;
    private static final long RESTORED_CONSUMED_RESOURCES = 34;
    private static final long RESTORED_REMAINING_RESOURCES = 78;

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
    public void miningBehaviourClientPacketRetainsProcessAndSynchronizesStatsForUnchangedClaim(
            GameTestHelper helper
    ) {
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

            var depositBlock = process.innerProcesses.iterator().next().recipe.getDepositBlock();
            var remainingResources = clientPacket.getCompound("stats").getCompound("remaining_resources");
            remainingResources.putLong(BuiltInRegistries.BLOCK.getKey(depositBlock).toString(),
                    SYNCHRONIZED_REMAINING_RESOURCES);
            behavior.stats.remainingResources.put(depositBlock, -1);

            behavior.read(clientPacket, true);
            helper.assertTrue(miner.process() == process,
                    "Client packet claim deserialization should remain deferred until the next behavior tick");
            CodecHelper.assertValueEqual(helper, behavior.stats.remainingResources.getLong(depositBlock),
                    SYNCHRONIZED_REMAINING_RESOURCES, "remaining resources synchronized with miner statistics");
        });

        helper.runAtTickTime(3, () -> {
            var retainedProcess = miner.process();
            helper.assertTrue(retainedProcess == originalProcess.get(),
                    "Client packet with an unchanged claim should retain the mining process");
            retainedProcess.innerProcesses.forEach(innerProcess -> {
                helper.assertTrue(innerProcess.progress >= SAVED_PROGRESS,
                        "Client packet should not synchronize progress");
                CodecHelper.assertValueEqual(helper, miner.behavior().stats.remainingResources.getLong(
                                innerProcess.recipe.getDepositBlock()), SYNCHRONIZED_REMAINING_RESOURCES,
                        "remaining resources retained without reconstructing the process");
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
                    innerProcess.progress >= SAVED_PROGRESS,
                    "Process reconstruction should preserve local mining progress"));
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
                    innerProcess.progress >= SAVED_PROGRESS,
                    "Process reconstruction should preserve local mining progress"));
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

    @GameTest(template = "empty16x16", timeoutTicks = 15)
    public void contraptionMiningStatisticsSerializeAndDisplaySourceIsAssociated(GameTestHelper helper) {
        var miner = createMiner(helper);
        var sources = DisplaySource.getAll(helper.getLevel(), helper.absolutePos(miner.bearingPos()));
        helper.assertTrue(sources.contains(RNSDisplaySources.COMPACT_MINER_STATUS.get()),
                "Miner bearing should advertise compact status display support");

        miner.assemble(TEST_RPM);
        helper.runAtTickTime(2, () -> {
            var behavior = miner.behavior();
            var level = behavior.getLevel();
            CodecHelper.assertValueEqual(helper, behavior.stats.getUptimeTicks(), 0L,
                    "Assembled miner should ignore uptime during the grace period");

            var diskTag = new CompoundTag();
            behavior.write(diskTag, false);
            helper.assertTrue(diskTag.get("stats") instanceof CompoundTag,
                    "Disk data should contain miner statistics");

            var stats = diskTag.getCompound("stats");
            helper.assertTrue(stats.contains("start_time"),
                    "Assembled miner should record its statistics start time");
            stats.putLong("start_time", helper.getLevel().getGameTime()
                    - MiningStats.UPTIME_GRACE_PERIOD_TICKS - RESTORED_ELAPSED_TICKS);
            stats.putLong("uptime", RESTORED_UPTIME);
            stats.putLong("mined_resources", RESTORED_MINED_RESOURCES);
            stats.putLong("consumed_resources", RESTORED_CONSUMED_RESOURCES);
            var process = behavior.getProcess();
            helper.assertTrue(process != null, "Assembled miner should have a mining process");
            var depositBlock = process.innerProcesses.iterator().next().recipe.getDepositBlock();
            var remainingResources = new CompoundTag();
            remainingResources.putLong(BuiltInRegistries.BLOCK.getKey(depositBlock).toString(),
                    RESTORED_REMAINING_RESOURCES);
            stats.put("remaining_resources", remainingResources);
            behavior.read(diskTag, false);

            CodecHelper.assertValueEqual(helper, behavior.stats.getElapsedTicks(level), RESTORED_ELAPSED_TICKS,
                    "restored statistics elapsed time");
            CodecHelper.assertValueEqual(helper, behavior.stats.getUptimeTicks(), RESTORED_UPTIME, "restored uptime");
            CodecHelper.assertValueEqual(helper, behavior.stats.minedResources, RESTORED_MINED_RESOURCES,
                    "restored mined resources");
            CodecHelper.assertValueEqual(helper, behavior.stats.consumedResources, RESTORED_CONSUMED_RESOURCES,
                    "restored consumed resources");
            CodecHelper.assertValueEqual(helper, behavior.stats.remainingResources.getLong(depositBlock),
                    RESTORED_REMAINING_RESOURCES,
                    "restored remaining resources");
            helper.assertFalse(behavior.stats.updateUptimeTicks(level, false),
                    "Deserialization should not restart a completed uptime grace period");
            CodecHelper.assertValueEqual(helper, behavior.stats.getUptimeTicks(), RESTORED_UPTIME,
                    "Uptime should remain unchanged while the restored miner is not mining");

            miner.bearing().disassemble();
            CodecHelper.assertValueEqual(helper, behavior.stats.getElapsedTicks(level), 0L,
                    "statistics elapsed time after disassembly");
            CodecHelper.assertValueEqual(helper, behavior.stats.getUptimeTicks(), 0L, "uptime after disassembly");
            CodecHelper.assertValueEqual(helper, behavior.stats.minedResources, 0L, "mined resources after disassembly");
            CodecHelper.assertValueEqual(helper, behavior.stats.consumedResources, 0L,
                    "consumed resources after disassembly");
            helper.assertTrue(behavior.stats.remainingResources.isEmpty(),
                    "remaining resources should be empty after disassembly");

            var resetTag = new CompoundTag();
            behavior.write(resetTag, false);
            var resetStats = resetTag.getCompound("stats");
            helper.assertFalse(resetStats.contains("start_time"),
                    "Disassembled miner should not retain a statistics start time");
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
