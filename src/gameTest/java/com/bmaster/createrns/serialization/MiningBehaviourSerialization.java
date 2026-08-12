package com.bmaster.createrns.serialization;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.content.deposit.claiming.IDepositBlockClaimer.ClaimedDepositBlocks;
import com.bmaster.createrns.content.deposit.mining.MiningProcess;
import com.bmaster.createrns.util.MinerSetup;
import com.bmaster.createrns.util.MinerSetupBuilder;
import com.simibubi.create.AllBlocks;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@GameTestHolder(CreateRNS.ID)
@PrefixGameTestTemplate(false)
public class MiningBehaviourSerialization {
    private static final int TEST_RPM = 128;
    private static final int SAVED_PROGRESS = 100_000;

    @GameTest(template = "empty16x16", timeoutTicks = 15)
    public void claimingBehaviourDiskRetainsInitializedClaim(GameTestHelper helper) {
        var miner = createMiner(helper);
        miner.assemble(TEST_RPM);

        helper.runAtTickTime(2, () -> {
            var behavior = miner.behavior();
            var originalClaim = behavior.getClaimedDepositBlocks();
            helper.assertTrue(originalClaim != null, "Assembled miner should have an exclusive claim");
            var diskTag = new CompoundTag();
            behavior.write(diskTag, helper.getLevel().registryAccess(), false);

            helper.assertTrue(diskTag.contains("claimer"), "Disk data should contain the exclusive claim");

            behavior.read(diskTag, helper.getLevel().registryAccess(), false);
            helper.assertTrue(behavior.getClaimedDepositBlocks() == originalClaim,
                    "An update to an initialized miner should retain its equal claim instance");
            helper.succeed();
        });
    }

    @GameTest(template = "empty16x16", timeoutTicks = 15)
    public void claimingBehaviourDiskDefersNewOwnerValidation(GameTestHelper helper) {
        var miner = createMiner(helper);
        var restoredClaim = new AtomicReference<ClaimedDepositBlocks>();
        miner.assemble(TEST_RPM);

        helper.runAtTickTime(2, () -> {
            var behavior = miner.behavior();
            var diskTag = new CompoundTag();
            behavior.write(diskTag, helper.getLevel().registryAccess(), false);

            behavior.setClaimedDepositBlocks(null);
            behavior.read(diskTag, helper.getLevel().registryAccess(), false);
            restoredClaim.set(behavior.getClaimedDepositBlocks());
            behavior.read(diskTag, helper.getLevel().registryAccess(), false);

            helper.assertTrue(restoredClaim.get() != null, "Disk data should restore a claim pending validation");
            helper.assertTrue(behavior.getClaimedDepositBlocks() == restoredClaim.get(),
                    "A repeated read before validation should retain the equal claim instance");
            helper.assertTrue(miner.process() == null,
                    "Process reconstruction should wait for restored claim validation");
        });

        helper.runAtTickTime(3, () -> {
            helper.assertTrue(miner.behavior().getClaimedDepositBlocks() == restoredClaim.get(),
                    "A valid restored claim should not be rebuilt");
            helper.assertTrue(miner.process() != null, "Process should reconstruct after claim validation");
            helper.succeed();
        });
    }

    @GameTest(template = "empty16x16", timeoutTicks = 15)
    public void claimingBehaviourClientPacketSynchronizesClaim(GameTestHelper helper) {
        var miner = createMiner(helper);
        miner.assemble(TEST_RPM);

        helper.runAtTickTime(2, () -> {
            var behavior = miner.behavior();
            var originalClaim = behavior.getClaimedDepositBlocks();
            helper.assertTrue(originalClaim != null, "Assembled miner should have a claim");
            var expectedClaim = originalClaim;

            var clientPacket = new CompoundTag();
            behavior.write(clientPacket, helper.getLevel().registryAccess(), true);
            helper.assertTrue(clientPacket.contains("claimer"), "Client packet should contain the claim");

            behavior.setClaimedDepositBlocks(new ClaimedDepositBlocks(Set.of(), false));
            clientPacket.remove("process");
            behavior.read(clientPacket, helper.getLevel().registryAccess(), true);

            helper.assertValueEqual(behavior.getClaimedDepositBlocks(), expectedClaim,
                    "claim deserialized from the client packet");
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
            behavior.write(diskTag, helper.getLevel().registryAccess(), false);
            helper.assertTrue(diskTag.contains("process"), "Disk data should contain mining process state");

            behavior.setClaimedDepositBlocks(null);
            behavior.read(diskTag, helper.getLevel().registryAccess(), false);
            helper.assertTrue(behavior.getClaimedDepositBlocks() != null,
                    "Disk read should restore the claim pending validation");
            helper.assertTrue(miner.process() == null,
                    "Process should not reconstruct before restored claim validation");
        });

        helper.runAtTickTime(3, () -> {
            var restoredProcess = miner.process();
            helper.assertTrue(restoredProcess != null, "Process should reconstruct after claim validation");
            helper.assertFalse(restoredProcess == originalProcess.get(),
                    "Disk read should invalidate the previous process");
            restoredProcess.innerProcesses.forEach(innerProcess -> helper.assertTrue(
                    innerProcess.progress >= SAVED_PROGRESS, "Process should restore saved mining progress"));
            assertProcessMatchesClaim(helper, miner, restoredProcess);
            helper.succeed();
        });
    }

    @GameTest(template = "empty16x16", timeoutTicks = 15)
    public void miningBehaviourClientPacketReplacesProcess(GameTestHelper helper) {
        var miner = createMiner(helper);
        miner.assemble(TEST_RPM);

        helper.runAtTickTime(2, () -> {
            var behavior = miner.behavior();
            var originalProcess = miner.process();
            helper.assertTrue(originalProcess != null, "Assembled miner should have a mining process");
            originalProcess.innerProcesses.forEach(innerProcess -> innerProcess.progress = SAVED_PROGRESS);

            var clientPacket = new CompoundTag();
            behavior.write(clientPacket, helper.getLevel().registryAccess(), true);
            helper.assertTrue(clientPacket.contains("process"), "Client packet should contain mining process state");

            behavior.read(clientPacket, helper.getLevel().registryAccess(), true);
            var restoredProcess = miner.process();
            helper.assertTrue(restoredProcess != null, "Client process should reconstruct on first access");
            helper.assertFalse(restoredProcess == originalProcess,
                    "Client packet should invalidate the previous process");
            restoredProcess.innerProcesses.forEach(innerProcess -> helper.assertValueEqual(
                    innerProcess.progress, 0, "client packet should not synchronize progress"));
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
        helper.assertValueEqual(processTargets, claimedBlocks.positions(), "process targets");
    }
}
