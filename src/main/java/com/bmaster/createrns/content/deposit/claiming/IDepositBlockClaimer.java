package com.bmaster.createrns.content.deposit.claiming;

import com.bmaster.createrns.RNSTags;
import com.bmaster.createrns.util.Utils;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public interface IDepositBlockClaimer {
    ClaimingMode getClaimingMode();

    ClaimerType getClaimerType();

    @Nullable Level getLevel();

    @Nullable IDepositBlockClaimer.ClaimingArea getClaimingArea();

    @Nullable BlockPos getAnchor();

    Direction getClaimingDirection();

    @Nullable Set<BlockPos> getClaimedDepositBlocks();

    void setClaimedDepositBlocks(@Nullable Set<BlockPos> claimedBlocks);

    void claimDepositBlocks();

    default @Nullable BoundingBox getClaimingBoundingBox() {
        var spec = getClaimingArea();
        if (spec == null) return null;
        var anchor = getAnchor();
        if (anchor == null) return null;
        var dir = getClaimingDirection();
        Vec3i pos = new Vec3i(anchor.getX(), anchor.getY(), anchor.getZ());

        var minOffset = dir.getNormal().multiply(
                dir.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1 : spec.length);
        var maxOffset = dir.getNormal().multiply(
                dir.getAxisDirection() == Direction.AxisDirection.NEGATIVE ? 1 : spec.length);

        var minRadiusDelta = Utils.normalVecFlip(dir, false).multiply(spec.radius);
        var maxRadiusDelta = Utils.normalVecFlip(dir, true).multiply(spec.radius);

        var minPos = pos.offset(minOffset).offset(minRadiusDelta);
        var maxPos = pos.offset(maxOffset).offset(maxRadiusDelta);

        return new BoundingBox(minPos.getX(), minPos.getY(), minPos.getZ(), maxPos.getX(), maxPos.getY(), maxPos.getZ());
    }

    default Set<BlockPos> getConfinedDepositVein() {
        var level = getLevel();
        if (level == null) return Set.of();
        var spec = getClaimingArea();
        if (spec == null) return Set.of();
        var anchor = getAnchor();
        if (anchor == null) return Set.of();
        var ma = getClaimingBoundingBox();
        if (ma == null) return Set.of();
        var dir = getClaimingDirection();

        Queue<BlockPos> q = new ArrayDeque<>();
        LongOpenHashSet visited = new LongOpenHashSet(ma.getXSpan() * ma.getYSpan() * ma.getZSpan());

        q.offer(anchor.relative(dir));
        while (!q.isEmpty()) {
            var bp = q.poll();
            if (visited.contains(bp.asLong()) || !ma.isInside(bp) || !level.getBlockState(bp).is(RNSTags.Block.DEPOSIT_BLOCKS)) {
                continue;
            }
            visited.add(bp.asLong());

            Direction.stream().forEach(d -> q.add(bp.relative(d)));
        }
        return visited.longStream().mapToObj(BlockPos::of).collect(Collectors.toSet());
    }

    default Set<BlockPos> getClaimableDepositVein(Level level) {
        var vein = getConfinedDepositVein();
        if (getClaimingMode() == ClaimingMode.EXCLUSIVE) {
            // Remove blocks claimed by other claimers of the same type
            for (var c : DepositClaimerInstanceHolder.getInstancesWithIntersectingArea(this, level, getClaimerType())) {
                var claimedBlocks = c.getClaimedDepositBlocks();
                if (claimedBlocks != null) vein.removeAll(claimedBlocks);
            }
        }
        return vein;
    }

    default CompoundTag serializeDepositBlockClaimer() {
        var root = new CompoundTag();

        var list = new ListTag();
        var claimedBlocks = getClaimedDepositBlocks();
        if (claimedBlocks != null) {
            for (var bp : claimedBlocks) {
                list.add(LongTag.valueOf(bp.asLong()));
            }
            root.put("claimed_blocks", list);
        }
        return root;
    }

    default void deserializeDepositBlockClaimer(CompoundTag nbt) {
        var alreadyClaimedBlocks = getClaimedDepositBlocks();
        Set<BlockPos> newlyClaimedBlocks = null;

        // If list exists (even if empty), the claimer has finished claiming
        if (nbt.get("claimed_blocks") instanceof ListTag list) {
            newlyClaimedBlocks = new HashSet<BlockPos>(nbt.size());
            for (var t : list) {
                if (!(t instanceof LongTag lt)) continue;
                newlyClaimedBlocks.add(BlockPos.of(lt.getAsLong()));
            }
            if (alreadyClaimedBlocks != null && alreadyClaimedBlocks.equals(newlyClaimedBlocks)) return;
        } else if (alreadyClaimedBlocks == null) {
            return;
        }

        var level = getLevel();
        boolean updateOutline = level != null && level.isClientSide;

        if (updateOutline) DepositClaimerOutlineRenderer.removeClaimer(this);
        setClaimedDepositBlocks(newlyClaimedBlocks);
        if (updateOutline) DepositClaimerOutlineRenderer.addClaimer(this);
    }

    /// All claimers whose area intersects the provided area will reclaim their blocks
    static void reclaimArea(Level level, BoundingBox area, ClaimerType type) {
        var claimers = DepositClaimerInstanceHolder.getInstancesWithIntersectingArea(level, area, type);
        for (var c : claimers) {
            c.claimDepositBlocks();
        }
    }

    record ClaimingArea(int radius, int length) {}

    record ClaimerType(String name) {
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            } else {
                return obj instanceof ClaimerType ct && this.name.equalsIgnoreCase(ct.name);
            }
        }
    }

    enum ClaimingMode {
        EXCLUSIVE, STACKABLE
    }
}
