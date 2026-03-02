package com.bmaster.createrns.content.deposit.mining.recipe.catalyst;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraftforge.fluids.capability.IFluidHandler;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class FluidCatalyst extends Catalyst {
    public IFluidHandler tank;

    public FluidCatalyst(IFluidHandler tank) {
        this.tank = tank;
    }

    @Override
    public boolean use(CatalystRequirement requirement, boolean simulate) {
        if (!(requirement instanceof FluidCatalystRequirement fluidCR)) return false;
        var fluidToDrain = fluidCR.fluidStack.copy();
        if (tank.drain(fluidToDrain, IFluidHandler.FluidAction.SIMULATE).getAmount() == fluidToDrain.getAmount()) {
            tank.drain(fluidToDrain, IFluidHandler.FluidAction.EXECUTE);
            return true;
        }
        return false;
    }
}
