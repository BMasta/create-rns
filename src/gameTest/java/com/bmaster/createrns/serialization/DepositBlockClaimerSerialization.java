package com.bmaster.createrns.serialization;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.content.deposit.claiming.IDepositBlockClaimer;
import com.bmaster.createrns.util.CodecHelper;
import com.mojang.math.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashSet;
import java.util.Set;

@GameTestHolder(CreateRNS.ID)
@PrefixGameTestTemplate(false)
public class DepositBlockClaimerSerialization {
    @GameTest(template = "empty16x16")
    public void depositBlockClaimerRoundTripsClaimedBlocks(GameTestHelper helper) {
        var level = helper.getLevel();

        var original = new FakeClaimer(level);
        original.setClaimedDepositBlocks(Set.of(new BlockPos(1, 65, 1), new BlockPos(2, 65, 2)));

        var restored = new FakeClaimer(level);
        restored.deserializeDepositBlockClaimer(original.serializeDepositBlockClaimer());

        var claimedBlocks = original.getClaimedDepositBlocks();
        helper.assertTrue(claimedBlocks != null, "Blocks should be claimed");
        helper.assertTrue(original.getClaimedDepositBlocks().equals(restored.getClaimedDepositBlocks()),
                "Deserialized claimer should preserve claimed blocks");
        helper.succeed();
    }

    @GameTest(template = "empty16x16")
    public void depositBlockClaimerCanClearClaimedBlocks(GameTestHelper helper) {
        var level = helper.getLevel();

        var claimer = new FakeClaimer(level);
        claimer.setClaimedDepositBlocks(Set.of(new BlockPos(3, 65, 3)));
        claimer.deserializeDepositBlockClaimer(new net.minecraft.nbt.CompoundTag());

        helper.assertTrue(claimer.getClaimedDepositBlocks() == null,
                "Deserializing a claimer without claimed_blocks should clear the current claim");
        helper.succeed();
    }

    /// Used to test serialization methods of IDepositBlockClaimer
    @ParametersAreNonnullByDefault
    @MethodsReturnNonnullByDefault
    private static class FakeClaimer implements IDepositBlockClaimer {
        private static final ClaimerType CLAIMER_TYPE = new ClaimerType("create_rns:test_claimer");

        private final ServerLevel level;
        private @Nullable Set<BlockPos> claimedBlocks;

        private FakeClaimer(ServerLevel level) {
            this.level = level;
        }

        @Override
        public ClaimingMode getClaimingMode() {
            return ClaimingMode.EXCLUSIVE;
        }

        @Override
        public ClaimerType getClaimerType() {
            return CLAIMER_TYPE;
        }

        @Override
        public @Nullable Level getLevel() {
            return level;
        }

        @Override
        public @Nullable ClaimingArea getClaimingArea() {
            return null;
        }

        @Override
        public @Nullable BlockPos getAnchor() {
            return null;
        }

        @Override
        public Direction getClaimingDirection() {
            return Direction.DOWN;
        }

        @Override
        public @Nullable Set<BlockPos> getClaimedDepositBlocks() {
            return claimedBlocks;
        }

        @Override
        public void setClaimedDepositBlocks(@Nullable Set<BlockPos> claimedBlocks) {
            this.claimedBlocks = claimedBlocks == null ? null : new HashSet<>(claimedBlocks);
        }

        @Override
        public void claimDepositBlocks() {
        }
    }
}
