package com.bmaster.createrns.content.deposit.mining.recipe.catalyst;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.neoforged.neoforge.fluids.FluidStack;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collection;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class FluidCatalystRequirement extends CatalystRequirement {
    public static final MapCodec<FluidCatalystRequirement> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                    FluidStack.CODEC.fieldOf("consume")
                            .forGetter(c -> c.fluidStack))
            .apply(i, FluidCatalystRequirement::new));

    protected static final Set<Class<? extends Catalyst>> RELEVANT_CATALYST_TYPES = Set.of(FluidCatalyst.class);

    public final FluidStack fluidStack;

    public FluidCatalystRequirement(FluidStack fluidStack) {
        this.fluidStack = fluidStack;
    }

    @Override
    public CatalystRequirementType<FluidCatalystRequirement, FluidCatalystReservation> type() {
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
    public boolean reserve(Collection<Catalyst> catalysts, CatalystReservations reservations) {
        var res = reservations.getForType(type());
        var reservedStack = res.getMatchingFluidStack(fluidStack).orElse(null);
        var reservedAmount = reservedStack != null ? reservedStack.getAmount() : 0;

        if (FluidCatalystReservation.drain(catalysts, fluidStack.copyWithAmount(fluidStack.getAmount() + reservedAmount), true)) {
            if (reservedStack == null) res.addFluidStack(fluidStack.copy());
            else reservedStack.grow(fluidStack.getAmount());
            return true;
        }
        return false;
    }
}
