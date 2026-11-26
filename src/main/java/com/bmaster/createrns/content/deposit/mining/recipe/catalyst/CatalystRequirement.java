package com.bmaster.createrns.content.deposit.mining.recipe.catalyst;

import com.mojang.serialization.Codec;
import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collection;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class CatalystRequirement {
    public static final Codec<CatalystRequirement> CODEC = CatalystRequirementType.CODEC.dispatch(
            "type",
            CatalystRequirement::type,
            CatalystRequirementType::mapCodec
    );

    public abstract CatalystRequirementType<? extends CatalystRequirement, ? extends CatalystReservation> type();

    public abstract Set<Class<? extends Catalyst>> relevantCatalystTypes();

    public abstract boolean isSatisfiedBy(Collection<Catalyst> catalysts);

    public abstract boolean reserve(Collection<Catalyst> catalysts, CatalystReservations reservations);
}
