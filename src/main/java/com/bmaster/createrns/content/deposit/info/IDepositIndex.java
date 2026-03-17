package com.bmaster.createrns.content.deposit.info;

import com.bmaster.createrns.RNSMisc;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.common.util.INBTSerializable;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public interface IDepositIndex extends INBTSerializable<CompoundTag> {
    boolean addCustomDeposit(CustomDepositLocation dep);

    boolean removeCustomDeposit(CustomDepositLocation dep);

    static DepositIndex get(ServerLevel level) {
        var cap = level.getCapability(RNSMisc.DEPOSIT_INDEX).resolve().orElse(null);
        return (DepositIndex) cap;
    }
}
