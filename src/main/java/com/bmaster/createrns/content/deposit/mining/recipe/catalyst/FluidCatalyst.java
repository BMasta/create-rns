package com.bmaster.createrns.content.deposit.mining.recipe.catalyst;

import com.simibubi.create.content.contraptions.bearing.BearingContraption;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.MutableComponent;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class FluidCatalyst extends Catalyst {
    public static @Nullable FluidCatalyst fromContraption(BearingContraption contraption) {
        var tank = contraption.getStorage().getFluids();
        if (tank.getTanks() == 0) return null;
        return new FluidCatalyst(tank);
    }

    public IFluidHandler tank;

    public FluidCatalyst(IFluidHandler tank) {
        this.tank = tank;
    }
}
