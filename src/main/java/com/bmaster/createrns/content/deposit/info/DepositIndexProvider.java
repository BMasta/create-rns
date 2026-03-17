package com.bmaster.createrns.content.deposit.info;

import com.bmaster.createrns.RNSMisc;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class DepositIndexProvider implements ICapabilitySerializable<CompoundTag> {
    private final DepositIndex data;
    private final LazyOptional<IDepositIndex> opt;

    public DepositIndexProvider(ServerLevel sl) {
        data = new DepositIndex(sl);
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

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        return RNSMisc.DEPOSIT_INDEX.orEmpty(cap, opt);
    }
}
