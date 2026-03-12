package com.bmaster.createrns.data.pack;

import com.bmaster.createrns.CreateRNS;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class DynamicDatapack {
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    public static List<Pack> DATAPACKS = new ArrayList<>();
    public static List<Pack> RESOURCE_PACKS = new ArrayList<>();
    private static final List<BuiltDynamicPack> PACK_SNAPSHOTS = new ArrayList<>();

    public static DynamicDatapack createDatapack(String id) {
        return new DynamicDatapack(ResourceLocation.fromNamespaceAndPath(CreateRNS.ID, id), PackType.SERVER_DATA);
    }

    public static DynamicDatapack createDatapack(ResourceLocation id) {
        return new DynamicDatapack(id, PackType.SERVER_DATA);
    }

    public static DynamicDatapack createResourcePack(String id) {
        return new DynamicDatapack(ResourceLocation.fromNamespaceAndPath(CreateRNS.ID, id), PackType.CLIENT_RESOURCES);
    }

    public static DynamicDatapack createResourcePack(ResourceLocation id) {
        return new DynamicDatapack(id, PackType.CLIENT_RESOURCES);
    }

    public static List<BuiltDynamicPack> getPackSnapshots() {
        return Collections.unmodifiableList(PACK_SNAPSHOTS);
    }

    public static void dumpRegisteredPacks(Path root) throws IOException {
        Files.createDirectories(root);

        for (var def : PACK_SNAPSHOTS) {
            Path packDir = root.resolve(def.folderName());
            Files.createDirectories(packDir);

            var metadata = new JsonObject();
            var metadataPack = new JsonObject();
            metadataPack.addProperty("description", def.title);
            metadataPack.addProperty("pack_format", SharedConstants.getCurrentVersion().getPackVersion(def.type));
            metadata.add("pack", metadataPack);
            Files.writeString(packDir.resolve("pack.mcmeta"), GSON.toJson(metadata), StandardCharsets.UTF_8);

            for (var f : def.files) {
                Path target = packDir.resolve(toPackRootPath(def.type, f.path));
                var parent = target.getParent();
                if (parent != null) Files.createDirectories(parent);
                Files.writeString(target, GSON.toJson(f.data), StandardCharsets.UTF_8);
            }
        }
    }

    private static Path toPackRootPath(PackType type, String path) {
        int sep = path.indexOf('/');
        if (sep <= 0 || sep >= path.length() - 1) {
            throw new IllegalArgumentException(
                    "Dynamic datapack file path must be in the format 'namespace/path': " + path);
        }

        String namespace = path.substring(0, sep);
        String relativePath = path.substring(sep + 1);
        String rootFolder = (type == PackType.SERVER_DATA) ? "data" : "assets";
        return Path.of(rootFolder).resolve(namespace).resolve(relativePath);
    }

    private final ResourceLocation id;
    private final PackType type;
    private Component title;

    private Component description = Component.empty();
    private boolean isRequired = true;
    private PackSource source = PackSource.BUILT_IN;
    private Pack.Position pos = Pack.Position.BOTTOM;
    private final List<DatapackFile> files = new ArrayList<>();

    public DynamicDatapack title(Component title) {
        this.title = title;
        return this;
    }

    public DynamicDatapack description(Component description) {
        this.description = description;
        return this;
    }

    public DynamicDatapack optional() {
        isRequired = false;
        return this;
    }

    public DynamicDatapack source(PackSource source) {
        this.source = source;
        return this;
    }

    public DynamicDatapack overwritesLoadedPacks() {
        pos = Pack.Position.TOP;
        return this;
    }

    public DynamicDatapack addContent(List<DatapackFile> files) {
        this.files.addAll(files);
        return this;
    }

    public DynamicDatapack addContent(DatapackFile file) {
        this.files.add(file);
        return this;
    }

    public Pack build() {
        var resources = new DynamicDatapackResources(id.toString(), description);

        for (var file : files) {
            resources.putJson(file.path, file.data);
        }

        var pack = Pack.readMetaAndCreate(id.toString(), title, isRequired, (id) -> resources, type, pos, source);
        assert pack != null;

        return pack;
    }

    public Pack buildAndRegister() {
        var pack = build();
        if (type == PackType.SERVER_DATA) DATAPACKS.add(pack);
        if (type == PackType.CLIENT_RESOURCES) RESOURCE_PACKS.add(pack);
        return pack;
    }

    public void registerSnapshots() {
        PACK_SNAPSHOTS.add(snapshot());
    }

    private DynamicDatapack(ResourceLocation id, PackType type) {
        this.id = id;
        this.type = type;
        this.title = Component.literal(id.toString());
    }

    private BuiltDynamicPack snapshot() {
        var copiedFiles = files.stream()
                .map(f -> new DatapackFile(f.path, f.data.deepCopy()))
                .toList();

        return new BuiltDynamicPack(id, type, title.getString(), isRequired, pos, copiedFiles);
    }

    public record DatapackFile(String path, JsonElement data) {}

    public record BuiltDynamicPack(
            ResourceLocation id, PackType type, String title, boolean required,
            Pack.Position position, List<DatapackFile> files
    ) {
        public String folderName() {
            return id.getNamespace() + "_" + id.getPath().replace('/', '_');
        }
    }
}
