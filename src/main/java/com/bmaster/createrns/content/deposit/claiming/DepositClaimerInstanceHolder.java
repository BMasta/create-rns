package com.bmaster.createrns.content.deposit.claiming;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.content.deposit.mining.behaviour.MiningBehaviour;
import com.bmaster.createrns.content.deposit.operating.sublevel.OperatingSublevelAdapterHolder;
import com.bmaster.createrns.util.SidedDimension;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Set;
import java.util.stream.Collectors;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class DepositClaimerInstanceHolder {
    private static final Object2ObjectOpenHashMap<SidedDimension, ObjectOpenHashSet<BlockPos>> instances = new Object2ObjectOpenHashMap<>();

    /// Sublevel-aware
    public static Set<IDepositBlockClaimer> getInstancesWithinManhattanDistance(Level level, BlockPos pos, int distance) {
        return getClaimers(level, pos, true).stream()
                .filter(i -> {
                    var anchor = i.getOperatingAnchor();
                    return anchor != null && OperatingSublevelAdapterHolder.getAdapter().distManhattan(level, anchor, pos) <= distance;
                })
                .collect(Collectors.toUnmodifiableSet());
    }

    /// Does not add passed claimer to the set
    public static Set<IDepositBlockClaimer> getInstancesWithIntersectingArea(IDepositBlockClaimer claimer, Level level) {
        var anchor = claimer.getOperatingAnchor();
        var bb = claimer.getOperatingBoundingBox();
        if (anchor == null || bb == null) return Set.of();

        return getClaimers(level, anchor, false).stream()
                .filter(c -> {
                    var cur_bb = c.getOperatingBoundingBox();
                    var cAnchor = c.getOperatingAnchor();
                    return cAnchor != null && !cAnchor.equals(anchor) && cur_bb != null && bb.intersects(cur_bb);
                })
                .collect(Collectors.toUnmodifiableSet());
    }

    public static Set<IDepositBlockClaimer> getInstancesWithIntersectingArea(Level level, BoundingBox area) {
        var referencePos = new BlockPos(area.minX(), area.minY(), area.minZ());
        return getClaimers(level, referencePos, false).stream()
                .filter(c -> {
                    var cur_bb = c.getOperatingBoundingBox();
                    return cur_bb != null && area.intersects(cur_bb);
                })
                .collect(Collectors.toUnmodifiableSet());
    }

    public static Set<IDepositBlockClaimer> getInstancesThatCanClaim(Level level, BlockPos pos) {
        return getClaimers(level, pos, false).stream()
                .filter(m -> {
                    var cur_ma = m.getOperatingBoundingBox();
                    return cur_ma != null && cur_ma.isInside(pos);
                })
                .collect(Collectors.toUnmodifiableSet());
    }

    public static void addClaimer(IDepositBlockClaimer claimer, Level level) {
        DepositClaimerInstanceHolder.instances
                .computeIfAbsent(SidedDimension.of(level), k -> new ObjectOpenHashSet<>())
                .add(claimer.getBlockPos());
    }

    public static void removeClaimer(IDepositBlockClaimer claimer, Level level) {
        var sd = SidedDimension.of(level);
        var levelSet = DepositClaimerInstanceHolder.instances.get(sd);
        if (levelSet == null) {
            CreateRNS.LOGGER.error("Could not get a set of deposit claimer instances at level {}", level);
            return;
        }
        levelSet.remove(claimer.getBlockPos());
        if (levelSet.isEmpty()) DepositClaimerInstanceHolder.instances.remove(sd);
    }

    /// Reference position may be used to resolve a sublevel
    private static Set<IDepositBlockClaimer> getClaimers(
            Level level, BlockPos referencePos, boolean crossSublevel
    ) {
        var sd = SidedDimension.of(level);
        var levelSet = instances.get(sd);
        if (levelSet == null) return Set.of();

        var claimers = new ObjectOpenHashSet<IDepositBlockClaimer>();
        var iterator = levelSet.iterator();
        while (iterator.hasNext()) {
            var bp = iterator.next();
            var be = level.getBlockEntity(bp);

            // Remove invalid claimers
            if (!(be instanceof SmartBlockEntity sbe) ||
                    !(sbe.getBehaviour(MiningBehaviour.BEHAVIOUR_TYPE) instanceof IDepositBlockClaimer claimer)) {
                iterator.remove();
                continue;
            }

            var claimerLevel = claimer.getLevel();
            var claimerAnchor = claimer.getOperatingAnchor();
            if (claimerLevel != null && claimerAnchor != null && (crossSublevel || OperatingSublevelAdapterHolder.getAdapter()
                    .isSameSublevel(level, referencePos, claimerLevel, claimerAnchor))) {
                claimers.add(claimer);
            }
        }

        if (levelSet.isEmpty()) instances.remove(sd);

        return claimers;
    }
}
