package com.bmaster.createrns.content.deposit.claiming;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.content.deposit.claiming.IDepositBlockClaimer.ClaimerType;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Set;
import java.util.stream.Collectors;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class DepositClaimerInstanceHolder {
    private static final Object2ObjectOpenHashMap<ClaimerType,
            Object2ObjectOpenHashMap<Level, ObjectOpenHashSet<IDepositBlockClaimer>>> INSTANCES = new Object2ObjectOpenHashMap<>();

    public static Set<IDepositBlockClaimer> getInstancesWithinManhattanDistance(Level level, BlockPos pos, int distance,
                                                                                @Nullable ClaimerType type) {
        return setFromLevelAndType(level, type).stream()
                .filter(i -> {
                    var anchor = i.getAnchor();
                    return anchor != null && anchor.distManhattan(pos) <= distance;
                })
                .collect(Collectors.toUnmodifiableSet());
    }

    /// Does not add passed claimer to the set
    public static Set<IDepositBlockClaimer> getInstancesWithIntersectingArea(IDepositBlockClaimer claimer, Level level,
                                                                             @Nullable ClaimerType type) {
        var anchor = claimer.getAnchor();
        var bb = claimer.getClaimingBoundingBox();
        if (anchor == null || bb == null) return Set.of();

        return setFromLevelAndType(level, type).stream()
                .filter(c -> {
                    var cur_bb = c.getClaimingBoundingBox();
                    var cAnchor = c.getAnchor();
                    return cAnchor != null && !cAnchor.equals(anchor) && cur_bb != null && bb.intersects(cur_bb);
                })
                .collect(Collectors.toUnmodifiableSet());
    }

    public static Set<IDepositBlockClaimer> getInstancesWithIntersectingArea(Level level, BoundingBox area,
                                                                             @Nullable ClaimerType type) {
        return setFromLevelAndType(level, type).stream()
                .filter(c -> {
                    var cur_bb = c.getClaimingBoundingBox();
                    return cur_bb != null && area.intersects(cur_bb);
                })
                .collect(Collectors.toUnmodifiableSet());
    }

    public static Set<IDepositBlockClaimer> getInstancesThatCanClaim(Level level, BlockPos bp, @Nullable ClaimerType type) {
        return setFromLevelAndType(level, type).stream()
                .filter(m -> {
                    var cur_ma = m.getClaimingBoundingBox();
                    return cur_ma != null && cur_ma.isInside(bp);
                })
                .collect(Collectors.toUnmodifiableSet());
    }

    public static void addClaimer(IDepositBlockClaimer claimer, Level level) {
        DepositClaimerInstanceHolder.INSTANCES
                .computeIfAbsent(claimer.getClaimerType(), k -> new Object2ObjectOpenHashMap<>())
                .computeIfAbsent(level, k -> new ObjectOpenHashSet<>())
                .add(claimer);
    }

    public static void removeClaimer(IDepositBlockClaimer claimer, Level level) {
        var type = claimer.getClaimerType();
        var typeMap = DepositClaimerInstanceHolder.INSTANCES.get(type);
        if (typeMap == null) {
            CreateRNS.LOGGER.error("Could not get a set of deposit claimer instances of type {}", type.name());
            return;
        }
        var levelSet = typeMap.get(level);
        if (levelSet == null) {
            CreateRNS.LOGGER.error("Could not get a set of deposit claimer instances at level {}", level);
            return;
        }
        levelSet.remove(claimer);
        if (levelSet.isEmpty()) typeMap.remove(level);
        if (typeMap.isEmpty()) INSTANCES.remove(type);
    }

    private static Set<IDepositBlockClaimer> setFromLevelAndType(Level l, @Nullable ClaimerType t) {
        // Simply get the set
        if (t != null) {
            var typeMap = INSTANCES.get(t);
            if (typeMap == null) return Set.of();
            var levelSet = typeMap.get(l);
            return (levelSet != null) ? levelSet : Set.of();
        }

        // Aggregate sets of all types at that level
        return INSTANCES.values().stream()
                .map(m -> {
                    var s = m.get(l);
                    return (s != null) ? s : new ObjectOpenHashSet<IDepositBlockClaimer>();
                })
                .reduce((s1, s2) -> {
                    s1.addAll(s2);
                    return s1;
                }).orElse(new ObjectOpenHashSet<>());
    }
}
