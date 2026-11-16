package com.bmaster.createrns.data;

import com.bmaster.createrns.CreateRNS;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

/** IDs needed by standalone data generation without initializing Minecraft's registries. */
public final class DumpSafeOverrides {
    public static final ResourceLocation OVERWORLD = ResourceLocation.withDefaultNamespace("overworld");
    public static final ResourceLocation NETHER = ResourceLocation.withDefaultNamespace("the_nether");
    public static final ResourceLocation END = ResourceLocation.withDefaultNamespace("the_end");

    public static final ResourceLocation BIOME = ResourceLocation.withDefaultNamespace("worldgen/biome");
    public static final ResourceLocation PROCESSOR_LIST = ResourceLocation.withDefaultNamespace("worldgen/processor_list");
    public static final ResourceLocation STRUCTURE = ResourceLocation.withDefaultNamespace("worldgen/structure");
    public static final ResourceLocation STRUCTURE_SET = ResourceLocation.withDefaultNamespace("worldgen/structure_set");
    public static final ResourceLocation DEPOSIT_SPEC = ResourceLocation.fromNamespaceAndPath(CreateRNS.ID, "deposit_spec");

    public static final ResourceLocation BLOCK = ResourceLocation.withDefaultNamespace("block");
    public static final ResourceLocation ENTITY_TYPE = ResourceLocation.withDefaultNamespace("entity_type");
    public static final ResourceLocation FLUID = ResourceLocation.withDefaultNamespace("fluid");
    public static final ResourceLocation GAME_EVENT = ResourceLocation.withDefaultNamespace("game_event");
    public static final ResourceLocation ITEM = ResourceLocation.withDefaultNamespace("item");

    // Minecraft 1.20 uses plural directories for these tags; other registries use their registry path.
    public static final Map<ResourceLocation, String> TAG_DIRECTORIES = Map.of(
            BLOCK, "tags/blocks", ENTITY_TYPE, "tags/entity_types", FLUID, "tags/fluids",
            GAME_EVENT, "tags/game_events", ITEM, "tags/items");

    private DumpSafeOverrides() {}
}
