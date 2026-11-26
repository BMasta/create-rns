package com.bmaster.createrns.content.deposit.mining.recipe.catalyst;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.util.FluidAndComponents;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.*;

public class FluidCatalystReservation extends CatalystReservation {
    public static boolean drain(Collection<Catalyst> catalysts, FluidStack stackToDrain, boolean simulate) {
        // Simulate
        var fluid = stackToDrain.getFluid();
        if (fluid == Fluids.EMPTY) return false;
        var amountToDrain = stackToDrain.getAmount();
        for (var c : catalysts) {
            if (!(c instanceof FluidCatalyst fluidCat)) continue;
            amountToDrain -= fluidCat.tank.drain(stackToDrain.copyWithAmount(amountToDrain), IFluidHandler.FluidAction.SIMULATE)
                    .getAmount();
            if (amountToDrain <= 0) break;
        }
        if (amountToDrain > 0) return false;
        if (simulate) return true;

        // Drain
        amountToDrain = stackToDrain.getAmount();
        for (var c : catalysts) {
            if (!(c instanceof FluidCatalyst fluidCat)) continue;
            amountToDrain -= fluidCat.tank.drain(stackToDrain.copyWithAmount(amountToDrain), IFluidHandler.FluidAction.EXECUTE)
                    .getAmount();
            if (amountToDrain <= 0) break;
        }
        return amountToDrain == 0;
    }

    private final HashMap<FluidAndComponents, FluidStack> fluidStacks;

    public FluidCatalystReservation() {
        fluidStacks = new HashMap<>();
    }

    private FluidCatalystReservation(HashMap<FluidAndComponents, FluidStack> fluidStacks) {
        HashMap<FluidAndComponents, FluidStack> fsTmp = HashMap.newHashMap(fluidStacks.size());
        for (var e : fluidStacks.entrySet()) {
            fsTmp.put(e.getKey(), e.getValue().copy());
        }
        this.fluidStacks = fsTmp;
    }

    public void addFluidStack(FluidStack stack) {
        var key = FluidAndComponents.of(stack);
        if (fluidStacks.containsKey(key)) throw new IllegalStateException("Fluid stack is already added");
        fluidStacks.put(key, stack);
    }

    public Optional<FluidStack> getMatchingFluidStack(FluidStack stack) {
        return Optional.ofNullable(fluidStacks.get(FluidAndComponents.of(stack)));
    }

    @Override
    public FluidCatalystReservation copy() {
        return new FluidCatalystReservation(fluidStacks);
    }

    @Override
    public boolean consume(Collection<Catalyst> catalysts) {
        for (var simulate : List.of(true, false)) {
            for (var fs : fluidStacks.values()) {
                if (fs == null || fs.getAmount() == 0) continue;
                if (!drain(catalysts, fs, simulate)) {
                    CreateRNS.LOGGER.error("Failed to consume {} {} reserved for catalysts", fs.getAmount(), fs.getFluid());
                    return false;
                }
            }
        }
        return true;
    }
}
