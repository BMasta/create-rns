package com.bmaster.createrns.content.deposit.claiming;

import com.bmaster.createrns.CreateRNS;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Set;
import java.util.stream.Collectors;

@ParametersAreNonnullByDefault
public class DepositClaimerInstanceHolder {
    private static final Object2ObjectOpenHashMap<Level, ObjectOpenHashSet<IDepositBlockClaimer>> INSTANCES =
            new Object2ObjectOpenHashMap<>();

    public static Set<IDepositBlockClaimer> getInstances(Level level) {
        var levelSet = INSTANCES.get(level);
        if (levelSet == null) return Set.of();
        return levelSet.stream().collect(Collectors.toUnmodifiableSet());
    }

    public static Set<IDepositBlockClaimer> getInstancesWithinManhattanDistance(Level level, BlockPos pos, int distance) {
        var levelSet = INSTANCES.get(level);
        if (levelSet == null) return Set.of();
        return levelSet.stream()
                .filter(i -> i.getAnchor().distManhattan(pos) <= distance)
                .collect(Collectors.toUnmodifiableSet());
    }

    public static Set<IDepositBlockClaimer> getInstancesWithIntersectingArea(IDepositBlockClaimer claimer, Level level) {
        var levelSet = INSTANCES.get(level);
        if (levelSet == null) return Set.of();
        var anchor = claimer.getAnchor();
        var bb = claimer.getClaimingBoundingBox();
        if (bb == null) return Set.of();

        return levelSet.stream()
                .filter(c -> {
                    var cur_bb = c.getClaimingBoundingBox();
                    return !c.getAnchor().equals(anchor) && cur_bb != null && bb.intersects(cur_bb);
                })
                .collect(Collectors.toUnmodifiableSet());
    }

    public static Set<IDepositBlockClaimer> getInstancesWithIntersectingArea(Level level, BoundingBox area) {
        var levelSet = INSTANCES.get(level);
        if (levelSet == null) return Set.of();

        return levelSet.stream()
                .filter(c -> {
                    var cur_bb = c.getClaimingBoundingBox();
                    return cur_bb != null && area.intersects(cur_bb);
                })
                .collect(Collectors.toUnmodifiableSet());
    }

    public static Set<IDepositBlockClaimer> getInstancesThatCanClaim(Level l, BlockPos bp) {
        var levelSet = INSTANCES.get(l);
        if (levelSet == null) return Set.of();

        return levelSet.stream()
                .filter(m -> {
                    var cur_ma = m.getClaimingBoundingBox();
                    return cur_ma != null && cur_ma.isInside(bp);
                })
                .collect(Collectors.toUnmodifiableSet());
    }

    public static void addClaimer(IDepositBlockClaimer claimer, Level level) {
        DepositClaimerInstanceHolder.INSTANCES.computeIfAbsent(level, k -> new ObjectOpenHashSet<>()).add(claimer);
    }

    public static void removeClaimer(IDepositBlockClaimer claimer, Level level) {
        var levelSet = DepositClaimerInstanceHolder.INSTANCES.get(level);
        if (levelSet == null) {
            CreateRNS.LOGGER.error("Could not get a set of deposit claimer instances at level {}", level);
            return;
        }
        levelSet.remove(claimer);
        if (levelSet.isEmpty()) DepositClaimerInstanceHolder.INSTANCES.remove(level);
    }
}
