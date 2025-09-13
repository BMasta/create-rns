package com.bmaster.createrns.data.pack;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static net.minecraft.util.datafix.fixes.BlockEntitySignTextStrictJsonFix.GSON;

public final class DynamicDatapackResources implements PackResources {
    private final String packId;

    private final Map<ResourceLocation, byte[]> serverData = new Object2ObjectOpenHashMap<>();
    private final PackMetadataSection metadata;

    public DynamicDatapackResources(String packId) {
        this.packId = packId;
        int packFormat = SharedConstants.getCurrentVersion().getPackVersion(PackType.SERVER_DATA);
        this.metadata = new PackMetadataSection(Component.literal(packId), packFormat);
    }

    public void putJson(String path, JsonElement json) {
        serverData.put(loc(path), GSON.toJson(json).getBytes(StandardCharsets.UTF_8));
    }

    private static ResourceLocation loc(String path) {
        // path must be "namespace/..." with NO "data/" prefix.
        int idx = path.indexOf('/');
        String ns = path.substring(0, idx);
        String p = path.substring(idx + 1);
        return ResourceLocation.fromNamespaceAndPath(ns, p);
    }

    @Override
    public @NotNull String packId() {
        return packId;
    }

    @ParametersAreNonnullByDefault
    @Override
    public @NotNull Set<String> getNamespaces(PackType type) {
        if (type != PackType.SERVER_DATA) return Set.of();
        return serverData.keySet().stream().map(ResourceLocation::getNamespace).collect(Collectors.toSet());
    }

    @ParametersAreNonnullByDefault
    @Override
    public @Nullable IoSupplier<InputStream> getResource(PackType type, ResourceLocation rl) {
        if (type != PackType.SERVER_DATA) return null;
        byte[] b = serverData.get(rl);
        return (b == null) ? null : () -> new ByteArrayInputStream(b);
    }

    @ParametersAreNonnullByDefault
    @Override
    public void listResources(PackType type, String ns, String path, PackResources.ResourceOutput out) {
        if (type != PackType.SERVER_DATA) return;
        var pathWithSlash = path.endsWith("/") ? path : (path + "/");

        for (var e : serverData.entrySet()) {
            var rl = e.getKey();
            if (!rl.getNamespace().equals(ns)) continue;
            if (rl.getPath().startsWith(pathWithSlash)) {
                out.accept(e.getKey(), () -> new ByteArrayInputStream(e.getValue()));
            }
        }
    }

    @ParametersAreNonnullByDefault
    @Override
    public @Nullable IoSupplier<InputStream> getRootResource(String... elements) {
        // Minecraft asks with elements = {"pack.mcmeta"}
        if (elements.length == 1 && "pack.mcmeta".equals(elements[0])) {
            byte[] bytes = buildPackMcMetaBytes();
            return () -> new ByteArrayInputStream(bytes);
        }
        return null; // no pack.png, etc.
    }

    @ParametersAreNonnullByDefault
    @Override
    public @Nullable <T> T getMetadataSection(MetadataSectionSerializer<T> deserializer) throws IOException {
        if (deserializer == PackMetadataSection.TYPE) {
            @SuppressWarnings("unchecked")
            T t = (T) metadata;
            return t;
        }
        return null;
    }

    private byte[] buildPackMcMetaBytes() {
        // This mirrors the structure of a normal pack.mcmeta:
        JsonObject root = new JsonObject();
        JsonObject pack = new JsonObject();
        // Description can be a plain string or a text component; dump as a component JSON for safety.
        pack.add("description", net.minecraft.network.chat.Component.Serializer.toJsonTree(metadata.getDescription()));
        pack.addProperty("pack_format", metadata.getPackFormat(PackType.SERVER_DATA));
        root.add("pack", pack);
        return GSON.toJson(root).getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public boolean isBuiltin() {
        return true;
    }

    @Override
    public void close() {
    }
}
