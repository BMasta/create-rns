package com.bmaster.createrns.serialization;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.content.deposit.claiming.IDepositBlockClaimer.ClaimedDepositBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.HashSet;
import java.util.Set;

@GameTestHolder(CreateRNS.ID)
@PrefixGameTestTemplate(false)
public class ClaimedDepositBlocksGameTest {
    @GameTest(template = "empty16x16")
    public void claimedDepositBlocksCopiesPositions(GameTestHelper helper) {
        var firstPos = new BlockPos(1, 65, 1);
        var secondPos = new BlockPos(2, 65, 2);
        var source = new HashSet<>(Set.of(firstPos));

        var claim = new ClaimedDepositBlocks(source, false);
        source.add(secondPos);

        helper.assertValueEqual(claim.positions(), Set.of(firstPos), "immutable claimed positions");
        helper.succeed();
    }

    @GameTest(template = "empty16x16")
    public void claimedDepositBlocksEqualityIncludesClaimMode(GameTestHelper helper) {
        var positions = Set.of(new BlockPos(1, 65, 1), new BlockPos(2, 65, 2));
        var sameLocalClaim = new ClaimedDepositBlocks(new HashSet<>(positions), false);

        helper.assertValueEqual(new ClaimedDepositBlocks(positions, false), sameLocalClaim,
                "claims with the same positions and mode");
        helper.assertFalse(new ClaimedDepositBlocks(positions, true).equals(sameLocalClaim),
                "Cross-sublevel mode should participate in claim equality");
        helper.succeed();
    }
}
