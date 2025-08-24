package com.bmaster.createrns.capability.depositindex;

import com.bmaster.createrns.AllContent;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DepositIndexProvider implements ICapabilitySerializable<CompoundTag> {
    private final DepositIndex data = new DepositIndex();
    private final LazyOptional<IDepositIndex> opt;

    public DepositIndexProvider() {
        this.opt = LazyOptional.of(() -> data);
    }

    @Override
    public CompoundTag serializeNBT() {
        return data.serializeNBT();
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
            data.deserializeNBT(tag);
    }

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return AllContent.DEPOSIT_INDEX.orEmpty(cap, opt);
    }
}
