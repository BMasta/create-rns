package com.bmaster.createrns.content.deposit.mining.recipe.catalyst;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class FluidCatalystRequirement extends CatalystRequirement {
    public static final Codec<FluidCatalystRequirement> CODEC = RecordCodecBuilder.create(i -> i.group(
                    FluidStack.CODEC.fieldOf("consume")
                            .forGetter(c -> c.fluidStack))
            .apply(i, FluidCatalystRequirement::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, FluidCatalystRequirement> STREAM_CODEC = StreamCodec.composite(
            FluidStack.STREAM_CODEC, cr -> cr.fluidStack,
            FluidCatalystRequirement::new
    );

    FluidStack fluidStack;

    public FluidCatalystRequirement(FluidStack fluidStack) {
        this.fluidStack = fluidStack;
    }

    @Override
    public boolean isSatisfiedBy(Catalyst catalyst) {
        return (catalyst instanceof FluidCatalyst);
    }

    @Override
    public boolean useCatalyst(Catalyst catalyst, boolean simulate) {
        if (!(catalyst instanceof FluidCatalyst fluidCat)) return false;
        var fluidToDrain = fluidStack.copy();
        if (fluidCat.tank.drain(fluidToDrain, IFluidHandler.FluidAction.SIMULATE).getAmount() == fluidToDrain.getAmount()) {
            if (!simulate) fluidCat.tank.drain(fluidToDrain, IFluidHandler.FluidAction.EXECUTE);
            return true;
        }
        return false;
    }
}
