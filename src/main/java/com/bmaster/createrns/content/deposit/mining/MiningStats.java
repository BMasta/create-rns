package com.bmaster.createrns.content.deposit.mining;

import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraft.world.level.block.Block;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class MiningStats implements INBTSerializable<CompoundTag> {
    public static final int UPTIME_GRACE_PERIOD_TICKS = 40;

    public long minedResources = 0;
    public long consumedResources = 0;
    public final Object2LongOpenHashMap<Block> remainingResources = new Object2LongOpenHashMap<>();

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
    }

    public long getTotalRemainingResources() {
        long total = 0;
        for (var v : remainingResources.values()) {
            if (v <= 0) continue;
            total += v;
        }
        return total;
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
            remainingTag.putLong(BuiltInRegistries.BLOCK.getKey(e.getKey()).toString(), e.getLongValue());
        }
        nbt.put("remaining_resources", remainingTag);
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
            if (blockId == null) continue;
            var block = BuiltInRegistries.BLOCK.getOptional(blockId).orElse(null);
            if (block == null) continue;
            remainingResources.put(block, remainingTag.getLong(blockIdString));
        }
    }
}
