package com.bmaster.createrns.util.codec;

import com.mojang.serialization.Codec;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class FluidWithFallbacks extends ResourceWithFallbacks<Fluid> {
    public static final Codec<FluidWithFallbacks> LENIENT_CODEC = createCodec(FluidWithFallbacks::new, true);
    public static final Codec<FluidWithFallbacks> STRICT_CODEC = createCodec(FluidWithFallbacks::new, false);
    public static final Codec<FluidWithFallbacks> STRICT_RESOLVABLE_CODEC =
            STRICT_CODEC.validate(ResourceWithFallbacks::validateResolvable);
    public static final StreamCodec<RegistryFriendlyByteBuf, FluidWithFallbacks> STREAM_CODEC =
            createStreamCodec(Registries.FLUID, FluidWithFallbacks::new);

    public static final FluidWithFallbacks EMPTY = new FluidWithFallbacks(List.of(), true);

    protected FluidWithFallbacks(List<ResourceOrTagLocation> originalEntries, boolean encodedAsList) {
        this(Fluids.EMPTY, originalEntries, encodedAsList);
    }

    protected FluidWithFallbacks(
            Fluid value, List<ResourceOrTagLocation> originalEntries, boolean encodedAsList
    ) {
        super(value, originalEntries, encodedAsList);
    }

    @Override
    protected Registry<Fluid> registry() {
        return BuiltInRegistries.FLUID;
    }

    @Override
    protected Fluid emptyFallbackValue() {
        return Fluids.EMPTY;
    }

    @Override
    protected String resourceLogIdentifier() {
        return "fluid";
    }

    @Override
    protected Fluid resolveFromResourceStack(Fluid resource) {
        return new FluidStack(resource, 1).getFluid();
    }
}
