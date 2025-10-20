package com.bmaster.createrns.mining;

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
        var pos = be.getBlockPos();
        var bb = be.getMiningArea(l);

        return levelSet.stream()
                .filter(m -> !m.getBlockPos().equals(pos) && m.getMiningArea(l).intersects(bb))
                .collect(Collectors.toUnmodifiableSet());
    }

    public static Set<MiningBlockEntity> getInstancesThatCouldMine(Level l, BlockPos bp) {
        var levelSet = INSTANCES.get(l);
        if (levelSet == null) return Set.of();

        return levelSet.stream()
                .filter(m -> m.getMiningArea(l).isInside(bp))
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
        levelSet.remove(be);
        if (levelSet.isEmpty()) MiningBlockEntityInstanceHolder.INSTANCES.remove(l);
    }
}
