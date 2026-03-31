package com.bmaster.createrns.content.deposit.mining.recipe.catalyst;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.LinkedHashMap;
import java.util.Map;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class CatalystRequirementType<T extends CatalystRequirement> {
    private static final Map<String, CatalystRequirementType<?>> BY_NAME = new LinkedHashMap<>();

    public static final CatalystRequirementType<FluidCatalystRequirement> FLUID =
            register("fluid", FluidCatalystRequirement.MAP_CODEC);
    public static final CatalystRequirementType<AttachmentCatalystRequirement> ATTACHMENT =
            register("attachment", AttachmentCatalystRequirement.MAP_CODEC);

    public static final Codec<CatalystRequirementType<?>> CODEC = Codec.STRING.comapFlatMap(
            CatalystRequirementType::getByName,
            CatalystRequirementType::name
    );

    private final String name;
    private final MapCodec<T> mapCodec;

    private CatalystRequirementType(String name, MapCodec<T> mapCodec) {
        this.name = name;
        this.mapCodec = mapCodec;
    }

    public String name() {
        return name;
    }

    public MapCodec<T> mapCodec() {
        return mapCodec;
    }

    private static DataResult<CatalystRequirementType<?>> getByName(String name) {
        var type = BY_NAME.get(name);
        if (type != null) return DataResult.success(type);
        return DataResult.error(() -> "Unknown catalyst requirement type: " + name);
    }

    private static <T extends CatalystRequirement> CatalystRequirementType<T> register(String name, MapCodec<T> mapCodec) {
        var type = new CatalystRequirementType<>(name, mapCodec);
        var previous = BY_NAME.putIfAbsent(name, type);
        if (previous != null) throw new IllegalStateException("Duplicate catalyst requirement type: " + name);
        return type;
    }
}
