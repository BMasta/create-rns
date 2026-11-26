package com.bmaster.createrns.util.codec;

import com.mojang.logging.annotations.MethodsReturnNonnullByDefault;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record ResourceOrTagLocation(ResourceLocation id, boolean isTag) implements Comparable<ResourceOrTagLocation> {
    public static final Codec<ResourceOrTagLocation> CODEC = Codec.STRING.comapFlatMap(
            ResourceOrTagLocation::decode,
            ResourceOrTagLocation::encode
    );

    public static final StreamCodec<ByteBuf, ResourceOrTagLocation> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, ResourceOrTagLocation::id,
            ByteBufCodecs.BOOL, ResourceOrTagLocation::isTag,
            ResourceOrTagLocation::new
    );

    public static ResourceOrTagLocation ofResource(ResourceLocation id) {
        return new ResourceOrTagLocation(id, false);
    }

    public static ResourceOrTagLocation ofTag(ResourceLocation id) {
        return new ResourceOrTagLocation(id, true);
    }

    public static ResourceOrTagLocation withDefaultNamespace(String path, boolean isTag) {
        return new ResourceOrTagLocation(ResourceLocation.withDefaultNamespace(path), isTag);
    }

    public static ResourceOrTagLocation fromNamespaceAndPath(String namespace, String path, boolean isTag) {
        return new ResourceOrTagLocation(ResourceLocation.fromNamespaceAndPath(namespace, path), isTag);
    }

    public static ResourceOrTagLocation parse(String location) {
        boolean isTag = location.startsWith("#");
        return new ResourceOrTagLocation(ResourceLocation.parse(isTag ? location.substring(1) : location), isTag);
    }

    public static @Nullable ResourceOrTagLocation tryParse(String location) {
        boolean isTag = location.startsWith("#");
        var rl = ResourceLocation.tryParse(isTag ? location.substring(1) : location);
        return rl != null ? new ResourceOrTagLocation(rl, isTag) : null;
    }

    public String getPath() {
        return this.id.getPath();
    }

    public String getNamespace() {
        return this.id.getNamespace();
    }

    @Override
    public String toString() {
        return (isTag ? "#" : "") + id.toString();
    }

    @Override
    public int compareTo(ResourceOrTagLocation other) {
        if (isTag != other.isTag) return Boolean.compare(isTag, other.isTag);
        return id.compareTo(other.id);
    }

    public String encode() {
        return toString();
    }

    public static DataResult<ResourceOrTagLocation> decode(String location) {
        boolean isTag = location.startsWith("#");
        return ResourceLocation.read(isTag ? location.substring(1) : location)
                .map(id -> new ResourceOrTagLocation(id, isTag));
    }
}
