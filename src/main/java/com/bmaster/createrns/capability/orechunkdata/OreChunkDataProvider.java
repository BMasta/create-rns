package com.bmaster.createrns.capability.orechunkdata;

import com.bmaster.createrns.AllContent;
import com.bmaster.createrns.CreateRNS;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class OreChunkDataProvider implements ICapabilitySerializable<CompoundTag> {
    private final OreChunkData data;
    private final LazyOptional<IOreChunkData> opt;
    private final boolean forceRecompute;
    private final LevelChunk chunk;

    public OreChunkDataProvider(LevelChunk chunk, boolean forceRecompute) {
        this.data = OreChunkClassifier.DEFAULT.classify(chunk);
        this.opt = LazyOptional.of(() -> data);
        this.forceRecompute = forceRecompute;
        this.chunk = chunk;
        chunk.setUnsaved(true);
        if (data.isOreChunk()) {
            CreateRNS.LOGGER.info("Ore chunk {}/{} at {},{}", data.getMinedItemStack().getItem(),
                    data.getPurity().name(),chunk.getPos().getBlockX(8), chunk.getPos().getBlockZ(8));
        }
    }

    @Override
    public CompoundTag serializeNBT() {
        return data.serializeNBT();
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        if (!forceRecompute) {
            data.deserializeNBT(tag);
            // If chunk data is extracted from NBT, no reason to save
            chunk.setUnsaved(false);
            if (data.isOreChunk()) {
                CreateRNS.LOGGER.info("[Loaded] Ore chunk {}/{} at {},{}", data.getMinedItemStack().getItem(),
                        data.getPurity().name(), chunk.getPos().getBlockX(8), chunk.getPos().getBlockZ(8));
            }
        }
    }

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (chunk.getLevel().isClientSide()) return LazyOptional.empty();
        return AllContent.ORE_CHUNK_DATA.orEmpty(cap, opt);
    }
}
