package com.bmaster.createrns.compat.kubejs;

import com.bmaster.createrns.data.pack.DepositDimension;
import com.bmaster.createrns.data.pack.DepositSpecBuilder;
import com.bmaster.createrns.data.pack.DepositStructureBuilder;
import com.bmaster.createrns.data.pack.DynamicDatapackContent;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dev.latvian.mods.kubejs.client.LangKubeEvent;
import dev.latvian.mods.kubejs.generator.KubeDataGenerator;
import dev.latvian.mods.kubejs.typings.Info;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class DepositStructureKubeBuilder {
    private final ResourceLocation id;
    private final ResourceLocation processorId;
    private final @Nullable DepositSpecBuilder.ConfiguredEntry builtInSpec;
    private final List<WeightedTemplate> weightedTemplates = new ArrayList<>();
    private final List<String> scannerIconCandidates = new ArrayList<>();
    private final List<String> mapIconCandidates = new ArrayList<>();

    private ResourceLocation blockId;
    private DepositDimension dimension = DepositDimension.OVERWORLD;
    private Integer height;
    private int heightDeviation;
    private Integer structureSetWeight;
    private String displayName;
    private boolean scannerIconsInherited;
    private boolean mapIconsInherited;
    private boolean templatesInherited;
    private boolean weightChanged;

    public DepositStructureKubeBuilder(ResourceLocation id) {
        this.id = id;
        processorId = ResourceLocation.fromNamespaceAndPath(id.getNamespace(),
                "replace_with_" + id.getNamespace() + "_" + id.getPath().replace('/', '_'));
        builtInSpec = null;
    }

    DepositStructureKubeBuilder(
            DepositStructureBuilder.ConfiguredEntry structureEntry,
            DepositSpecBuilder.ConfiguredEntry specEntry
    ) {
        var structure = structureEntry.structure();

        id = DepositStructureBuilder.structureId(structureEntry);
        processorId = DynamicDatapackContent.processorId(structure.depositBlock());
        builtInSpec = specEntry;
        blockId = structure.depositBlock();
        dimension = structure.dimension();
        height = -structure.depth();
        heightDeviation = structure.depthDeviation();
        structureSetWeight = structure.weight();
        scannerIconCandidates.addAll(specEntry.spec().scannerIconCandidates());
        mapIconCandidates.addAll(specEntry.spec().mapIconCandidates());
        for (var template : structure.weightedTemplates()) {
            weightedTemplates.add(new WeightedTemplate(template.template(), template.weight()));
        }
        scannerIconsInherited = true;
        mapIconsInherited = true;
        templatesInherited = true;
    }

    @Info("Sets the deposit block used by this deposit structure.")
    public DepositStructureKubeBuilder block(String blockId) {
        if (isTweak()) {
            throw new UnsupportedOperationException("A built-in deposit structure's block cannot be changed");
        }
        this.blockId = ResourceLocation.parse(blockId);
        return this;
    }

    @Info("Sets the structure name shown by the deposit scanner.")
    public DepositStructureKubeBuilder displayName(String name) {
        displayName = name;
        return this;
    }

    @Info("Sets the Y offset from the world surface for the deposit.")
    public DepositStructureKubeBuilder height(int value) {
        height = value;
        heightDeviation = 0;
        return this;
    }

    @Info("""
            Sets the weight used when this deposit structure is selected in StartupEvents.rnsEnableDeposits.
            Preset structures already assign a default.
            """)
    public DepositStructureKubeBuilder weight(int value) {
        if (value <= 0) throw new IllegalArgumentException("Structure set weight must be positive");

        structureSetWeight = value;
        weightChanged = true;
        return this;
    }

    @Info("""
            Uses the specified item or item tag as an icon in deposit scanner for the respective deposit.
            If tag is specified, the first item with that tag is picked.
            Can be called multiple times to add more items as fallbacks in case the original item does not exist.
            """)
    public DepositStructureKubeBuilder scannerIcon(String candidateId) {
        if (scannerIconsInherited) {
            scannerIconCandidates.clear();
            scannerIconsInherited = false;
        }
        scannerIconCandidates.add(candidateId);
        return this;
    }

    @Info("""
            Adds all metal-related tags as scanner icon candidates.
            The first item of a first non-empty tag is selected.
            """)
    public DepositStructureKubeBuilder scannerIconMetal(String material) {
        for (var candidate : DepositSpecBuilder.metalScannerIconCandidates(material)) {
            scannerIcon(candidate);
        }
        return this;
    }

    @Info("""
            Adds all gem-related tags as scanner icon candidates.
            The first item of a first non-empty tag is selected.
            """)
    public DepositStructureKubeBuilder scannerIconGem(String material) {
        for (var candidate : DepositSpecBuilder.gemScannerIconCandidates(material)) {
            scannerIcon(candidate);
        }
        return this;
    }

    @Info("""
            Adds all dust-related tags as scanner icon candidates.
            The first item of a first non-empty tag is selected.
            """)
    public DepositStructureKubeBuilder scannerIconDust(String material) {
        for (var candidate : DepositSpecBuilder.dustScannerIconCandidates(material)) {
            scannerIcon(candidate);
        }
        return this;
    }

    @Info("""
            Uses the specified item or item tag as a map icon for a found deposit.
            Can be called multiple times to add more items as fallbacks in case the original item does not exist.
            """)
    public DepositStructureKubeBuilder mapIcon(String candidateId) {
        if (mapIconsInherited) {
            mapIconCandidates.clear();
            mapIconsInherited = false;
        }
        mapIconCandidates.add(candidateId);
        return this;
    }

    @Info("""
            Adds a structure nbt template to the generated deposit.
            Can be called multiple times to specify more templates.
            When multiple templates are specified, the chance of picking one is determined by the weight.
            """)
    public DepositStructureKubeBuilder nbt(String templateId, int weight) {
        if (weight <= 0) throw new IllegalArgumentException("Template weight must be positive");

        if (templatesInherited) {
            weightedTemplates.clear();
            templatesInherited = false;
        }
        weightedTemplates.add(new WeightedTemplate(ResourceLocation.parse(templateId), weight));
        return this;
    }

    @Info("""
            Applies a preset that covers height, weighted nbts, and the default structure set weight.
            These presets are used by the default deposits (iron - common, nickel - uncommon, redstone - precious).
            Supported values: overworld_common, overworld_uncommon, overworld_rare, nether_common, nether_uncommon, nether_rare.
            """)
    public DepositStructureKubeBuilder preset(String presetId) {
        var preset = DepositStructureBuilder.getPreset(presetId);
        weightedTemplates.clear();
        templatesInherited = false;
        weight(preset.weight());
        height(-preset.depth());
        for (var template : preset.weightedTemplates()) {
            nbt(template.template().toString(), template.weight());
        }
        return this;
    }

    void generateData(KubeDataGenerator generator) {
        generateData(generator, true);
    }

    void generateData(KubeDataGenerator generator, boolean scannable) {
        validate();
        generator.json(depositSpecPath(), depositSpecJson(scannable));
        generator.json(processorPath(), processorJson());
        generator.json(structurePath(), structureJson());
    }

    void generateLang(LangKubeEvent lang) {
        validate();
        if (displayName != null) {
            lang.add(id.getNamespace(), structureLangKey(), displayName);
        }
    }

    ResourceLocation id() {
        return id;
    }

    DepositDimension dimension() {
        return dimension;
    }

    void setDimension(DepositDimension dimension) {
        this.dimension = dimension;
    }

    Integer structureSetWeight() {
        return structureSetWeight;
    }

    boolean weightChanged() {
        return weightChanged;
    }

    boolean defaultScannable() {
        if (builtInSpec == null) return true;
        return builtInSpec.spec().scannable().get();
    }

    boolean isTweak() {
        return builtInSpec != null;
    }

    ResourceLocation blockId() {
        if (blockId == null) {
            throw new IllegalStateException("Deposit structure " + id + " must specify a deposit block");
        }

        return blockId;
    }

    private ResourceLocation depositSpecId() {
        return ResourceLocation.fromNamespaceAndPath(id.getNamespace(), normalizedStructureName());
    }

    private ResourceLocation depositSpecPath() {
        if (builtInSpec != null) return DynamicDatapackContent.depositSpecPath(builtInSpec);

        var specId = depositSpecId();
        return ResourceLocation.fromNamespaceAndPath(specId.getNamespace(), "create_rns/deposit_spec/" + specId.getPath());
    }

    private ResourceLocation processorId() {
        return processorId;
    }

    private ResourceLocation processorPath() {
        var processorId = processorId();
        return ResourceLocation.fromNamespaceAndPath(processorId.getNamespace(),
                "worldgen/processor_list/" + processorId.getPath());
    }

    private ResourceLocation structurePath() {
        return ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "worldgen/structure/" + id.getPath());
    }

    private String structureLangKey() {
        return id.getNamespace() + ".structure." + id.getPath().replace('/', '.');
    }

    private String normalizedStructureName() {
        return id.getPath().startsWith("deposit_")
                ? id.getPath().substring("deposit_".length())
                : id.getPath();
    }

    private void validate() {
        if (blockId == null) {
            throw new IllegalStateException("Deposit structure " + id + " must specify a deposit block");
        }

        if (scannerIconCandidates.isEmpty()) {
            throw new IllegalStateException("Deposit structure " + id + " must specify at least one scanner icon");
        }

        if (weightedTemplates.isEmpty()) {
            throw new IllegalStateException("Deposit structure " + id + " must specify at least one nbt template");
        }
    }

    private JsonObject depositSpecJson(boolean scannable) {
        var json = new JsonObject();
        json.add("scanner_icon_item", stringOrArray(scannerIconCandidates));
        json.add("map_icon_item", stringOrArray(mapIconCandidates.isEmpty()
                ? List.of(blockId.toString())
                : mapIconCandidates));
        json.addProperty("structure", id.toString());
        if (!isTweak() || dimension != DepositDimension.OVERWORLD) {
            json.addProperty("dimension", dimension.levelDimension().location().toString());
        }
        if (!scannable) {
            json.addProperty("scannable", false);
        }
        return json;
    }

    private JsonObject processorJson() {
        var json = new JsonObject();
        var processors = new JsonArray();
        var processor = new JsonObject();
        var rules = new JsonArray();
        var rule = new JsonObject();
        var inputPredicate = new JsonObject();
        var locationPredicate = new JsonObject();
        var outputState = new JsonObject();

        inputPredicate.addProperty("block", DepositStructureBuilder.PROCESSOR_INPUT_BLOCK.toString());
        inputPredicate.addProperty("predicate_type", "minecraft:block_match");
        locationPredicate.addProperty("predicate_type", "minecraft:always_true");
        outputState.addProperty("Name", blockId.toString());

        rule.add("input_predicate", inputPredicate);
        rule.add("location_predicate", locationPredicate);
        rule.add("output_state", outputState);
        rules.add(rule);

        processor.addProperty("processor_type", "minecraft:rule");
        processor.add("rules", rules);
        processors.add(processor);

        json.add("processors", processors);
        return json;
    }

    private JsonObject structureJson() {
        var json = new JsonObject();
        json.addProperty("type", "create_rns:deposit");
        json.addProperty("biomes", biomeTag());
        json.addProperty("placement_strategy", dimension.placement().getSerializedName());
        if (heightDeviation == 0) {
            json.addProperty("height", resolvedHeight());
        } else {
            var heightRange = new JsonObject();
            heightRange.addProperty("min", resolvedHeight() - heightDeviation);
            heightRange.addProperty("max", resolvedHeight() + heightDeviation);
            json.add("height", heightRange);
        }
        json.add("structures", weightedStructuresJson());
        return json;
    }

    private JsonArray weightedStructuresJson() {
        var structures = new JsonArray();

        for (var entry : weightedTemplates) {
            var structure = new JsonObject();
            structure.addProperty("id", entry.template().toString());
            structure.addProperty("weight", entry.weight());
            structure.addProperty("processor", processorId().toString());
            structures.add(structure);
        }

        return structures;
    }

    private int resolvedHeight() {
        if (height != null) return height;
        var preset = dimension == DepositDimension.NETHER
                ? DepositStructureBuilder.NETHER_COMMON
                : DepositStructureBuilder.OVERWORLD_COMMON;
        return -preset.depth();
    }

    private String biomeTag() {
        return dimension == DepositDimension.NETHER
                ? "#create_rns:has_deposit_nether"
                : "#create_rns:has_deposit";
    }

    private static JsonElement stringOrArray(List<String> values) {
        if (values.size() == 1) return new JsonPrimitive(values.getFirst());

        var array = new JsonArray();
        for (var value : values) {
            array.add(value);
        }
        return array;
    }

    private record WeightedTemplate(ResourceLocation template, int weight) {
    }
}
