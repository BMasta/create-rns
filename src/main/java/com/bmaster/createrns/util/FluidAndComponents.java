package com.bmaster.createrns.util;

import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.Objects;

public record FluidAndComponents(Fluid fluid, PatchedDataComponentMap components) {

    public static FluidAndComponents of(FluidStack stack) {
        return new FluidAndComponents(stack.getFluid(), stack.getComponents());
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof FluidAndComponents other)) return false;
        return fluid.equals(other.fluid) && Objects.equals(components, other.components);
    }

    @Override
    public int hashCode() {
        int i = 31 + fluid.hashCode();
        return 31 * i + components.hashCode();
    }
}
