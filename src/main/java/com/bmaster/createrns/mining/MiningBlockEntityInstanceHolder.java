package com.bmaster.createrns.mining;

import com.bmaster.createrns.CreateRNS;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.Set;
import java.util.stream.Collectors;

public class MiningBlockEntityInstanceHolder {
    private static final Object2ObjectOpenHashMap<Level, ObjectOpenHashSet<MiningBlockEntity>> INSTANCES =
            new Object2ObjectOpenHashMap<>();

    public static Set<MiningBlockEntity> getInstances(Level level) {
        var levelSet = INSTANCES.get(level);
        if (levelSet == null) return Set.of();
        return levelSet.stream().collect(Collectors.toUnmodifiableSet());
    }

    public static Set<MiningBlockEntity> getInstancesWithinManhattanDistance(Level level, BlockPos pos, int distance) {
        var levelSet = INSTANCES.get(level);
        if (levelSet == null) return Set.of();
        return levelSet.stream()
                .filter(i -> i.getBlockPos().distManhattan(pos) <= distance)
                .collect(Collectors.toUnmodifiableSet());
    }

    public static Set<MiningBlockEntity> getInstancesWithIntersectingMiningArea(MiningBlockEntity be) {
        Level l = be.getLevel();
        if (l == null) return Set.of();
        var levelSet = INSTANCES.get(l);
        if (levelSet == null) return Set.of();
        var ma = be.getMiningArea();
        if (ma == null) return Set.of();
        var pos = be.getBlockPos();

        return levelSet.stream()
                .filter(m -> {
                    var cur_ma = m.getMiningArea();
                    return !m.getBlockPos().equals(pos) && cur_ma != null && ma.intersects(cur_ma);
                })
                .collect(Collectors.toUnmodifiableSet());
    }

    public static Set<MiningBlockEntity> getInstancesMiningAt(Level l, BlockPos bp) {
        var levelSet = INSTANCES.get(l);
        if (levelSet == null) return Set.of();

        return levelSet.stream()
                .filter(m -> {
                    var cur_ma = m.getMiningArea();
                    return cur_ma != null && cur_ma.isInside(bp);
                })
                .collect(Collectors.toUnmodifiableSet());
    }

    protected static void addInstance(MiningBlockEntity be) {
        var l = be.getLevel();
        if (l == null) return;
        MiningBlockEntityInstanceHolder.INSTANCES.computeIfAbsent(l, k -> new ObjectOpenHashSet<>()).add(be);
    }

    protected static void removeInstance(MiningBlockEntity be) {
        var l = be.getLevel();
        if (l == null) return;
        var levelSet = MiningBlockEntityInstanceHolder.INSTANCES.get(l);
        if (levelSet == null) {
            CreateRNS.LOGGER.error("Could not get a set of miner instances at level {}", l);
            return;
        }
        levelSet.remove(be);
        if (levelSet.isEmpty()) MiningBlockEntityInstanceHolder.INSTANCES.remove(l);
    }
}
