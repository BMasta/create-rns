package com.bmaster.createrns.serialization;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.content.deposit.mining.MiningProcess;
import com.bmaster.createrns.util.MinerSetup;
import com.bmaster.createrns.util.MinerSetupBuilder;
import com.simibubi.create.AllBlocks;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Set;
import java.util.stream.Collectors;

@GameTestHolder(CreateRNS.ID)
@PrefixGameTestTemplate(false)
public class MiningBehaviourSerialization {
    private static final int TEST_RPM = 128;
    private static final int SAVED_PROGRESS = 77;

    @GameTest(template = "empty16x16", timeoutTicks = 15)
    public void hybridBehaviourDiskPersistsClaimButNotSelection(GameTestHelper helper) {
        var miner = createMiner(helper);
        miner.assemble(TEST_RPM);

        helper.runAtTickTime(2, () -> {
            var behavior = miner.behavior();
            var claimedBlocks = behavior.getClaimedDepositBlocks();
            helper.assertTrue(claimedBlocks != null, "Assembled miner should have an exclusive claim");
            var expectedClaim = Set.copyOf(claimedBlocks);
            var diskTag = new CompoundTag();
            behavior.write(diskTag, helper.getLevel().registryAccess(), false);

            helper.assertTrue(diskTag.contains("claimer"), "Disk data should contain the exclusive claim");
            helper.assertFalse(diskTag.contains("selection"), "Disk data should not persist the operating selection");

            behavior.setClaimedDepositBlocks(Set.of());
            behavior.read(diskTag, helper.getLevel().registryAccess(), false);

            helper.assertValueEqual(behavior.getClaimedDepositBlocks(), expectedClaim,
                    "claim deserialized from disk data");
            helper.assertTrue(behavior.getOperatingSelection() == null,
                    "Disk read should leave operating selection uninitialized until runtime reconstruction");
            helper.succeed();
        });
    }

    @GameTest(template = "empty16x16", timeoutTicks = 15)
    public void hybridBehaviourClientPacketSynchronizesSelectionIndependentlyOfClaim(GameTestHelper helper) {
        var miner = createMiner(helper);
        miner.assemble(TEST_RPM);

        helper.runAtTickTime(2, () -> {
            var behavior = miner.behavior();
            var originalSelection = behavior.getOperatingSelection();
            helper.assertTrue(originalSelection != null, "Assembled miner should have an operating selection");

            var clientPacket = new CompoundTag();
            behavior.write(clientPacket, helper.getLevel().registryAccess(), true);
            helper.assertTrue(clientPacket.contains("selection"),
                    "Client packet should contain the operating selection");

            var emptyClaim = new CompoundTag();
            emptyClaim.put("claimed_blocks", new ListTag());
            clientPacket.put("claimer", emptyClaim);
            clientPacket.remove("process");
            behavior.read(clientPacket, helper.getLevel().registryAccess(), true);

            helper.assertValueEqual(behavior.getClaimedDepositBlocks(), Set.of(),
                    "claim deserialized independently from the client packet");
            var restoredSelection = behavior.getOperatingSelection();
            helper.assertTrue(restoredSelection != null, "Client packet should restore the operating selection");
            helper.assertValueEqual(restoredSelection.crossSublevel, originalSelection.crossSublevel, "operating mode");
            helper.assertValueEqual(restoredSelection.sublevel, originalSelection.sublevel, "operating sublevel");
            helper.assertValueEqual(restoredSelection.positions, originalSelection.positions,
                    "active deposit positions");
            helper.succeed();
        });
    }

    @GameTest(template = "empty16x16", timeoutTicks = 15)
    public void miningBehaviourDiskLazilyRestoresProcessProgress(GameTestHelper helper) {
        var miner = createMiner(helper);
        miner.assemble(TEST_RPM);

        helper.runAtTickTime(2, () -> {
            var behavior = miner.behavior();
            var originalProcess = miner.process();
            helper.assertTrue(originalProcess != null, "Assembled miner should have a mining process");
            originalProcess.innerProcesses.forEach(innerProcess -> innerProcess.progress = SAVED_PROGRESS);

            var diskTag = new CompoundTag();
            behavior.write(diskTag, helper.getLevel().registryAccess(), false);
            helper.assertTrue(diskTag.contains("process"), "Disk data should contain mining process state");

            behavior.read(diskTag, helper.getLevel().registryAccess(), false);
            helper.assertTrue(behavior.getOperatingSelection() == null,
                    "Disk read should defer local selection and process reconstruction");

            var restoredProcess = miner.process();
            helper.assertTrue(restoredProcess != null, "Process should reconstruct on first access");
            helper.assertFalse(restoredProcess == originalProcess, "Disk read should invalidate the previous process");
            restoredProcess.innerProcesses.forEach(innerProcess ->
                    helper.assertValueEqual(innerProcess.progress, SAVED_PROGRESS, "restored mining progress"));
            assertProcessMatchesSelection(helper, miner, restoredProcess);
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
            restoredProcess.innerProcesses.forEach(innerProcess ->
                    helper.assertValueEqual(innerProcess.progress, 0, "client packet should not synchronize progress"));
            assertProcessMatchesSelection(helper, miner, restoredProcess);
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

    private static void assertProcessMatchesSelection(
            GameTestHelper helper, MinerSetup miner, MiningProcess process
    ) {
        var selection = miner.behavior().getOperatingSelection();
        helper.assertTrue(selection != null, "Process initialization should reconstruct the operating selection");
        var processTargets = process.innerProcesses.stream()
                .flatMap(innerProcess -> innerProcess.depositPositions.stream())
                .collect(Collectors.toUnmodifiableSet());
        helper.assertValueEqual(processTargets, selection.positions, "process targets");
    }
}
