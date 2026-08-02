package com.bmaster.createrns.content.deposit.claiming;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.content.deposit.claiming.IDepositBlockClaimer.ClaimerType;
import com.bmaster.createrns.content.deposit.mining.MiningBehaviour;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import javax.annotation.ParametersAreNonnullByDefault;
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

    public static void addClaimer(IDepositBlockClaimer claimer, Level level) {
        DepositClaimerInstanceHolder.INSTANCES
                .computeIfAbsent(claimer.getClaimerType(), k -> new Object2ObjectOpenHashMap<>())
                .computeIfAbsent(level.dimension(), k -> new ObjectOpenHashSet<>())
                .add(claimer.getBlockPos());
    }

    public static void removeClaimer(IDepositBlockClaimer claimer, Level level) {
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
        levelSet.remove(claimer.getBlockPos());
        if (levelSet.isEmpty()) typeMap.remove(level.dimension());
        if (typeMap.isEmpty()) INSTANCES.remove(type);
    }

    private static Set<IDepositBlockClaimer> getClaimersFromLevelAndType(Level l, ClaimerType t) {
        var typeMap = INSTANCES.get(t);
        if (typeMap == null) return Set.of();
        var levelSet = typeMap.get(l.dimension());
        if (levelSet == null) return Set.of();

        var claimers = new ObjectOpenHashSet<IDepositBlockClaimer>();
        var iterator = levelSet.iterator();
        while (iterator.hasNext()) {
            var bp = iterator.next();
            var be = l.getBlockEntity(bp);

            // Remove invalid claimers
            if (!(be instanceof SmartBlockEntity sbe)) {
                iterator.remove();
                continue;
            }

            var claimer = sbe.getBehaviour(MiningBehaviour.BEHAVIOUR_TYPE);
            if (claimer == null) {
                iterator.remove();
                continue;
            }

            claimers.add(claimer);
        }

        // Clean up collections if removing elements left them empty
        if (levelSet.isEmpty()) typeMap.remove(l.dimension());
        if (typeMap.isEmpty()) INSTANCES.remove(t);

        return claimers;
    }
}
