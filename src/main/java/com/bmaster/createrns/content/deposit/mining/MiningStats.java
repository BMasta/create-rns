package com.bmaster.createrns.content.deposit.mining;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.util.INBTSerializable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class MiningStats implements INBTSerializable<CompoundTag> {
    public long uptimeTicks = 0;
    public long minedResources = 0;
    public long consumedResources = 0;
    public long remainingResources = 0;

    private long startGameTime = -1;

    public long elapsedTicks(Level level) {
        if (level == null || startGameTime < 0) return 0;
        return Math.max(0, level.getGameTime() - startGameTime);
    }

    public boolean isStarted() {
        return startGameTime >= 0;
    }

    public void start(Level level) {
        reset();
        startGameTime = level.getGameTime();
    }

    public void reset() {
        startGameTime = -1;
        uptimeTicks = 0;
        minedResources = 0;
        consumedResources = 0;
        remainingResources = 0;
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        var nbt = new CompoundTag();
        if (startGameTime >= 0) nbt.putLong("start_time", startGameTime);
        nbt.putLong("uptime", uptimeTicks);
        nbt.putLong("mined_resources", minedResources);
        nbt.putLong("consumed_resources", consumedResources);
        nbt.putLong("remaining_resources", remainingResources);
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
        remainingResources = tag.getLong("remaining_resources");
    }
}
