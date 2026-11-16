package com.bmaster.createrns.data.deposit;

import com.bmaster.createrns.compat.kubejs.RNSKubeJSPluginBridge;
import com.bmaster.createrns.content.deposit.mining.recipe.DepositDurability;
import com.bmaster.createrns.content.deposit.worldgen.PlacementStrategy;
import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.data.DumpSafeOverrides;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public final class DepositData {

    static final List<BlockEntry> BLOCKS = new ArrayList<>();
    static final List<StructureEntry> STRUCTURES = new ArrayList<>();
    static final List<RecipeEntry> RECIPES = new ArrayList<>();

    public static final ResourceLocation PROCESSOR_INPUT_BLOCK = ResourceLocation.withDefaultNamespace("end_stone");

    public static final ResourceLocation TEMPLATE_SMALL = ResourceLocation.fromNamespaceAndPath(CreateRNS.ID, "ore_deposit_small");
    public static final ResourceLocation TEMPLATE_MEDIUM = ResourceLocation.fromNamespaceAndPath(CreateRNS.ID, "ore_deposit_medium");
    public static final ResourceLocation TEMPLATE_LARGE = ResourceLocation.fromNamespaceAndPath(CreateRNS.ID, "ore_deposit_large");

    private static final ResourceLocation DEPOSIT_STRUCTURE_TAG_ID = ResourceLocation.fromNamespaceAndPath(CreateRNS.ID, "deposits");
    private static final String BIOME_TAG_PREFIX = "has_deposit";

    //=================================================== Presets ====================================================//
    public static final StructurePreset PRESET_OVERWORLD_RARE = new StructurePreset(
            DumpSafeOverrides.OVERWORLD, 12, 0, 20,
            List.of(new WeightedTemplate(TEMPLATE_SMALL, 70), new WeightedTemplate(TEMPLATE_MEDIUM, 28),
                    new WeightedTemplate(TEMPLATE_LARGE, 2)));

    public static final StructurePreset PRESET_NETHER_RARE = new StructurePreset(
            DumpSafeOverrides.NETHER, 4, 0, 20, PRESET_OVERWORLD_RARE.templates());

    public static final StructurePreset PRESET_OVERWORLD_UNCOMMON = new StructurePreset(
            DumpSafeOverrides.OVERWORLD, 10, 0, 35,
            List.of(new WeightedTemplate(TEMPLATE_SMALL, 30), new WeightedTemplate(TEMPLATE_MEDIUM, 60),
                    new WeightedTemplate(TEMPLATE_LARGE, 10)));

    public static final StructurePreset PRESET_NETHER_UNCOMMON = new StructurePreset(
            DumpSafeOverrides.NETHER, 4, 0, 35, PRESET_OVERWORLD_UNCOMMON.templates());

    public static final StructurePreset PRESET_OVERWORLD_COMMON = new StructurePreset(
            DumpSafeOverrides.OVERWORLD, 8, 0, 50,
            List.of(new WeightedTemplate(TEMPLATE_MEDIUM, 70), new WeightedTemplate(TEMPLATE_LARGE, 30)));

    public static final StructurePreset PRESET_NETHER_COMMON = new StructurePreset(
            DumpSafeOverrides.NETHER, 4, 0, 50, PRESET_OVERWORLD_COMMON.templates());

    private static final Map<String, StructurePreset> PRESETS = Map.of(
            "overworld_common", PRESET_OVERWORLD_COMMON,
            "overworld_uncommon", PRESET_OVERWORLD_UNCOMMON,
            "overworld_rare", PRESET_OVERWORLD_RARE,
            "nether_common", PRESET_NETHER_COMMON,
            "nether_uncommon", PRESET_NETHER_UNCOMMON,
            "nether_rare", PRESET_NETHER_RARE
    );
    //================================================================================================================//

    public static StructurePreset getStructurePreset(String presetId) {
        var preset = DepositData.PRESETS.get(presetId);
        if (preset != null) return preset;

        throw new IllegalArgumentException("Unknown deposit preset: " + presetId + "\n" +
                "Available presets: " + String.join(", ", DepositData.PRESETS.keySet()));
    }

    public static List<BlockEntry> getBlocks() {
        return Collections.unmodifiableList(BLOCKS);
    }

    public static List<BlockEntry> getHiddenBlocks() {
        return BLOCKS.stream()
                .filter(b -> RNSKubeJSPluginBridge.isManagingDeposits()
                        ? !RNSKubeJSPluginBridge.getEnabledDepositBlocks().contains(b.blockId())
                        : !b.isVisible.get())
                .toList();
    }

    public static List<StructureEntry> getStructures() {
        return Collections.unmodifiableList(STRUCTURES);
    }

    public static List<StructureEntry> getScannableStructures(ResourceLocation dimension) {
        return STRUCTURES.stream()
                .filter(s -> s.dimension.equals(dimension) && s.isScannable.get())
                .toList();
    }

    public static List<RecipeEntry> getRecipes() {
        return Collections.unmodifiableList(RECIPES);
    }

    public static List<RecipeEntry> getEnabledRecipes() {
        return RECIPES.stream()
                .filter(r -> RNSKubeJSPluginBridge.isManagingDeposits()
                        ? RNSKubeJSPluginBridge.getEnabledDepositBlocks().contains(r.sourceBlockId())
                        : r.isEnabled.get())
                .toList();
    }

    public static List<String> metalScannerIconCandidates(String material) {
        return List.of(
                "#forge:raw_materials/" + material,
                "#forge:ores/" + material,
                "#forge:ingots/" + material,
                "#forge:nuggets/" + material);
    }

    public static List<String> gemScannerIconCandidates(String material) {
        return List.of("#forge:gems/" + material);
    }

    public static List<String> dustScannerIconCandidates(String material) {
        return List.of("#forge:dusts/" + material);
    }

    public static List<String> coalScannerIconCandidates() {
        return List.of("#minecraft:coals");
    }

    public static ResourceLocation blockId(String namespace, String keyword) {
        return ResourceLocation.fromNamespaceAndPath(namespace, keyword + "_deposit_block");
    }

    public static ResourceLocation structureId(String namespace, ResourceLocation dimension, String keyword) {
        return ResourceLocation.fromNamespaceAndPath(namespace, "deposit_" + dimPrefix(dimension) + keyword);
    }

    public static ResourceLocation processorId(String namespace, String keyword) {
        return processorId(namespace, blockId(namespace, keyword));
    }

    public static ResourceLocation processorId(String namespace, ResourceLocation blockId) {
        return ResourceLocation.fromNamespaceAndPath(namespace,
                "replace_with_" + blockId.getNamespace() + "_" + blockId.getPath());
    }

    public static ResourceLocation specId(String namespace, ResourceLocation dimension, String keyword) {
        return specId(structureId(namespace, dimension, keyword));
    }

    public static ResourceLocation specId(ResourceLocation structureId) {
        var path = structureId.getPath();
        if (path.startsWith("deposit_")) path = path.substring("deposit_".length());
        return ResourceLocation.fromNamespaceAndPath(structureId.getNamespace(), path);
    }

    public static String structureLangKey(ResourceLocation structureId) {
        return structureId.getNamespace() + ".structure." + structureId.getPath().replace('/', '.');
    }

    public static ResourceLocation structureSetId(String namespace, ResourceLocation dimension) {
        return ResourceLocation.fromNamespaceAndPath(namespace, dimPrefix(dimension) + "deposits");
    }

    public static ResourceLocation biomeTagId(String namespace, ResourceLocation dimension) {
        return ResourceLocation.fromNamespaceAndPath(namespace, BIOME_TAG_PREFIX + dimSuffix(dimension));
    }

    public static ResourceLocation structureTagId() {
        return DEPOSIT_STRUCTURE_TAG_ID;
    }

    public static ResourceLocation recipeId(String namespace, ResourceLocation dimension, String keyword) {
        var blockId = blockId(namespace, keyword);
        return ResourceLocation.fromNamespaceAndPath(namespace,
                dimPrefix(dimension) + blockId.getNamespace() + "_" + blockId.getPath());
    }

    public static PlacementStrategy defaultPlacementStrategy(ResourceLocation dimension) {
        if (dimension.equals(DumpSafeOverrides.NETHER)) {
            return PlacementStrategy.NETHER;
        } else {
            return PlacementStrategy.OVERWORLD;
        }
    }

    /// Vanilla dimensions are abbreviated
    public static String dimensionName(ResourceLocation dimension) {
        if (dimension.equals(DumpSafeOverrides.OVERWORLD)) {
            return "";
        } else if (dimension.equals(DumpSafeOverrides.NETHER)) {
            return "nether";
        } else if (dimension.equals(DumpSafeOverrides.END)) {
            return "end";
        } else {
            return dimension.getNamespace() + "_" + dimension.getPath();
        }
    }

    private static String dimPrefix(ResourceLocation dimension) {
        var name =  dimensionName(dimension);
        return name.isBlank() ? "" : name + "_";
    }

    private static String dimSuffix(ResourceLocation dimension) {
        var name =  dimensionName(dimension);
        return name.isBlank() ? "" : "_" + name;
    }

    public record WeightedTemplate(ResourceLocation template, int weight) {}

    public record StructurePreset(
            ResourceLocation dimension, int depth, int depthDeviation, int weight, List<WeightedTemplate> templates
    ) {
        public StructurePreset {
            templates = List.copyOf(templates);
        }
    }

    public record BlockEntry(
            String namespace,
            String keyword,
            Supplier<Boolean> isVisible
    ) {
        public ResourceLocation blockId() {
            return DepositData.blockId(namespace, keyword);
        }

        public ResourceLocation processorId() {
            return DepositData.processorId(namespace, keyword);
        }
    }

    public record StructureEntry(
            String namespace,
            String keyword,
            ResourceLocation dimension,
            Supplier<Boolean> isScannable,
            int depth,
            int depthDeviation,
            int weight,
            List<WeightedTemplate> templates,
            PlacementStrategy placementStrategy,
            List<String> scannerIconCandidates,
            List<String> mapIconCandidates
    ) {
        public StructureEntry {
            templates = List.copyOf(templates);
            scannerIconCandidates = List.copyOf(scannerIconCandidates);
            mapIconCandidates = List.copyOf(mapIconCandidates);
        }

        public ResourceLocation structureId() {
            return DepositData.structureId(namespace, dimension, keyword);
        }

        public ResourceLocation specId() {
            return DepositData.specId(namespace, dimension, keyword);
        }
    }

    public record RecipeEntry(
            String namespace,
            String keyword,
            ResourceLocation dimension,
            Supplier<Boolean> isEnabled,
            @Nullable ResourceLocation replacementBlockId,
            @Nullable DepositDurability durability,
            List<YieldEntry> yields
    ) {
        public RecipeEntry {
            yields = List.copyOf(yields);
        }

        public ResourceLocation sourceBlockId() {
            return DepositData.blockId(namespace, keyword);
        }

        public ResourceLocation recipeId() {
            return DepositData.recipeId(namespace, dimension, keyword);
        }
    }

    public record YieldEntry(
            float chance,
            List<WeightedItemEntry> items,
            List<ResourceLocation> catalysts,
            int jeiSlotColor
    ) {
        public YieldEntry {
            items = List.copyOf(items);
            catalysts = List.copyOf(catalysts);
        }
    }

    public record WeightedItemEntry(
            List<String> candidateIds,
            int weight,
            boolean compat
    ) {
        public WeightedItemEntry {
            candidateIds = List.copyOf(candidateIds);
        }
    }
}
