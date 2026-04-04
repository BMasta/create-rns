package com.bmaster.createrns.content.deposit.mining.recipe.catalyst;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.LinkedHashMap;
import java.util.Map;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class CatalystRequirementType<T extends CatalystRequirement> {
    private static final Map<String, CatalystRequirementType<?>> BY_NAME = new LinkedHashMap<>();

    public static final CatalystRequirementType<FluidCatalystRequirement> FLUID =
            register("fluid", FluidCatalystRequirement.CODEC, FluidCatalystRequirement.STREAM_CODEC);
    public static final CatalystRequirementType<AttachmentCatalystRequirement> ATTACHMENT =
            register("attachment", AttachmentCatalystRequirement.CODEC, AttachmentCatalystRequirement.STREAM_CODEC);

    public static final Codec<CatalystRequirementType<?>> CODEC = Codec.STRING.comapFlatMap(
            CatalystRequirementType::getByName,
            CatalystRequirementType::name
    );
    public static final Codec<CatalystRequirementType<?>> STREAM_CODEC = Codec.STRING.comapFlatMap(
            CatalystRequirementType::getByName,
            CatalystRequirementType::name
    );

    private final String name;
    private final Codec<T> codec;
    private final Codec<T> streamCodec;

    private CatalystRequirementType(String name, Codec<T> codec, Codec<T> streamCodec) {
        this.name = name;
        this.codec = codec;
        this.streamCodec = streamCodec;
    }

    public String name() {
        return name;
    }

    public Codec<T> codec() {
        return codec;
    }

    public Codec<T> streamCodec() {
        return streamCodec;
    }

    private static DataResult<CatalystRequirementType<?>> getByName(String name) {
        var type = BY_NAME.get(name);
        if (type != null) return DataResult.success(type);
        return DataResult.error(() -> "Unknown catalyst requirement type: " + name);
    }

    private static <T extends CatalystRequirement> CatalystRequirementType<T> register(
            String name, Codec<T> codec, Codec<T> streamCodec
    ) {
        var type = new CatalystRequirementType<>(name, codec, streamCodec);
        var previous = BY_NAME.putIfAbsent(name, type);
        if (previous != null) throw new IllegalStateException("Duplicate catalyst requirement type: " + name);
        return type;
    }
}
