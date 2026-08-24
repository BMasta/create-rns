package com.bmaster.createrns.content.deposit.claiming;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.content.deposit.operating.sublevel.OperatingSublevelAdapter.SidedOperatingSublevel;
import com.bmaster.createrns.content.deposit.operating.sublevel.OperatingSublevelAdapterHolder;
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
    // Maps the target operating sublevel (i.e. the sublevel a claimer's operating bounding box) to claimers
    private static final Object2ObjectOpenHashMap<SidedOperatingSublevel, ObjectOpenHashSet<BlockPos>> instances =
            new Object2ObjectOpenHashMap<>();

    public static Set<IDepositBlockClaimer> getInstancesWithinManhattanDistance(Level level, BlockPos pos, int distance) {
        return getClaimers(level).stream()
                .filter(i -> {
                    var start = i.getOperatingStart();
                    return start != null && OperatingSublevelAdapterHolder.getAdapter().distManhattan(level, start, pos) <= distance;
                })
                .collect(Collectors.toUnmodifiableSet());
    }

    /// Does not add passed claimer to the set
    public static Set<IDepositBlockClaimer> getInstancesWithIntersectingArea(BoundingBox area, IDepositBlockClaimer except) {
        var level = except.getLevel();
        if (level == null) return Set.of();
        var claimerPos = except.getBlockPos();
        var targetReferencePos = new BlockPos(area.minX(), area.minY(), area.minZ());

        var claimers = getClaimers(level, SidedOperatingSublevel.of(level, claimerPos));
        claimers.addAll(getClaimers(level, SidedOperatingSublevel.of(level, targetReferencePos)));

        return claimers.stream()
                .filter(curClaimer -> {
                    var curArea = curClaimer.getOperatingBoundingBox();
                    return !curClaimer.equals(except) && curArea != null && area.intersects(curArea);
                })
                .collect(Collectors.toUnmodifiableSet());
    }

    public static Set<IDepositBlockClaimer> getInstancesWithIntersectingArea(Level level, BoundingBox area) {
        var referencePos = new BlockPos(area.minX(), area.minY(), area.minZ());
        return getClaimers(level, SidedOperatingSublevel.of(level, referencePos)).stream()
                .filter(c -> {
                    var cur_bb = c.getOperatingBoundingBox();
                    return cur_bb != null && area.intersects(cur_bb);
                })
                .collect(Collectors.toUnmodifiableSet());
    }

    public static Set<IDepositBlockClaimer> getInstancesThatCanClaim(Level level, BlockPos pos) {
        return getClaimers(level, SidedOperatingSublevel.of(level, pos)).stream()
                .filter(m -> {
                    var cur_ma = m.getOperatingBoundingBox();
                    return cur_ma != null && cur_ma.isInside(pos);
                })
                .collect(Collectors.toUnmodifiableSet());
    }

    public static void addClaimer(IDepositBlockClaimer claimer) {
        var level = claimer.getLevel();
        var area = claimer.getOperatingBoundingBox();
        if (level == null || area == null) return;

        instances.computeIfAbsent(SidedOperatingSublevel.of(level, area.getCenter()), k -> new ObjectOpenHashSet<>())
                .add(claimer.getBlockPos());
    }

    public static void removeClaimer(IDepositBlockClaimer claimer) {
        var level = claimer.getLevel();
        var area = claimer.getOperatingBoundingBox();
        if (level == null || area == null) return;
        var sublevel = SidedOperatingSublevel.of(level, area.getCenter());

        var levelSet = instances.get(sublevel);
        if (levelSet == null) {
            CreateRNS.LOGGER.error("Could not get a set of deposit claimers at level {}", level);
            return;
        }

        levelSet.remove(claimer.getBlockPos());
        if (levelSet.isEmpty()) instances.remove(sublevel);
    }

    public static void clear() {
        instances.clear();
    }

    /// Reference position may be used to resolve a sublevel
    private static ObjectOpenHashSet<IDepositBlockClaimer> getClaimers(Level level, SidedOperatingSublevel sublevel) {
        var levelSet = instances.get(sublevel);
        if (levelSet == null) return new ObjectOpenHashSet<>();

        var claimers = new ObjectOpenHashSet<IDepositBlockClaimer>();
        var iterator = levelSet.iterator();
        while (iterator.hasNext()) {
            var bp = iterator.next();
            var be = level.getBlockEntity(bp);
            if (!(be instanceof IClaimerHolderBE holder)) {
                iterator.remove();
                continue;
            }
            claimers.add(holder.getClaimer());
        }

        if (levelSet.isEmpty()) instances.remove(sublevel);

        return claimers;
    }

    private static ObjectOpenHashSet<IDepositBlockClaimer> getClaimers(Level level) {
        return instances.keySet().stream()
                .flatMap(sublevel -> getClaimers(level, sublevel).stream())
                .collect(Collectors.toCollection(ObjectOpenHashSet::new));
    }
}
