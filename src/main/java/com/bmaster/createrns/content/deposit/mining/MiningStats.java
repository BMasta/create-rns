package com.bmaster.createrns.content.deposit.mining;

import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class MiningStats implements INBTSerializable<CompoundTag> {
    public static final int UPTIME_GRACE_PERIOD_TICKS = 40;

    public long minedResources = 0;
    public long consumedResources = 0;
    public final Object2LongOpenHashMap<Block> remainingResources = new Object2LongOpenHashMap<>();
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
        float uptimePercentage = (float) uptimeTicks / getElapsedTicks(process.level);
        var estimatedRates = process.getEstimatedRates(0, uptimePercentage);
        return estimatedRates.entrySet().stream().collect(Collectors.toMap(
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

    @Override
    public CompoundTag serializeNBT() {
        var nbt = new CompoundTag();
        if (startGameTime >= 0) nbt.putLong("start_time", startGameTime);
        nbt.putLong("uptime", uptimeTicks);
        nbt.putLong("mined_resources", minedResources);
        nbt.putLong("consumed_resources", consumedResources);
        var remainingTag = new CompoundTag();
        for (var e : remainingResources.object2LongEntrySet()) {
            remainingTag.putLong(Objects.requireNonNull(ForgeRegistries.BLOCKS.getKey(e.getKey())).toString(),
                    e.getLongValue());
        }
        nbt.put("remaining_resources", remainingTag);
        var minedItemsTag = new CompoundTag();
        for (var e : minedItems.object2LongEntrySet()) {
            minedItemsTag.putLong(Objects.requireNonNull(ForgeRegistries.ITEMS.getKey(e.getKey())).toString(),
                    e.getLongValue());
        }
        nbt.put("mined_items", minedItemsTag);
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
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
            var blockId = ResourceLocation.tryParse(blockIdString);
            if (blockId == null || !ForgeRegistries.BLOCKS.containsKey(blockId)) continue;
            var block = ForgeRegistries.BLOCKS.getValue(blockId);
            remainingResources.put(block, remainingTag.getLong(blockIdString));
        }

        minedItems.clear();
        var minedItemsTag = tag.getCompound("mined_items");
        for (var itemIdString : minedItemsTag.getAllKeys()) {
            var itemId = ResourceLocation.tryParse(itemIdString);
            if (itemId == null || !ForgeRegistries.ITEMS.containsKey(itemId)) continue;
            var item = ForgeRegistries.ITEMS.getValue(itemId);
            minedItems.put(item, minedItemsTag.getLong(itemIdString));
        }
    }
}
