package com.bmaster.createrns.content.deposit.mining.recipe.catalyst;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.MutableComponent;
import net.minecraftforge.fluids.FluidStack;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class FluidCatalystRequirement extends CatalystRequirement {
    public static final Codec<FluidCatalystRequirement> CODEC = RecordCodecBuilder.create(i -> i.group(
                    FluidStack.CODEC.fieldOf("consume")
                            .forGetter(c -> c.fluidStack),
                    Codec.floatRange(0, Float.MAX_VALUE).fieldOf("chance_multiplier")
                            .orElse(1f)
                            .forGetter(c -> c.chanceMultiplier))
            .apply(i, FluidCatalystRequirement::new));

    public static final Codec<FluidCatalystRequirement> STREAM_CODEC = RecordCodecBuilder.create(i -> i.group(
                    FluidStack.CODEC.fieldOf("fluid_per_operation")
                            .forGetter(c -> c.fluidStack),
                    Codec.FLOAT.fieldOf("chance_multiplier")
                            .orElse(1f)
                            .forGetter(c -> c.chanceMultiplier))
            .apply(i, FluidCatalystRequirement::new));

    FluidStack fluidStack;
    float chanceMultiplier;

    public FluidCatalystRequirement(FluidStack fluidStack, float chanceMultiplier) {
        this.fluidStack = fluidStack;
        this.chanceMultiplier = chanceMultiplier;
    }

    @Override
    public boolean isSatisfiedBy(Catalyst catalyst) {
        return (catalyst instanceof FluidCatalyst);
    }

    @Override
    public float getChanceMult(Catalyst catalyst) {
        return chanceMultiplier;
    }

    @Override
    public float getMaxChance() {
        return chanceMultiplier;
    }

    @Override
    public List<MutableComponent> JEIRequirementDescriptions() {
        return List.of();
    }

    @Override
    public List<MutableComponent> JEIChanceDescriptions(float weightRatio) {
        return List.of();
    }
}
