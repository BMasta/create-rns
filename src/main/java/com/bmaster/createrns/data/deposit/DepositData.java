package com.bmaster.createrns.data.deposit;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.compat.kubejs.RNSKubeJSPluginBridge;
import com.bmaster.createrns.content.deposit.mining.recipe.DepositDurability;
import com.bmaster.createrns.content.deposit.worldgen.PlacementStrategy;
import com.bmaster.createrns.util.codec.ResourceOrTagLocation;
import com.mojang.logging.annotations.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class DepositData {

    static final List<BlockEntry> BLOCKS = new ArrayList<>();
    static final List<StructureEntry> STRUCTURES = new ArrayList<>();
    static final List<RecipeEntry> RECIPES = new ArrayList<>();

    public static final ResourceLocation PROCESSOR_INPUT_BLOCK = ResourceLocation.withDefaultNamespace("end_stone");

    public static final ResourceLocation TEMPLATE_SMALL = CreateRNS.asResource("ore_deposit_small");
    public static final ResourceLocation TEMPLATE_MEDIUM = CreateRNS.asResource("ore_deposit_medium");
    public static final ResourceLocation TEMPLATE_LARGE = CreateRNS.asResource("ore_deposit_large");

    private static final ResourceLocation DEPOSIT_STRUCTURE_TAG_ID = CreateRNS.asResource("deposits");

    //=================================================== Presets ====================================================//
    public static final StructurePreset PRESET_OVERWORLD_RARE = new StructurePreset(
            Level.OVERWORLD, 12, 0, 20,
            List.of(new WeightedTemplate(TEMPLATE_SMALL, 70), new WeightedTemplate(TEMPLATE_MEDIUM, 28),
                    new WeightedTemplate(TEMPLATE_LARGE, 2)));

    public static final StructurePreset PRESET_NETHER_RARE = new StructurePreset(
            Level.NETHER, 4, 0, 20, PRESET_OVERWORLD_RARE.templates());

    public static final StructurePreset PRESET_OVERWORLD_UNCOMMON = new StructurePreset(
            Level.OVERWORLD, 10, 0, 35,
            List.of(new WeightedTemplate(TEMPLATE_SMALL, 30), new WeightedTemplate(TEMPLATE_MEDIUM, 60),
                    new WeightedTemplate(TEMPLATE_LARGE, 10)));

    public static final StructurePreset PRESET_NETHER_UNCOMMON = new StructurePreset(
            Level.NETHER, 4, 0, 35, PRESET_OVERWORLD_UNCOMMON.templates());

    public static final StructurePreset PRESET_OVERWORLD_COMMON = new StructurePreset(
            Level.OVERWORLD, 8, 0, 50,
            List.of(new WeightedTemplate(TEMPLATE_MEDIUM, 70), new WeightedTemplate(TEMPLATE_LARGE, 30)));

    public static final StructurePreset PRESET_NETHER_COMMON = new StructurePreset(
            Level.NETHER, 4, 0, 50, PRESET_OVERWORLD_COMMON.templates());

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

    public static List<StructureEntry> getScannableStructures(ResourceKey<Level> dimension) {
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

    public static List<ResourceOrTagLocation> metalScannerIconCandidates(String material) {
        return List.of(
                ResourceOrTagLocation.fromNamespaceAndPath("c", "raw_materials/" + material, true),
                ResourceOrTagLocation.fromNamespaceAndPath("c", "ores/" + material, true),
                ResourceOrTagLocation.fromNamespaceAndPath("c", "ingots/" + material, true),
                ResourceOrTagLocation.fromNamespaceAndPath("c", "nuggets/" + material, true));
    }

    public static List<ResourceOrTagLocation> gemScannerIconCandidates(String material) {
        return List.of(ResourceOrTagLocation.fromNamespaceAndPath("c", "gems/" + material, true));
    }

    public static List<ResourceOrTagLocation> dustScannerIconCandidates(String material) {
        return List.of(ResourceOrTagLocation.fromNamespaceAndPath("c", "dusts/" + material, true));
    }

    public static List<ResourceOrTagLocation> coalScannerIconCandidates() {
        return List.of(ResourceOrTagLocation.withDefaultNamespace("coals", true));
    }

    public static ResourceLocation blockId(String namespace, String keyword) {
        return ResourceLocation.fromNamespaceAndPath(namespace, keyword + "_deposit_block");
    }

    public static ResourceLocation structureId(String namespace, ResourceKey<Level> dimension, String keyword) {
        return ResourceLocation.fromNamespaceAndPath(namespace, "deposit_" + dimPrefix(dimension) + keyword);
    }

    public static ResourceLocation processorId(String namespace, String keyword) {
        return processorId(namespace, blockId(namespace, keyword));
    }

    public static ResourceLocation processorId(String namespace, ResourceLocation blockId) {
        return ResourceLocation.fromNamespaceAndPath(namespace,
                "replace_with_" + blockId.getNamespace() + "_" + blockId.getPath());
    }

    public static ResourceLocation specId(String namespace, ResourceKey<Level> dimension, String keyword) {
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

    public static ResourceLocation structureSetId(String namespace, ResourceKey<Level> dimension) {
        return ResourceLocation.fromNamespaceAndPath(namespace, dimPrefix(dimension) + "deposits");
    }

    public static ResourceLocation biomeTagId(String namespace, ResourceKey<Level> dimension) {
        return ResourceLocation.fromNamespaceAndPath(namespace, "has_deposit" + dimSuffix(dimension));
    }

    public static ResourceLocation structureTagId() {
        return DEPOSIT_STRUCTURE_TAG_ID;
    }

    public static ResourceLocation recipeId(String namespace, ResourceKey<Level> dimension, String keyword) {
        var blockId = blockId(namespace, keyword);
        return ResourceLocation.fromNamespaceAndPath(namespace,
                dimPrefix(dimension) + blockId.getNamespace() + "_" + blockId.getPath());
    }

    public static PlacementStrategy defaultPlacementStrategy(ResourceKey<Level> dimension) {
        if (dimension.equals(Level.NETHER)) {
            return PlacementStrategy.NETHER;
        } else {
            return PlacementStrategy.OVERWORLD;
        }
    }

    /// Vanilla dimensions are abbreviated
    public static String dimensionName(ResourceKey<Level> dimension) {
        if (dimension.equals(Level.OVERWORLD)) {
            return "";
        } else if (dimension.equals(Level.NETHER)) {
            return "nether";
        } else if (dimension.equals(Level.END)) {
            return "end";
        } else {
            return dimension.location().getNamespace() + "_" + dimension.location().getPath();
        }
    }

    private static String dimPrefix(ResourceKey<Level> dimension) {
        var name =  dimensionName(dimension);
        return name.isBlank() ? "" : name + "_";
    }

    private static String dimSuffix(ResourceKey<Level> dimension) {
        var name =  dimensionName(dimension);
        return name.isBlank() ? "" : "_" + name;
    }

    public record WeightedTemplate(ResourceLocation template, int weight) {}

    public record WeightedStructure(ResourceLocation structureId, int weight) {}

    public record StructurePreset(
            ResourceKey<Level> dimension, int depth, int depthDeviation, int weight, List<WeightedTemplate> templates
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
            ResourceKey<Level> dimension,
            Supplier<Boolean> isScannable,
            int depth,
            int depthDeviation,
            int weight,
            List<WeightedTemplate> templates,
            PlacementStrategy placementStrategy,
            List<ResourceOrTagLocation> scannerIconCandidates,
            List<ResourceOrTagLocation> mapIconCandidates
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
            ResourceKey<Level> dimension,
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
