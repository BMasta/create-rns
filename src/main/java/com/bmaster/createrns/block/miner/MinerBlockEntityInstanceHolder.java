package com.bmaster.createrns.block.miner;

import com.bmaster.createrns.util.Utils;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.Set;
import java.util.stream.Collectors;

public class MinerBlockEntityInstanceHolder {
    private static final Object2ObjectOpenHashMap<Level, ObjectOpenHashSet<MinerBlockEntity>> INSTANCES =
            new Object2ObjectOpenHashMap<>();

    public static Set<MinerBlockEntity> getInstances(Level level) {
        var levelSet = INSTANCES.get(level);
        if (levelSet == null) return Set.of();
        return levelSet.stream().collect(Collectors.toUnmodifiableSet());
    }

    public static Set<MinerBlockEntity> getInstancesWithinManhattanDistance(Level level, BlockPos pos, int distance) {
        var levelSet = INSTANCES.get(level);
        if (levelSet == null) return Set.of();
        return levelSet.stream()
                .filter(i -> i.getBlockPos().distManhattan(pos) <= distance)
                .collect(Collectors.toUnmodifiableSet());
    }

    public static Set<MinerBlockEntity> getInstancesWithinChebyshevDistance(Level level, BlockPos pos, int distance) {
        var levelSet = INSTANCES.get(level);
        if (levelSet == null) return Set.of();
        return levelSet.stream()
                .filter(i -> Utils.distChebyshev(i.getBlockPos(), pos) <= distance)
                .collect(Collectors.toUnmodifiableSet());
    }

    protected static void addInstance(MinerBlockEntity be) {
        var l = be.getLevel();
        if (l == null) return;
        MinerBlockEntityInstanceHolder.INSTANCES.computeIfAbsent(l, k -> new ObjectOpenHashSet<>()).add(be);
    }

    protected static void removeInstance(MinerBlockEntity be) {
        var l = be.getLevel();
        if (l == null) return;
        var levelSet = MinerBlockEntityInstanceHolder.INSTANCES.get(l);
        levelSet.remove(be);
        if (levelSet.isEmpty()) MinerBlockEntityInstanceHolder.INSTANCES.remove(l);
    }
}
