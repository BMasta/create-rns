package com.bmaster.createrns.content.deposit.mining;

import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.CatalystRequirementSet;
import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.CatalystUsageStats;
import com.bmaster.createrns.util.Utils;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.util.INBTSerializable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class MiningStats implements INBTSerializable<CompoundTag> {
    public enum RateEstimationStatus {
        NONE, SOME, ALL
    }

    public static final int UPTIME_GRACE_PERIOD_TICKS = 40;

    public long minedResources = 0;
    public long consumedResources = 0;
    public final PerDepositResourceStat remainingResources = new PerDepositResourceStat();
    public final PerDepositCatalystUsageStats catStats = new PerDepositCatalystUsageStats();
    public final Object2LongOpenHashMap<Item> minedItems = new Object2LongOpenHashMap<>();

    private boolean gracePeriod = true;
    private long uptimeTicks = 0;
    private long startGameTime = -1;

    public long getElapsedTicks(Level level) {
        if (level == null || !isStarted()) return 0;
        long elapsed = level.getGameTime() - startGameTime - UPTIME_GRACE_PERIOD_TICKS;
        if (elapsed >= 0) gracePeriod = false;
        return Math.max(0, elapsed);
    }

    public boolean isStarted() {
        return startGameTime >= 0;
    }

    public long getUptimeTicks() {
        return uptimeTicks;
    }

    public boolean updateUptimeTicks(Level level, boolean mining) {
        if (!isStarted() || !mining) return false;
        // Refresh grace period
        if (gracePeriod) getElapsedTicks(level);
        if (gracePeriod) return false;
        uptimeTicks++;
        return true;
    }

    public void start(Level level) {
        reset();
        startGameTime = level.getGameTime();
    }

    public void reset() {
        gracePeriod = true;
        startGameTime = -1;
        uptimeTicks = 0;
        minedResources = 0;
        consumedResources = 0;
        remainingResources.clear();
        catStats.clear();
        minedItems.clear();
    }

    public long getTotalRemainingResources() {
        long total = 0;
        for (var v : remainingResources.values()) {
            if (v <= 0) continue;
            total += v;
        }
        return total;
    }

    public LinkedHashMap<Item, Float> getRates(MiningProcess process) {
        return getEstimatedRates(process, 0).sequencedEntrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> {
                    double hrs = (double) getElapsedTicks(process.level) / (SharedConstants.TICKS_PER_MINUTE * 60);
                    if (hrs <= 0) return 0f;
                    return (float) (minedItems.getOrDefault(e.getKey(), 0) / hrs);
                },
                Float::sum,
                LinkedHashMap::new
        ));
    }

    public List<Holder<CatalystRequirementSet>> getLastSatisfiedCRSes(MiningProcess process) {
        return process.innerProcesses.stream()
                .flatMap(ip -> {
                    var cs = catStats.ofInnerProcess(ip);
                    return cs.lastTickedCRSes != null ? cs.lastTickedCRSes.stream() : Stream.of();
                })
                .distinct()
                .map(crsId -> CatalystRequirementSet.get(process.level.registryAccess(), crsId))
                .sorted(Comparator.comparingInt(crs -> crs.value().displayPriority))
                .toList();
    }

    public RateEstimationStatus getRateEstimationStatus(MiningProcess process) {
        boolean anyEstimated = false;
        boolean allEstimated = true;
        for (var ip : process.innerProcesses) {
            if (!catStats.ofInnerProcess(ip).isComputed()) allEstimated = false;
            else anyEstimated = true;
            if (anyEstimated && !allEstimated) return RateEstimationStatus.SOME;
        }
        return (allEstimated) ? RateEstimationStatus.ALL : RateEstimationStatus.NONE;
    }

    public LinkedHashMap<Item, Float> getEstimatedRates(MiningProcess process, int progressPerTick) {
        var rates = new Object2FloatOpenHashMap<Item>();

        long elapsedTicks = getElapsedTicks(process.level);
        float uptime = elapsedTicks > 0 ? (float) getUptimeTicks() / elapsedTicks : 1;

        for (var p : process.innerProcesses) {
            var cs = catStats.ofInnerProcess(p);
            if (!cs.isComputed()) continue;
            var ys = p.recipe.getYields();
            for (int i = 0; i < ys.size(); ++i) {
                float chance = cs.getLastComputedChance(i);
                if (chance <= 0f) continue;
                var y = ys.get(i);
                for (var wi : ys.get(i).items) {
                    rates.addTo(wi.item, (float) wi.weight / y.getTotalWeight() / p.maxProgress * chance * uptime);
                }
            }
        }

        var progressPerHour = 60 * SharedConstants.TICKS_PER_MINUTE * progressPerTick;
        return rates.object2FloatEntrySet().stream()
                .sorted((first, second) -> {
                    float firstRate = first.getFloatValue();
                    float secondRate = second.getFloatValue();
                    // First sort by rate
                    if (firstRate != secondRate) return -Float.compare(firstRate, secondRate);
                    // Then by item id
                    var firstId = BuiltInRegistries.ITEM.getKeyOrNull(first.getKey());
                    var secondId = BuiltInRegistries.ITEM.getKeyOrNull(second.getKey());
                    if (firstId == null) return secondId == null ? 0 : 1;
                    if (secondId == null) return -1;
                    return firstId.toString().compareToIgnoreCase(secondId.toString());
                })
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getFloatValue() * progressPerHour,
                        (a, b) -> b, LinkedHashMap::new));
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        var nbt = new CompoundTag();
        if (startGameTime >= 0) nbt.putLong("start_time", startGameTime);
        nbt.putLong("uptime", uptimeTicks);
        nbt.putLong("mined_resources", minedResources);
        nbt.putLong("consumed_resources", consumedResources);

        var remainingTag = new CompoundTag();
        for (var e : remainingResources.object2LongEntrySet()) {
            remainingTag.putLong(BuiltInRegistries.BLOCK.getKey(e.getKey()).toString(), e.getLongValue());
        }
        nbt.put("remaining_resources", remainingTag);

        var minedItemsTag = new CompoundTag();
        for (var e : minedItems.object2LongEntrySet()) {
            minedItemsTag.putLong(BuiltInRegistries.ITEM.getKey(e.getKey()).toString(), e.getLongValue());
        }
        nbt.put("mined_items", minedItemsTag);

        var catUsageStatsTag = new CompoundTag();
        for (var e : catStats.object2ObjectEntrySet()) {
            catUsageStatsTag.put(
                    BuiltInRegistries.BLOCK.getKey(e.getKey()).toString(),
                    e.getValue().serializeNBT(provider));
        }
        nbt.put("catalyst_usage", catUsageStatsTag);

        return nbt;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        reset();
        if (tag.contains("start_time")) {
            startGameTime = tag.getLong("start_time");
        }
        uptimeTicks = tag.getLong("uptime");
        minedResources = tag.getLong("mined_resources");
        consumedResources = tag.getLong("consumed_resources");

        remainingResources.clear();
        var remainingTag = tag.getCompound("remaining_resources");
        for (var blockIdString : remainingTag.getAllKeys()) {
            var block = Utils.registryEntryFromString(BuiltInRegistries.BLOCK, blockIdString);
            if (block == null) continue;
            remainingResources.put(block, remainingTag.getLong(blockIdString));
        }

        minedItems.clear();
        var minedItemsTag = tag.getCompound("mined_items");
        for (var itemIdString : minedItemsTag.getAllKeys()) {
            var item = Utils.registryEntryFromString(BuiltInRegistries.ITEM, itemIdString);
            if (item == null) continue;
            minedItems.put(item, minedItemsTag.getLong(itemIdString));
        }

        catStats.clear();
        var catStatsTag = tag.getCompound("catalyst_usage");
        for (var blockIdString : catStatsTag.getAllKeys()) {
            var block = Utils.registryEntryFromString(BuiltInRegistries.BLOCK, blockIdString);
            if (block == null) continue;
            var cs = new CatalystUsageStats();
            cs.deserializeNBT(provider, catStatsTag.getCompound(blockIdString));
            catStats.put(block, cs);
        }
    }

    public static class PerDepositCatalystUsageStats extends Object2ObjectOpenHashMap<Block, CatalystUsageStats> {
        public CatalystUsageStats ofDeposit(Block block) {
            return super.computeIfAbsent(block, b -> new CatalystUsageStats());
        }

        public CatalystUsageStats ofInnerProcess(InnerProcess ip) {
            return ofDeposit(ip.recipe.getDepositBlock());
        }

        public void recompute(InnerProcess ip) {
            // Simulate catalyst usage, so that catalyst handler can record which catalysts were used
            ip.catalystHandler.useCatalysts(ofInnerProcess(ip), true);
        }
    }

    public static class PerDepositResourceStat extends Object2LongOpenHashMap<Block> {
        public long ofDeposit(Block block) {
            return super.getLong(block);
        }

        public long ofInnerProcess(InnerProcess ip) {
            return ofDeposit(ip.recipe.getDepositBlock());
        }
    }
}
