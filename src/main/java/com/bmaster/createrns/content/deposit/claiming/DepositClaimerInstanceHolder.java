package com.bmaster.createrns.content.deposit.claiming;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.content.deposit.claiming.IDepositBlockClaimer.ClaimerType;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class DepositClaimerInstanceHolder {
    private static final Object2ObjectOpenHashMap<ClaimerType,
            Object2ObjectOpenHashMap<ResourceKey<Level>, ObjectOpenHashSet<BlockPos>>> INSTANCES = new Object2ObjectOpenHashMap<>();

    public static Set<IDepositBlockClaimer> getInstancesWithinManhattanDistance(
            Level level, BlockPos pos, int distance, ClaimerType type
    ) {
        return getClaimersFromLevelAndType(level, type).stream()
                .filter(i -> {
                    var anchor = i.getAnchor();
                    return anchor != null && anchor.distManhattan(pos) <= distance;
                })
                .collect(Collectors.toUnmodifiableSet());
    }

    /// Does not add passed claimer to the set
    public static Set<IDepositBlockClaimer> getInstancesWithIntersectingArea(
            IDepositBlockClaimer claimer, Level level, ClaimerType type
    ) {
        var anchor = claimer.getAnchor();
        var bb = claimer.getClaimingBoundingBox();
        if (anchor == null || bb == null) return Set.of();

        return getClaimersFromLevelAndType(level, type).stream()
                .filter(c -> {
                    var cur_bb = c.getClaimingBoundingBox();
                    var cAnchor = c.getAnchor();
                    return cAnchor != null && !cAnchor.equals(anchor) && cur_bb != null && bb.intersects(cur_bb);
                })
                .collect(Collectors.toUnmodifiableSet());
    }

    public static Set<IDepositBlockClaimer> getInstancesWithIntersectingArea(
            Level level, BoundingBox area, ClaimerType type
    ) {
        return getClaimersFromLevelAndType(level, type).stream()
                .filter(c -> {
                    var cur_bb = c.getClaimingBoundingBox();
                    return cur_bb != null && area.intersects(cur_bb);
                })
                .collect(Collectors.toUnmodifiableSet());
    }

    public static Set<IDepositBlockClaimer> getInstancesThatCanClaim(
            Level level, BlockPos bp, ClaimerType type
    ) {
        return getClaimersFromLevelAndType(level, type).stream()
                .filter(m -> {
                    var cur_ma = m.getClaimingBoundingBox();
                    return cur_ma != null && cur_ma.isInside(bp);
                })
                .collect(Collectors.toUnmodifiableSet());
    }

    public static void addClaimer(IDepositClaimerHolder holder, Level level) {
        var claimer = holder.getClaimer().orElse(null);
        if (claimer == null) return;
        DepositClaimerInstanceHolder.INSTANCES
                .computeIfAbsent(claimer.getClaimerType(), k -> new Object2ObjectOpenHashMap<>())
                .computeIfAbsent(level.dimension(), k -> new ObjectOpenHashSet<>())
                .add(holder.getBlockPos());
    }

    public static void removeClaimer(IDepositClaimerHolder holder, Level level) {
        var claimer = holder.getClaimer().orElse(null);
        if (claimer == null) return;
        var type = claimer.getClaimerType();
        var typeMap = DepositClaimerInstanceHolder.INSTANCES.get(type);
        if (typeMap == null) {
            CreateRNS.LOGGER.error("Could not get a set of deposit claimer instances of type {}", type.name());
            return;
        }
        var levelSet = typeMap.get(level.dimension());
        if (levelSet == null) {
            CreateRNS.LOGGER.error("Could not get a set of deposit claimer instances at level {}", level);
            return;
        }
        levelSet.remove(holder.getBlockPos());
        if (levelSet.isEmpty()) typeMap.remove(level.dimension());
        if (typeMap.isEmpty()) INSTANCES.remove(type);
    }

    private static Set<IDepositBlockClaimer> getClaimersFromLevelAndType(Level l, ClaimerType t) {
        var typeMap = INSTANCES.get(t);
        if (typeMap == null) return Set.of();
        var levelSet = typeMap.get(l.dimension());
        if (levelSet == null) return Set.of();
        return levelSet.stream()
                .map(bp -> {
                    var be = l.getBlockEntity(bp);
                    if (!(be instanceof IDepositClaimerHolder holder)) return null;
                    return holder.getClaimer().orElse(null);

                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }
}
