package com.bmaster.createrns.content.deposit.info;

import com.bmaster.createrns.CreateRNS;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public final class FoundDepositClientCache {
    private static final Map<ResourceKey<Level>, Set<ClientDepositLocation>> FOUND_DEPOSITS = new Object2ObjectOpenHashMap<>();

    private FoundDepositClientCache() {
    }

    public static void replaceAll(Map<ResourceKey<Level>, ? extends Collection<ClientDepositLocation>> foundDeposits) {
        FOUND_DEPOSITS.clear();
        int count = 0;
        for (var entry : foundDeposits.entrySet()) {
            if (entry.getValue().isEmpty()) continue;
            var deposits = new ObjectOpenHashSet<>(entry.getValue());
            FOUND_DEPOSITS.put(entry.getKey(), deposits);
            count += deposits.size();
        }
        CreateRNS.LOGGER.trace("Updated found deposit cache with {} deposits across {} dimensions",
                count, FOUND_DEPOSITS.size());
    }

    public static boolean add(ResourceKey<Level> dimension, ClientDepositLocation deposit) {
        var added = FOUND_DEPOSITS.computeIfAbsent(dimension, ignored -> new ObjectOpenHashSet<>())
                .add(deposit);
        CreateRNS.LOGGER.trace("{} found deposit {} [{} : {}]", added ? "Added" : "Skipped duplicate",
                deposit.getKey().location(), deposit.getLocationStr(), dimension.location());
        return added;
    }

    public static boolean remove(ResourceKey<Level> dimension, ClientDepositLocation deposit) {
        var deposits = FOUND_DEPOSITS.get(dimension);
        if (deposits == null || !deposits.remove(deposit)) {
            CreateRNS.LOGGER.trace("Could not remove found deposit {} [{} : {}]",
                    deposit.getKey().location(), deposit.getLocationStr(), dimension.location());
            return false;
        }
        if (deposits.isEmpty()) FOUND_DEPOSITS.remove(dimension);
        CreateRNS.LOGGER.trace("Removed found deposit {} [{} : {}]",
                deposit.getKey().location(), deposit.getLocationStr(), dimension.location());
        return true;
    }

    public static void clear() {
        int count = FOUND_DEPOSITS.values().stream().mapToInt(Set::size).sum();
        FOUND_DEPOSITS.clear();
        CreateRNS.LOGGER.trace("Cleared found deposit cache ({} deposits removed)", count);
    }

    public static void clear(ResourceKey<Level> dimension) {
        var removed = FOUND_DEPOSITS.remove(dimension);
        int count = removed == null ? 0 : removed.size();
        CreateRNS.LOGGER.trace("Cleared found deposit cache for {} ({} deposits removed)",
                dimension.location(), count);
    }

    public static Set<ClientDepositLocation> getDeposits(ResourceKey<Level> dimension) {
        var deposits = FOUND_DEPOSITS.get(dimension);
        return deposits == null ? Collections.emptySet() : Collections.unmodifiableSet(deposits);
    }

    public static boolean isEmpty() {
        return FOUND_DEPOSITS.isEmpty();
    }
}
