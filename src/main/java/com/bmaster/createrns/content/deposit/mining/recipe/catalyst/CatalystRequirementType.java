package com.bmaster.createrns.content.deposit.mining.recipe.catalyst;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class CatalystRequirementType<T extends CatalystRequirement, R extends CatalystReservation> {
    protected static final Map<String, CatalystRequirementType<? extends CatalystRequirement, ? extends CatalystReservation>> BY_NAME = new LinkedHashMap<>();

    public static final CatalystRequirementType<FluidCatalystRequirement, FluidCatalystReservation> FLUID =
            register("fluid", FluidCatalystRequirement.MAP_CODEC, FluidCatalystReservation::new);
    public static final CatalystRequirementType<AttachmentCatalystRequirement, AttachmentCatalystReservation> ATTACHMENT =
            register("attachment", AttachmentCatalystRequirement.MAP_CODEC, AttachmentCatalystReservation::new);

    public static final Codec<CatalystRequirementType<?, ?>> CODEC = Codec.STRING.comapFlatMap(
            CatalystRequirementType::getByName,
            CatalystRequirementType::name
    );

    private final String name;
    private final MapCodec<T> mapCodec;
    private final Supplier<R> reservationFactory;

    private CatalystRequirementType(String name, MapCodec<T> mapCodec, Supplier<R> reservationFactory) {
        this.name = name;
        this.mapCodec = mapCodec;
        this.reservationFactory = reservationFactory;
    }

    public String name() {
        return name;
    }

    public MapCodec<T> mapCodec() {
        return mapCodec;
    }

    public R createReservation() {
        return reservationFactory.get();
    }

    private static DataResult<CatalystRequirementType<?, ?>> getByName(String name) {
        var type = BY_NAME.get(name);
        if (type != null) return DataResult.success(type);
        return DataResult.error(() -> "Unknown catalyst requirement type: " + name);
    }

    private static <T extends CatalystRequirement, R extends CatalystReservation> CatalystRequirementType<T, R>
    register(String name, MapCodec<T> mapCodec, Supplier<R> reservationFactory) {
        var type = new CatalystRequirementType<>(name, mapCodec, reservationFactory);
        var previous = BY_NAME.putIfAbsent(name, type);
        if (previous != null) throw new IllegalStateException("Duplicate catalyst requirement type: " + name);
        return type;
    }
}
