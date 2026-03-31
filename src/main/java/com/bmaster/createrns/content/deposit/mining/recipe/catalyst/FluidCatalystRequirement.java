package com.bmaster.createrns.content.deposit.mining.recipe.catalyst;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

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

    public static final StreamCodec<RegistryFriendlyByteBuf, FluidCatalystRequirement> STREAM_CODEC = StreamCodec.composite(
            FluidStack.STREAM_CODEC, cr -> cr.fluidStack,
            FluidCatalystRequirement::new
    );

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
        var fluidToDrain = fluidStack.getAmount();
        for (var c : catalysts) {
            if (!(c instanceof FluidCatalyst fluidCat)) continue;
            fluidToDrain -= fluidCat.tank.drain(fluidToDrain, IFluidHandler.FluidAction.SIMULATE).getAmount();
            if (fluidToDrain <= 0) break;
        }
        if (fluidToDrain > 0) return false;
        if (simulate) return true;

        // Drain
        fluidToDrain = fluidStack.getAmount();
        for (var c : catalysts) {
            if (!(c instanceof FluidCatalyst fluidCat)) continue;
            fluidToDrain -= fluidCat.tank.drain(fluidToDrain, IFluidHandler.FluidAction.EXECUTE).getAmount();
            if (fluidToDrain <= 0) break;
        }
        return true;
    }
}
