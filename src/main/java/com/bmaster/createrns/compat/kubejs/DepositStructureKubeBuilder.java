package com.bmaster.createrns.compat.kubejs;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.data.deposit.DepositData;
import com.bmaster.createrns.data.DumpSafeOverrides;
import com.bmaster.createrns.RNSRegistries;
import com.bmaster.createrns.util.Utils;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dev.latvian.mods.kubejs.client.LangEventJS;
import dev.latvian.mods.kubejs.generator.DataJsonGenerator;
import dev.latvian.mods.kubejs.typings.Info;
import dev.latvian.mods.kubejs.util.ConsoleJS;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class DepositStructureKubeBuilder {
    private static final Set<ResourceLocation> VANILLA_BIOMES = VanillaRegistries.createLookup()
            .lookupOrThrow(Registries.BIOME)
            .listElementIds()
            .map(ResourceKey::location)
            .collect(Collectors.toUnmodifiableSet());

    private final ResourceLocation id;
    private final @Nullable DepositData.StructureEntry builtInStructure;
    private final List<WeightedTemplate> weightedTemplates = new ArrayList<>();
    private final List<String> scannerIconCandidates = new ArrayList<>();
    private final List<String> mapIconCandidates = new ArrayList<>();

    private ResourceLocation blockId;
    private ResourceLocation dimension = DumpSafeOverrides.OVERWORLD;
    private @Nullable String biomeSelector;
    private Integer height;
    private int heightDeviation;
    private Integer structureSetWeight;
    private String displayName;
    private boolean scannerIconsInherited;
    private boolean mapIconsInherited;
    private boolean templatesInherited;
    private boolean weightChanged;
    private boolean invalid;

    public DepositStructureKubeBuilder(ResourceLocation id) {
        this.id = id;
        builtInStructure = null;
    }

    DepositStructureKubeBuilder(DepositData.StructureEntry structureEntry) {
        id = structureEntry.structureId();
        builtInStructure = structureEntry;
        blockId = DepositData.blockId(structureEntry.namespace(), structureEntry.keyword());
        dimension = structureEntry.dimension();
        height = -structureEntry.depth();
        heightDeviation = structureEntry.depthDeviation();
        structureSetWeight = structureEntry.weight();
        scannerIconCandidates.addAll(structureEntry.scannerIconCandidates());
        mapIconCandidates.addAll(structureEntry.mapIconCandidates());
        for (var template : structureEntry.templates()) {
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

    @Info("Sets the biome or biome tag in which this deposit structure may generate.")
    public DepositStructureKubeBuilder biome(String biomeOrTagId) {
        if (biomeOrTagId.isBlank()) {
            throw new IllegalArgumentException("Biome or biome tag id cannot be blank");
        }

        boolean isTag = biomeOrTagId.startsWith("#");
        var idString = isTag ? biomeOrTagId.substring(1) : biomeOrTagId;
        var id = ResourceLocation.tryParse(idString);
        if (id == null) {
            throw new IllegalArgumentException("Invalid biome or biome tag id: " + biomeOrTagId);
        }
        if (!isTag && id.getNamespace().equals("minecraft") && !VANILLA_BIOMES.contains(id)) {
            throw new IllegalArgumentException("Unknown vanilla biome: " + id);
        }

        biomeSelector = (isTag ? "#" : "") + id;
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
        validateItemCandidate(candidateId);
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
        for (var candidate : DepositData.metalScannerIconCandidates(material)) {
            scannerIcon(candidate);
        }
        return this;
    }

    @Info("""
            Adds all gem-related tags as scanner icon candidates.
            The first item of a first non-empty tag is selected.
            """)
    public DepositStructureKubeBuilder scannerIconGem(String material) {
        for (var candidate : DepositData.gemScannerIconCandidates(material)) {
            scannerIcon(candidate);
        }
        return this;
    }

    @Info("""
            Adds all dust-related tags as scanner icon candidates.
            The first item of a first non-empty tag is selected.
            """)
    public DepositStructureKubeBuilder scannerIconDust(String material) {
        for (var candidate : DepositData.dustScannerIconCandidates(material)) {
            scannerIcon(candidate);
        }
        return this;
    }

    @Info("""
            Uses the specified item or item tag as a map icon for a found deposit.
            Can be called multiple times to add more items as fallbacks in case the original item does not exist.
            """)
    public DepositStructureKubeBuilder mapIcon(String candidateId) {
        validateItemCandidate(candidateId);
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
        var preset = DepositData.getStructurePreset(presetId);
        weightedTemplates.clear();
        templatesInherited = false;
        weight(preset.weight());
        height(-preset.depth());
        heightDeviation = preset.depthDeviation();
        for (var template : preset.templates()) {
            nbt(template.template().toString(), template.weight());
        }
        return this;
    }

    void generateData(DataJsonGenerator generator) {
        generateData(generator, true);
    }

    void generateData(DataJsonGenerator generator, boolean scannable) {
        if (invalid) return;
        validate();
        generator.json(depositSpecPath(), depositSpecJson(scannable));
        generator.json(processorPath(), processorJson());
        generator.json(structurePath(), structureJson());
    }

    void generateLang(LangEventJS lang) {
        if (invalid) return;
        validate();
        if (displayName != null) {
            lang.add(id.getNamespace(), DepositData.structureLangKey(id), displayName);
        }
    }

    ResourceLocation id() {
        return id;
    }

    ResourceLocation dimension() {
        return dimension;
    }

    void setDimension(ResourceLocation dimension) {
        this.dimension = dimension;
    }

    Integer structureSetWeight() {
        return structureSetWeight;
    }

    boolean weightChanged() {
        return weightChanged;
    }

    boolean defaultScannable() {
        if (builtInStructure == null) return true;
        return builtInStructure.isScannable().get();
    }

    boolean isTweak() {
        return builtInStructure != null;
    }

    boolean isInvalid() {
        return invalid;
    }

    boolean validateForKubeJS() {
        var error = validationError(true);
        if (error == null) return true;

        invalid = true;
        ConsoleJS.STARTUP.error(error);
        return false;
    }

    ResourceLocation blockId() {
        if (blockId == null) {
            throw new IllegalStateException("Deposit structure " + id + " must specify a deposit block");
        }

        return blockId;
    }

    private ResourceLocation depositSpecPath() {
        var specId = builtInStructure != null ? builtInStructure.specId() : DepositData.specId(id);
        return Utils.resourceFileLocation(RNSRegistries.DEPOSIT_SPEC, specId);
    }

    private ResourceLocation processorId() {
        return DepositData.processorId(id.getNamespace(), blockId());
    }

    private ResourceLocation processorPath() {
        return Utils.resourceFileLocation(Registries.PROCESSOR_LIST, processorId());
    }

    private ResourceLocation structurePath() {
        return Utils.resourceFileLocation(Registries.STRUCTURE, id);
    }

    private void validate() {
        var error = validationError(false);
        if (error != null) throw new IllegalStateException(error);
    }

    private @Nullable String validationError(boolean validateItems) {
        if (blockId == null) return "Deposit structure " + id + " must specify a deposit block";
        if (scannerIconCandidates.isEmpty()) {
            return "Deposit structure " + id + " must specify at least one scanner icon";
        }
        if (validateItems && !hasResolvableItem(scannerIconCandidates)) {
            return "None of the scanner icon items exist for deposit structure " + id + ": "
                    + scannerIconCandidates;
        }
        if (weightedTemplates.isEmpty()) {
            return "Deposit structure " + id + " must specify at least one nbt template";
        }
        if (validateItems && !mapIconCandidates.isEmpty() && !hasResolvableItem(mapIconCandidates)) {
            return "None of the map icon items exist for deposit structure " + id + ": " + mapIconCandidates;
        }
        return null;
    }

    private JsonObject depositSpecJson(boolean scannable) {
        var json = new JsonObject();
        json.add("scanner_icon_item", stringOrArray(scannerIconCandidates));
        json.add("map_icon_item", stringOrArray(mapIconCandidates.isEmpty()
                ? List.of(blockId.toString())
                : mapIconCandidates));
        json.addProperty("structure", id.toString());
        if (!isTweak() || !dimension.equals(DumpSafeOverrides.OVERWORLD)) {
            json.addProperty("dimension", dimension.toString());
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

        inputPredicate.addProperty("block", DepositData.PROCESSOR_INPUT_BLOCK.toString());
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
        json.addProperty("biomes", biomeSelector != null
                ? biomeSelector
                : "#" + DepositData.biomeTagId(CreateRNS.ID, dimension));
        json.addProperty("placement_strategy", (builtInStructure != null ? builtInStructure.placementStrategy()
                : DepositData.defaultPlacementStrategy(dimension)).getSerializedName());
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
        var preset = dimension.equals(DumpSafeOverrides.NETHER)
                ? DepositData.PRESET_NETHER_COMMON
                : DepositData.PRESET_OVERWORLD_COMMON;
        return -preset.depth();
    }

    private static JsonElement stringOrArray(List<String> values) {
        if (values.size() == 1) return new JsonPrimitive(values.get(0));

        var array = new JsonArray();
        for (var value : values) {
            array.add(value);
        }
        return array;
    }

    private static void validateItemCandidate(String candidateId) {
        if (candidateId.isBlank()) throw new IllegalArgumentException("Item or item tag id cannot be blank");

        var id = candidateId.startsWith("#") ? candidateId.substring(1) : candidateId;
        if (id.isBlank() || ResourceLocation.tryParse(id) == null) {
            throw new IllegalArgumentException("Invalid item or item tag id: " + candidateId);
        }
    }

    private static boolean hasResolvableItem(List<String> candidateIds) {
        for (var candidateId : candidateIds) {
            if (candidateId.startsWith("#")) return true;

            var item = ForgeRegistries.ITEMS.getValue(ResourceLocation.parse(candidateId));
            if (item != null && item != Items.AIR) return true;
        }
        return false;
    }

    private record WeightedTemplate(ResourceLocation template, int weight) {
    }
}
