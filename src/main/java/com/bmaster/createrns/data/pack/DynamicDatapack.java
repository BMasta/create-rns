package com.bmaster.createrns.data.pack;

import com.bmaster.createrns.CreateRNS;
import com.google.gson.JsonElement;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.BuiltInPackSource;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class DynamicDatapack {
    public static List<Pack> DATAPACKS = new ArrayList<>();
    public static List<Pack> RESOURCE_PACKS = new ArrayList<>();

    public static DynamicDatapack createDatapack(String id) {
        return new DynamicDatapack(CreateRNS.asResource(id), PackType.SERVER_DATA);
    }

    public static DynamicDatapack createDatapack(ResourceLocation id) {
        return new DynamicDatapack(id, PackType.SERVER_DATA);
    }

    public static DynamicDatapack createResourcePack(String id) {
        return new DynamicDatapack(CreateRNS.asResource(id), PackType.CLIENT_RESOURCES);
    }

    public static DynamicDatapack createResourcePack(ResourceLocation id) {
        return new DynamicDatapack(id, PackType.CLIENT_RESOURCES);
    }

    private final ResourceLocation id;
    private final PackType type;

    private Component title = Component.empty();
    private boolean isRequired = true;
    private PackSource source = PackSource.BUILT_IN;
    private Pack.Position pos = Pack.Position.BOTTOM;
    private final List<DatapackFile> files = new ArrayList<>();

    public DynamicDatapack title(Component title) {
        this.title = title;
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

    public DynamicDatapack addFile(DatapackFile file) {
        files.add(file);
        return this;
    }

    public DynamicDatapack addContent(List<DatapackFile> files) {
        this.files.addAll(files);
        return this;
    }

    public Pack build() {
        var resources = new DynamicDatapackResources(new PackLocationInfo(
                id.toString(), title, source, Optional.empty()));

        for (var file : files) {
            resources.putJson(file.path, file.data);
        }

        var pack = Pack.readMetaAndCreate(resources.location(), BuiltInPackSource.fixedResources(resources),
                type, new PackSelectionConfig(isRequired, pos, false));
        assert pack != null;

        return pack;
    }

    public Pack buildAndRegister() {
        var pack = build();
        if (type == PackType.SERVER_DATA) DATAPACKS.add(build());
        if (type == PackType.CLIENT_RESOURCES) RESOURCE_PACKS.add(build());
        return pack;
    }

    private DynamicDatapack(ResourceLocation id, PackType type) {
        this.id = id;
        this.type = type;
    }

    public record DatapackFile(String path, JsonElement data) {}
}
