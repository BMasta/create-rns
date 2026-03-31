package com.bmaster.createrns.testutil;

import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;

public class TestPlatformObjects {
    public static FluidStack fluidStack(Fluid fluid, int amount) {
        return new FluidStack(fluid, amount);
    }
}
