package com.bmaster.createrns.deposit;

import com.bmaster.createrns.RNSTags;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

@ParametersAreNonnullByDefault
public interface IDepositBlockClaimer {
    Level getLevel();

    ClaimingAreaSpec getClaimingAreaSpec();

    BlockPos getAnchor();

    Set<BlockPos> getClaimedDepositBlocks();

    void setClaimedDepositBlocks(Set<BlockPos> claimedBlocks);

    void claimDepositBlocks();

    default @Nullable BoundingBox getClaimingBoundingBox() {
        var level = getLevel();
        var spec = getClaimingAreaSpec();
        var anchor = getAnchor();
        int px = anchor.getX(), py = anchor.getY(), pz = anchor.getZ();
        int minBuildHeight = level.getMinBuildHeight(), maxBuildHeight = level.getMaxBuildHeight();

        int yMin = Mth.clamp(py + spec.verticalOffset() - spec.height() + 1, minBuildHeight, maxBuildHeight);
        int yMax = Mth.clamp(py + spec.verticalOffset(), minBuildHeight, maxBuildHeight);

        return new BoundingBox(
                px - spec.radius(), yMin, pz - spec.radius(),
                px + spec.radius(), yMax, pz + spec.radius());
    }

    default Set<BlockPos> getConfinedDepositVein() {
        var level = getLevel();
        var spec = getClaimingAreaSpec();
        var anchor = getAnchor();
        var ma = getClaimingBoundingBox();
        if (ma == null) return Set.of();

        Queue<BlockPos> q = new ArrayDeque<>();
        LongOpenHashSet visited = new LongOpenHashSet(ma.getXSpan() * ma.getYSpan() * ma.getZSpan());

        q.offer(anchor.relative(Direction.Axis.Y, spec.verticalOffset()));
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
        for (var c : DepositClaimerInstanceHolder.getInstancesWithIntersectingArea(this, level)) {
            vein.removeAll(c.getClaimedDepositBlocks());
        }
        return vein;
    }

    default CompoundTag serializeDepositBlockClaimer(HolderLookup.Provider provider) {
        var root = new CompoundTag();

        var list = new ListTag();
        var blocks = getClaimedDepositBlocks();
        for (var bp : blocks) {
            list.add(LongTag.valueOf(bp.asLong()));
        }
        root.put("claimed_blocks", list);
        return root;
    }

    default void deserializeDepositBlockClaimer(HolderLookup.Provider provider, CompoundTag nbt) {
        var blocks = new HashSet<BlockPos>(nbt.size());
        if (!(nbt.get("claimed_blocks") instanceof ListTag list)) return;
        for (var t : list) {
            if (!(t instanceof LongTag lt)) continue;
            blocks.add(BlockPos.of(lt.getAsLong()));
        }
        setClaimedDepositBlocks(blocks);
    }

    /// All claimers whose area intersects the provided area will reclaim their blocks
    static void reclaimArea(Level level, BoundingBox area) {
        var claimers = DepositClaimerInstanceHolder.getInstancesWithIntersectingArea(level, area);
        for (var c : claimers) {
            c.claimDepositBlocks();
        }
    }

    record ClaimingAreaSpec(int radius, int height, int verticalOffset) {
        public static final Codec<ClaimingAreaSpec> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.intRange(0, Integer.MAX_VALUE).fieldOf("radius").forGetter(ClaimingAreaSpec::radius),
                Codec.intRange(0, Integer.MAX_VALUE).fieldOf("height").forGetter(ClaimingAreaSpec::height),
                Codec.INT.fieldOf("vertical_offset").forGetter(ClaimingAreaSpec::verticalOffset)
        ).apply(i, ClaimingAreaSpec::new));
    }
}
