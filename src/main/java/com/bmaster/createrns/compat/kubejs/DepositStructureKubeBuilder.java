package com.bmaster.createrns.compat.kubejs;

import com.bmaster.createrns.data.pack.DepositDimension;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dev.latvian.mods.kubejs.client.LangKubeEvent;
import dev.latvian.mods.kubejs.generator.KubeDataGenerator;
import dev.latvian.mods.kubejs.typings.Info;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class DepositStructureKubeBuilder {
    private static final ResourceLocation DEFAULT_PROCESSOR_INPUT_BLOCK = ResourceLocation.withDefaultNamespace("end_stone");
    private static final ResourceLocation DEFAULT_TEMPLATE_SMALL =
            ResourceLocation.fromNamespaceAndPath("create_rns", "ore_deposit_small");
    private static final ResourceLocation DEFAULT_TEMPLATE_MEDIUM =
            ResourceLocation.fromNamespaceAndPath("create_rns", "ore_deposit_medium");
    private static final ResourceLocation DEFAULT_TEMPLATE_LARGE =
            ResourceLocation.fromNamespaceAndPath("create_rns", "ore_deposit_large");
    private static final int OVERWORLD_COMMON_HEIGHT = -8;
    private static final int OVERWORLD_UNCOMMON_HEIGHT = -10;
    private static final int OVERWORLD_RARE_HEIGHT = -12;
    private static final int NETHER_HEIGHT = -4;
    private static final int COMMON_STRUCTURE_SET_WEIGHT = 50;
    private static final int UNCOMMON_STRUCTURE_SET_WEIGHT = 35;
    private static final int RARE_STRUCTURE_SET_WEIGHT = 20;

    private final ResourceLocation id;
    private final List<WeightedTemplate> weightedTemplates = new ArrayList<>();
    private final List<String> scannerIconCandidates = new ArrayList<>();
    private final List<String> mapIconCandidates = new ArrayList<>();

    private ResourceLocation blockId;
    private DepositDimension dimension = DepositDimension.OVERWORLD;
    private Integer height;
    private Integer structureSetWeight;
    private String displayName;

    public DepositStructureKubeBuilder(ResourceLocation id) {
        this.id = id;
    }

    @Info("Sets the deposit block used by this deposit structure.")
    public DepositStructureKubeBuilder block(String blockId) {
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
        return this;
    }

    @Info("""
            Sets the weight used when this deposit structure is selected in StartupEvents.createRnsStructureSet.
            Preset structures already assign a default.
            """)
    public DepositStructureKubeBuilder weight(int value) {
        if (value <= 0) throw new IllegalArgumentException("Structure set weight must be positive");

        structureSetWeight = value;
        return this;
    }

    @Info("""
            Uses the specified item or item tag as an icon in deposit scanner for the respective deposit.
            If tag is specified, the first item with that tag is picked.
            Can be called multiple times to add more items as fallbacks in case the original item does not exist.
            """)
    public DepositStructureKubeBuilder scannerIcon(String candidateId) {
        scannerIconCandidates.add(candidateId);
        return this;
    }

    @Info("""
            Adds all metal-related tags as scanner icon candidates.
            The first item of a first non-empty tag is selected.
            """)
    public DepositStructureKubeBuilder scannerIconMetal(String material) {
        return scannerIcon("#c:raw_materials/" + material)
                .scannerIcon("#c:ores/" + material)
                .scannerIcon("#c:ingots/" + material)
                .scannerIcon("#c:nuggets/" + material);
    }

    @Info("""
            Adds all gem-related tags as scanner icon candidates.
            The first item of a first non-empty tag is selected.
            """)
    public DepositStructureKubeBuilder scannerIconGem(String material) {
        return scannerIcon("#c:gems/" + material);
    }

    @Info("""
            Adds all dust-related tags as scanner icon candidates.
            The first item of a first non-empty tag is selected.
            """)
    public DepositStructureKubeBuilder scannerIconDust(String material) {
        return scannerIcon("#c:dusts/" + material);
    }

    @Info("""
            Uses the specified item or item tag as a map icon for a found deposit.
            Can be called multiple times to add more items as fallbacks in case the original item does not exist.
            """)
    public DepositStructureKubeBuilder mapIcon(String candidateId) {
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

        weightedTemplates.add(new WeightedTemplate(ResourceLocation.parse(templateId), weight));
        return this;
    }

    @Info("""
            Applies a preset that covers height, weighted nbts, and the default structure set weight.
            These presets are used by the default deposits (iron - common, nickel - uncommon, redstone - precious).
            Supported values: overworld_common, overworld_uncommon, overworld_rare, nether_common, nether_uncommon, nether_rare.
            """)
    public DepositStructureKubeBuilder preset(String presetId) {
        weightedTemplates.clear();

        return switch (presetId) {
            case "overworld_common" -> weight(COMMON_STRUCTURE_SET_WEIGHT)
                    .height(OVERWORLD_COMMON_HEIGHT)
                    .commonTemplates();
            case "overworld_uncommon" -> weight(UNCOMMON_STRUCTURE_SET_WEIGHT)
                    .height(OVERWORLD_UNCOMMON_HEIGHT)
                    .uncommonTemplates();
            case "overworld_rare" -> weight(RARE_STRUCTURE_SET_WEIGHT)
                    .height(OVERWORLD_RARE_HEIGHT)
                    .rareTemplates();
            case "nether_common" -> weight(COMMON_STRUCTURE_SET_WEIGHT)
                    .height(NETHER_HEIGHT)
                    .commonTemplates();
            case "nether_uncommon" -> weight(UNCOMMON_STRUCTURE_SET_WEIGHT)
                    .height(NETHER_HEIGHT)
                    .uncommonTemplates();
            case "nether_rare" -> weight(RARE_STRUCTURE_SET_WEIGHT)
                    .height(NETHER_HEIGHT)
                    .rareTemplates();
            default -> throw new IllegalArgumentException("Unknown deposit preset: " + presetId);
        };
    }

    void generateData(KubeDataGenerator generator) {
        validate();
        generator.json(depositSpecPath(), depositSpecJson());
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

    private ResourceLocation depositSpecId() {
        return ResourceLocation.fromNamespaceAndPath(id.getNamespace(), normalizedStructureName());
    }

    private ResourceLocation depositSpecPath() {
        var specId = depositSpecId();
        return ResourceLocation.fromNamespaceAndPath(specId.getNamespace(), "create_rns/deposit_spec/" + specId.getPath());
    }

    private ResourceLocation processorId() {
        return ResourceLocation.fromNamespaceAndPath(id.getNamespace(),
                "replace_with_" + id.getNamespace() + "_" + id.getPath().replace('/', '_'));
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

    private JsonObject depositSpecJson() {
        var json = new JsonObject();
        json.add("scanner_icon_item", stringOrArray(scannerIconCandidates));
        json.add("map_icon_item", stringOrArray(mapIconCandidates.isEmpty()
                ? List.of(blockId.toString())
                : mapIconCandidates));
        json.addProperty("structure", id.toString());
        json.addProperty("dimension", dimension.levelDimension().location().toString());
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

        inputPredicate.addProperty("block", DEFAULT_PROCESSOR_INPUT_BLOCK.toString());
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
        json.addProperty("height", resolvedHeight());
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
        return dimension == DepositDimension.NETHER ? NETHER_HEIGHT : OVERWORLD_COMMON_HEIGHT;
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

    private DepositStructureKubeBuilder nbt(ResourceLocation templateId, int weight) {
        return nbt(templateId.toString(), weight);
    }

    private DepositStructureKubeBuilder commonTemplates() {
        return nbt(DEFAULT_TEMPLATE_MEDIUM, 70)
                .nbt(DEFAULT_TEMPLATE_LARGE, 30);
    }

    private DepositStructureKubeBuilder uncommonTemplates() {
        return nbt(DEFAULT_TEMPLATE_SMALL, 30)
                .nbt(DEFAULT_TEMPLATE_MEDIUM, 60)
                .nbt(DEFAULT_TEMPLATE_LARGE, 10);
    }

    private DepositStructureKubeBuilder rareTemplates() {
        return nbt(DEFAULT_TEMPLATE_SMALL, 70)
                .nbt(DEFAULT_TEMPLATE_MEDIUM, 28)
                .nbt(DEFAULT_TEMPLATE_LARGE, 2);
    }

    private record WeightedTemplate(ResourceLocation template, int weight) {
    }
}
