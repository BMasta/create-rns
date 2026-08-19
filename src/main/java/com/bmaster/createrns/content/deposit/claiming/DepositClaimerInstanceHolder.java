package com.bmaster.createrns.content.deposit.claiming;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.content.deposit.mining.behaviour.MiningBehaviour;
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
    private static final Object2ObjectOpenHashMap<SidedDimension, ObjectOpenHashSet<BlockPos>> instances =
            new Object2ObjectOpenHashMap<>();

    public static Set<IDepositBlockClaimer> getInstancesWithinManhattanDistance(Level level, BlockPos pos, int distance) {
        return getClaimers(level).stream()
                .filter(i -> {
                    var start = i.getOperatingStart();
                    return start != null && start.distManhattan(pos) <= distance;
                })
                .collect(Collectors.toUnmodifiableSet());
    }

    /// Does not add passed claimer to the set
    public static Set<IDepositBlockClaimer> getInstancesWithIntersectingArea(BoundingBox area, IDepositBlockClaimer except) {
        var level = except.getLevel();
        if (level == null) return Set.of();
        return getClaimers(level).stream()
                .filter(curClaimer -> {
                    var curArea = curClaimer.getOperatingBoundingBox();
                    return !curClaimer.equals(except) && curArea != null && area.intersects(curArea);
                })
                .collect(Collectors.toUnmodifiableSet());
    }

    public static Set<IDepositBlockClaimer> getInstancesWithIntersectingArea(Level level, BoundingBox area) {
        return getClaimers(level).stream()
                .filter(c -> {
                    var cur_bb = c.getOperatingBoundingBox();
                    return cur_bb != null && area.intersects(cur_bb);
                })
                .collect(Collectors.toUnmodifiableSet());
    }

    public static Set<IDepositBlockClaimer> getInstancesThatCanClaim(Level level, BlockPos pos) {
        return getClaimers(level).stream()
                .filter(m -> {
                    var cur_ma = m.getOperatingBoundingBox();
                    return cur_ma != null && cur_ma.isInside(pos);
                })
                .collect(Collectors.toUnmodifiableSet());
    }

    public static void addClaimer(IDepositBlockClaimer claimer) {
        var level = claimer.getLevel();
        if (level == null) return;

        instances.computeIfAbsent(SidedDimension.of(level), k -> new ObjectOpenHashSet<>())
                .add(claimer.getBlockPos());
    }

    public static void removeClaimer(IDepositBlockClaimer claimer) {
        var level = claimer.getLevel();
        if (level == null) return;
        var sd = SidedDimension.of(level);

        var levelSet = instances.get(sd);
        if (levelSet == null) {
            CreateRNS.LOGGER.error("Could not get a set of deposit claimers at level {}", level);
            return;
        }

        levelSet.remove(claimer.getBlockPos());
        if (levelSet.isEmpty()) instances.remove(sd);
    }

    public static void clear() {
        instances.clear();
    }

    /// Reference position may be used to resolve a sublevel
    private static ObjectOpenHashSet<IDepositBlockClaimer> getClaimers(Level level) {
        var sd = SidedDimension.of(level);
        var levelSet = instances.get(sd);
        if (levelSet == null) return new ObjectOpenHashSet<>();

        var claimers = new ObjectOpenHashSet<IDepositBlockClaimer>();
        var iterator = levelSet.iterator();
        while (iterator.hasNext()) {
            var bp = iterator.next();
            var be = level.getBlockEntity(bp);
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

        if (levelSet.isEmpty()) instances.remove(sd);

        return claimers;
    }
}
