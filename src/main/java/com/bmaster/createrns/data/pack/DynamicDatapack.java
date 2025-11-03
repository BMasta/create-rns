package com.bmaster.createrns.data.pack;

import com.bmaster.createrns.CreateRNS;
import com.google.gson.JsonElement;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;

import java.util.*;

public class DynamicDatapack {

    public static DynamicDatapack createDatapack(String id) {
        return new DynamicDatapack(ResourceLocation.fromNamespaceAndPath(CreateRNS.MOD_ID, id), PackType.SERVER_DATA);
    }

    public static DynamicDatapack createDatapack(ResourceLocation id) {
        return new DynamicDatapack(id, PackType.SERVER_DATA);
    }

    public static DynamicDatapack createResourcePack(String id) {
        return new DynamicDatapack(ResourceLocation.fromNamespaceAndPath(CreateRNS.MOD_ID, id), PackType.CLIENT_RESOURCES);
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
        var resources = new DynamicDatapackResources(id.toString());

        for (var file : files) {
            resources.putJson(file.path, file.data);
        }

        return Pack.readMetaAndCreate(id.toString(), title, isRequired, (id) -> resources, type, pos, source);
    }

    private DynamicDatapack(ResourceLocation id, PackType type) {
        this.id = id;
        this.type = type;
    }

    public record DatapackFile(String path, JsonElement data) {}
}
