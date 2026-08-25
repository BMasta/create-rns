package com.bmaster.createrns.content.deposit.mining.recipe.catalyst;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraft.world.level.material.Fluids;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collection;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class FluidCatalystRequirement extends CatalystRequirement {
    public static final Codec<FluidCatalystRequirement> CODEC = RecordCodecBuilder.create(i -> i.group(
                    FluidStack.CODEC.fieldOf("consume")
                            .forGetter(c -> c.fluidStack))
            .apply(i, FluidCatalystRequirement::new));

    public static final Codec<FluidCatalystRequirement> STREAM_CODEC = RecordCodecBuilder.create(i -> i.group(
                    FluidStack.CODEC.fieldOf("consume")
                            .forGetter(c -> c.fluidStack))
            .apply(i, FluidCatalystRequirement::new));

    protected static final Set<Class<? extends Catalyst>> RELEVANT_CATALYST_TYPES = Set.of(FluidCatalyst.class);

    public final FluidStack fluidStack;

    public FluidCatalystRequirement(FluidStack fluidStack) {
        this.fluidStack = fluidStack;
    }

    @Override
    public CatalystRequirementType<?> type() {
        return CatalystRequirementType.FLUID;
    }

    @Override
    public Set<Class<? extends Catalyst>> relevantCatalystTypes() {
        return RELEVANT_CATALYST_TYPES;
    }

    @Override
    public boolean isSatisfiedBy(Collection<Catalyst> catalysts) {
        for (var c : catalysts) {
            if (c instanceof FluidCatalyst) return true;
        }
        return false;
    }

    @Override
    public boolean useCatalysts(Collection<Catalyst> catalysts, boolean simulate) {
        // Simulate
        var fluid = fluidStack.getFluid();
        if (fluid == Fluids.EMPTY) return false;
        var amountToDrain = fluidStack.getAmount();
        for (var c : catalysts) {
            if (!(c instanceof FluidCatalyst fluidCat)) continue;
            amountToDrain -= fluidCat.tank.drain(new FluidStack(fluidStack, amountToDrain), IFluidHandler.FluidAction.SIMULATE)
                    .getAmount();
            if (amountToDrain <= 0) break;
        }
        if (amountToDrain > 0) return false;
        if (simulate) return true;

        // Drain
        amountToDrain = fluidStack.getAmount();
        for (var c : catalysts) {
            if (!(c instanceof FluidCatalyst fluidCat)) continue;
            amountToDrain -= fluidCat.tank.drain(new FluidStack(fluidStack, amountToDrain), IFluidHandler.FluidAction.EXECUTE)
                    .getAmount();
            if (amountToDrain <= 0) break;
        }
        return true;
    }
}
