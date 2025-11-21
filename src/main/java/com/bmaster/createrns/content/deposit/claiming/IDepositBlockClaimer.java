package com.bmaster.createrns.content.deposit.claiming;

import com.bmaster.createrns.RNSTags;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
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
import java.util.function.Function;
import java.util.stream.Collectors;

@ParametersAreNonnullByDefault
public interface IDepositBlockClaimer {
    ClaimingMode getClaimingMode();

    ClaimerType getClaimerType();

    Level getLevel();

    @Nullable ClaimingAreaSpec getClaimingAreaSpec();

    BlockPos getAnchor();

    Direction getClaimingDirection();

    Set<BlockPos> getClaimedDepositBlocks();

    void setClaimedDepositBlocks(Set<BlockPos> claimedBlocks);

    void claimDepositBlocks();

    default @Nullable BoundingBox getClaimingBoundingBox() {
        var level = getLevel();
        var spec = getClaimingAreaSpec();
        if (spec == null) return null;
        var anchor = getAnchor();
        var dir = getClaimingDirection();
        Vec3i pos = new Vec3i(anchor.getX(), anchor.getY(), anchor.getZ());

        var minOffset = dir.getNormal().multiply(
                dir.getAxisDirection() == Direction.AxisDirection.POSITIVE ? spec.offset : spec.offset + spec.length - 1);
        var maxOffset = dir.getNormal().multiply(
                dir.getAxisDirection() == Direction.AxisDirection.NEGATIVE ? spec.offset : spec.offset + spec.length - 1);

        var minRadiusDelta = diagonalPlaneVec(dir, false).multiply(spec.radius);
        var maxRadiusDelta = diagonalPlaneVec(dir, true).multiply(spec.radius);

        var minPos = pos.offset(minOffset).offset(minRadiusDelta);
        var maxPos = pos.offset(maxOffset).offset(maxRadiusDelta);

        return new BoundingBox(minPos.getX(), minPos.getY(), minPos.getZ(), maxPos.getX(), maxPos.getY(), maxPos.getZ());
    }

    default Set<BlockPos> getConfinedDepositVein() {
        var level = getLevel();
        var spec = getClaimingAreaSpec();
        if (spec == null) return Set.of();
        var anchor = getAnchor();
        var ma = getClaimingBoundingBox();
        if (ma == null) return Set.of();
        var dir = getClaimingDirection();

        Queue<BlockPos> q = new ArrayDeque<>();
        LongOpenHashSet visited = new LongOpenHashSet(ma.getXSpan() * ma.getYSpan() * ma.getZSpan());

        q.offer(anchor.relative(dir, spec.offset()));
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
                vein.removeAll(c.getClaimedDepositBlocks());
            }
        }
        return vein;
    }

    default CompoundTag serializeDepositBlockClaimer() {
        var root = new CompoundTag();

        var list = new ListTag();
        var blocks = getClaimedDepositBlocks();
        for (var bp : blocks) {
            list.add(LongTag.valueOf(bp.asLong()));
        }
        root.put("claimed_blocks", list);
        return root;
    }

    default void deserializeDepositBlockClaimer(CompoundTag nbt) {
        var blocks = new HashSet<BlockPos>(nbt.size());
        if (!(nbt.get("claimed_blocks") instanceof ListTag list)) return;
        for (var t : list) {
            if (!(t instanceof LongTag lt)) continue;
            blocks.add(BlockPos.of(lt.getAsLong()));
        }
        if (getClaimedDepositBlocks().equals(blocks)) return;

        // Clients also need to update the outline
        var level = getLevel();
        if (level != null && level.isClientSide) DepositClaimerOutlineRenderer.removeClaimer(this);
        setClaimedDepositBlocks(blocks);
        if (level != null && level.isClientSide) DepositClaimerOutlineRenderer.addClaimer(this);
    }

    /// All claimers whose area intersects the provided area will reclaim their blocks
    static void reclaimArea(Level level, BoundingBox area, ClaimerType type) {
        var claimers = DepositClaimerInstanceHolder.getInstancesWithIntersectingArea(level, area, type);
        for (var c : claimers) {
            c.claimDepositBlocks();
        }
    }

    record ClaimingAreaSpec(int radius, int length, int offset) {
        public static final Codec<ClaimingAreaSpec> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.intRange(0, Integer.MAX_VALUE).fieldOf("radius").forGetter(ClaimingAreaSpec::radius),
                Codec.intRange(1, Integer.MAX_VALUE).fieldOf("length").forGetter(ClaimingAreaSpec::length),
                Codec.INT.fieldOf("offset").forGetter(ClaimingAreaSpec::offset)
        ).apply(i, ClaimingAreaSpec::new));
    }

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

    private static Vec3i diagonalPlaneVec(Direction dir, boolean positive) {
        var n = dir.getNormal();
        var flipVal = (positive ? 1 : -1);
        Function<Integer, Integer> flip = v -> v == 0 ? flipVal : 0;
        return new Vec3i(flip.apply(n.getX()), flip.apply(n.getY()), flip.apply(n.getZ()));
    }
}
