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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class DepositResources {
    public static final ResourceLocation PROCESSOR_INPUT_BLOCK = ResourceLocation.withDefaultNamespace("end_stone");

    public static final ResourceLocation TEMPLATE_SMALL = CreateRNS.asResource("ore_deposit_small");
    public static final ResourceLocation TEMPLATE_MEDIUM = CreateRNS.asResource("ore_deposit_medium");
    public static final ResourceLocation TEMPLATE_LARGE = CreateRNS.asResource("ore_deposit_large");

    private static final ResourceLocation DEPOSIT_STRUCTURE_TAG_ID = CreateRNS.asResource("deposits");

    //=================================================== Presets ====================================================//
    public static final StructurePreset PRESET_OVERWORLD_RARE = new StructurePreset(
            Level.OVERWORLD, 8, 0, 20,
            List.of(new WeightedTemplate(TEMPLATE_SMALL, 70), new WeightedTemplate(TEMPLATE_MEDIUM, 28),
                    new WeightedTemplate(TEMPLATE_LARGE, 2)));

    public static final StructurePreset PRESET_NETHER_RARE = new StructurePreset(
            Level.NETHER, 4, 0, 20, PRESET_OVERWORLD_RARE.templates());

    public static final StructurePreset PRESET_END_RARE = new StructurePreset(
            Level.END, PRESET_OVERWORLD_RARE.depth(), PRESET_OVERWORLD_RARE.depthDeviation(),
            PRESET_OVERWORLD_RARE.weight(), PRESET_OVERWORLD_RARE.templates());

    public static final StructurePreset PRESET_OVERWORLD_UNCOMMON = new StructurePreset(
            Level.OVERWORLD, 6, 0, 35,
            List.of(new WeightedTemplate(TEMPLATE_SMALL, 30), new WeightedTemplate(TEMPLATE_MEDIUM, 60),
                    new WeightedTemplate(TEMPLATE_LARGE, 10)));

    public static final StructurePreset PRESET_NETHER_UNCOMMON = new StructurePreset(
            Level.NETHER, 4, 0, 35, PRESET_OVERWORLD_UNCOMMON.templates());

    public static final StructurePreset PRESET_END_UNCOMMON = new StructurePreset(
            Level.END, PRESET_OVERWORLD_UNCOMMON.depth(), PRESET_OVERWORLD_UNCOMMON.depthDeviation(),
            PRESET_OVERWORLD_UNCOMMON.weight(), PRESET_OVERWORLD_UNCOMMON.templates());

    public static final StructurePreset PRESET_OVERWORLD_COMMON = new StructurePreset(
            Level.OVERWORLD, 4, 0, 50,
            List.of(new WeightedTemplate(TEMPLATE_MEDIUM, 70), new WeightedTemplate(TEMPLATE_LARGE, 30)));

    public static final StructurePreset PRESET_NETHER_COMMON = new StructurePreset(
            Level.NETHER, 4, 0, 50, PRESET_OVERWORLD_COMMON.templates());

    public static final StructurePreset PRESET_END_COMMON = new StructurePreset(
            Level.END, PRESET_OVERWORLD_COMMON.depth(), PRESET_OVERWORLD_COMMON.depthDeviation(),
            PRESET_OVERWORLD_COMMON.weight(), PRESET_OVERWORLD_COMMON.templates());

    private static final Map<String, StructurePreset> PRESETS = Map.of(
            "overworld_common", PRESET_OVERWORLD_COMMON,
            "overworld_uncommon", PRESET_OVERWORLD_UNCOMMON,
            "overworld_rare", PRESET_OVERWORLD_RARE,
            "nether_common", PRESET_NETHER_COMMON,
            "nether_uncommon", PRESET_NETHER_UNCOMMON,
            "nether_rare", PRESET_NETHER_RARE,
            "end_common", PRESET_END_COMMON,
            "end_uncommon", PRESET_END_UNCOMMON,
            "end_rare", PRESET_END_RARE
    );
    //================================================================================================================//

    public static StructurePreset getStructurePreset(String presetId) {
        var preset = DepositResources.PRESETS.get(presetId);
        if (preset != null) return preset;

        throw new IllegalArgumentException("Unknown deposit preset: " + presetId + "\n" +
                "Available presets: " + String.join(", ", DepositResources.PRESETS.keySet()));
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

    public static String catalystLangKey(ResourceLocation catalystId, String suffix) {
        return catalystId.getNamespace() + ".catalyst." + catalystId.getPath().replace('/', '.') + "." + suffix;
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
        return dimension.equals(Level.NETHER) ? PlacementStrategy.NETHER : PlacementStrategy.DEFAULT;
    }

    public static ResourceLocation miningCategoryId(ResourceKey<Level> dimension) {
        var id = dimension.location();
        return CreateRNS.asResource(dimension.equals(Level.OVERWORLD) ? "mining"
                : "mining_" + id.getNamespace() + "_" + id.getPath());
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

    public final List<Block> blocks = new ArrayList<>();
    public final List<BlockTag> blockTags = new ArrayList<>();
    public final List<Structure> structures = new ArrayList<>();
    public final List<StructureTag> structureTags = new ArrayList<>();
    public final List<Spec> specs = new ArrayList<>();
    public final List<BiomeSelectorTag> biomeSelectorTags = new ArrayList<>();
    public final List<Processor> processors = new ArrayList<>();
    public final List<StructureSet> structureSets = new ArrayList<>();
    public final List<Recipe> recipes = new ArrayList<>();
    public final List<Catalyst> catalysts = new ArrayList<>();
    public final List<LangEntry> langEntries = new ArrayList<>();

    public List<Block> getHiddenBlocks() {
        var selection = RNSKubeJSPluginBridge.getSelectedDepositBlocks();
        var visible = selection.values().stream().flatMap(Set::stream).collect(Collectors.toSet());
        return blocks.stream()
                .filter(b -> !selection.isEmpty()
                        ? !visible.contains(b.id())
                        : !b.isVisible.get())
                .toList();
    }

    public List<Structure> getScannableStructures(ResourceKey<Level> dimension) {
        return structures.stream()
                .filter(s -> s.dimension.equals(dimension) && getSpec(s.id()).isScannable().get())
                .toList();
    }

    public Spec getSpec(ResourceLocation structureId) {
        return specs.stream().filter(spec -> spec.structureId().equals(structureId)).findFirst()
                .orElseThrow(() -> new IllegalStateException("Missing deposit spec for structure: " + structureId));
    }

    public Processor getProcessor(ResourceLocation id) {
        return processors.stream().filter(processor -> processor.id().equals(id)).findFirst()
                .orElseThrow(() -> new IllegalStateException("Missing deposit processor: " + id));
    }

    public List<Recipe> getEnabledRecipes() {
        var selection = RNSKubeJSPluginBridge.getSelectedDepositBlocks();
        return recipes.stream()
                .filter(r -> !selection.isEmpty()
                        ? selection.getOrDefault(r.dimension(), Set.of()).contains(r.sourceBlockId())
                        : r.isEnabled.get())
                .toList();
    }

    public record WeightedTemplate(ResourceLocation template, int weight) {}

    /// Used in a structure set
    public record WeightedStructure(ResourceLocation structureId, int weight) {}

    public record StructurePreset(
            ResourceKey<Level> dimension, int depth, int depthDeviation, int weight, List<WeightedTemplate> templates
    ) {
        public StructurePreset {
            templates = List.copyOf(templates);
        }
    }

    public record Block(
            ResourceLocation id,
            Supplier<Boolean> isVisible
    ) {}

    public record BlockTag(
            ResourceLocation id,
            List<ResourceOrTagLocation> blockIds,
            boolean replace
    ) {
        public BlockTag {
            blockIds = List.copyOf(blockIds);
        }
    }

    public record Structure(
            ResourceLocation id,
            ResourceLocation processorId,
            ResourceKey<Level> dimension,
            ResourceOrTagLocation biomeSelector,
            int depth,
            int depthDeviation,
            @Nullable Integer weight,
            PlacementStrategy placementStrategy,
            List<WeightedTemplate> templates
    ) {
        public Structure {
            templates = List.copyOf(templates);
        }
    }

    public record Spec(
            ResourceLocation id,
            ResourceLocation structureId,
            ResourceKey<Level> dimension,
            Supplier<Boolean> isScannable,
            List<ResourceOrTagLocation> scannerIconCandidates,
            List<ResourceOrTagLocation> mapIconCandidates
    ) {
        public Spec {
            scannerIconCandidates = List.copyOf(scannerIconCandidates);
            mapIconCandidates = List.copyOf(mapIconCandidates);
        }
    }

    public record Processor(
            ResourceLocation id,
            ResourceLocation targetBlock
    ) {}

    public record StructureTag(
            ResourceLocation id,
            List<ResourceOrTagLocation> structureIds,
            boolean replace
    ) {
        public StructureTag {
            structureIds = List.copyOf(structureIds);
        }
    }

    public record BiomeSelectorTag(
            ResourceLocation id,
            List<ResourceOrTagLocation> biomeIds,
            boolean replace
    ) {
        public BiomeSelectorTag {
            biomeIds = List.copyOf(biomeIds);
        }
    }

    public record StructureSet(
            ResourceLocation id,
            List<WeightedStructure> structureIds,
            int spacing,
            int separation,
            int salt
    ) {
        public StructureSet {
            structureIds = List.copyOf(structureIds);
        }
    }

    public record Recipe(
            ResourceLocation id,
            ResourceLocation sourceBlockId,
            ResourceKey<Level> dimension,
            Supplier<Boolean> isEnabled,
            @Nullable ResourceLocation replacementBlockId,
            @Nullable DepositDurability durability,
            List<YieldEntry> yields
    ) {
        public Recipe {
            yields = List.copyOf(yields);
        }
    }

    public record Catalyst(
            ResourceLocation id,
            float chanceMultiplier,
            boolean optional,
            @Nullable Integer displayPriority,
            List<ResourceLocation> representativeItems,
            List<ResourceLocation> hideIfPresent,
            @Nullable ResourceLocation playWhenActive,
            List<CatalystRequirement> requirements
    ) {
        public Catalyst {
            representativeItems = List.copyOf(representativeItems);
            hideIfPresent = List.copyOf(hideIfPresent);
            requirements = List.copyOf(requirements);
        }
    }

    public sealed interface CatalystRequirement permits AttachmentRequirement, FluidRequirement {}

    public record AttachmentRequirement(
            List<ResourceLocation> blockIds,
            int count
    ) implements CatalystRequirement {
        public AttachmentRequirement {
            blockIds = List.copyOf(blockIds);
        }
    }

    public record FluidRequirement(
            ResourceLocation fluidId,
            int amount
    ) implements CatalystRequirement {}

    public record LangEntry(String assetNamespace, String key, String value) {}

    public record YieldEntry(
            float chance,
            List<WeightedItemStackEntry> items,
            List<ResourceLocation> catalysts,
            int jeiSlotColor
    ) {
        public YieldEntry {
            items = List.copyOf(items);
            catalysts = List.copyOf(catalysts);
        }
    }

    public record WeightedItemStackEntry(
            List<String> candidateIds,
            int count,
            int weight,
            boolean compat
    ) {
        public WeightedItemStackEntry {
            candidateIds = List.copyOf(candidateIds);
        }
    }
}
