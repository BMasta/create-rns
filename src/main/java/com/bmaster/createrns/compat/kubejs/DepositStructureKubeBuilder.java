package com.bmaster.createrns.compat.kubejs;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.RNSRegistries;
import com.bmaster.createrns.data.deposit.DepositData;
import com.bmaster.createrns.data.deposit.DepositData.WeightedTemplate;
import com.bmaster.createrns.data.deposit.DepositJson;
import com.bmaster.createrns.util.Utils;
import com.bmaster.createrns.util.codec.ResourceOrTagLocation;
import com.google.gson.JsonObject;
import dev.latvian.mods.kubejs.client.LangKubeEvent;
import dev.latvian.mods.kubejs.generator.KubeDataGenerator;
import dev.latvian.mods.kubejs.script.SourceLine;
import dev.latvian.mods.kubejs.typings.Info;
import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.util.HideFromJS;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class DepositStructureKubeBuilder extends RNSStartupKubeBuilder {
    private static final Set<ResourceLocation> VANILLA_BIOMES = VanillaRegistries.createLookup()
            .lookupOrThrow(Registries.BIOME)
            .listElementIds()
            .map(ResourceKey::location)
            .collect(Collectors.toUnmodifiableSet());

    private final ResourceLocation id;
    private final @Nullable DepositData.StructureEntry builtInStructure;
    private final List<WeightedTemplate> weightedTemplates = new ArrayList<>();
    private final List<ResourceOrTagLocation> scannerIconCandidates = new ArrayList<>();
    private final List<ResourceOrTagLocation> mapIconCandidates = new ArrayList<>();

    private ResourceLocation blockId;
    private ResourceKey<Level> dimension = Level.OVERWORLD;
    private @Nullable ResourceOrTagLocation biomeSelector;
    private Integer height;
    private int heightDeviation;
    private Integer structureSetWeight;
    private String displayName;
    private boolean scannerIconsInherited;
    private boolean mapIconsInherited;
    private boolean templatesInherited;
    private boolean weightChanged;
    private SourceLine scannerIconSourceLine = SourceLine.UNKNOWN;
    private SourceLine mapIconSourceLine = SourceLine.UNKNOWN;

    public DepositStructureKubeBuilder(ResourceLocation id) {
        this(id, SourceLine.UNKNOWN);
    }

    DepositStructureKubeBuilder(ResourceLocation id, SourceLine sourceLine) {
        super(sourceLine, "rnsDepositStructures", "create|tweak");
        this.id = id;
        builtInStructure = null;
    }

    DepositStructureKubeBuilder(DepositData.StructureEntry structureEntry) {
        this(structureEntry, SourceLine.UNKNOWN);
    }

    DepositStructureKubeBuilder(DepositData.StructureEntry structureEntry, SourceLine sourceLine) {
        super(sourceLine, "rnsDepositStructures", "create|tweak");
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
    public DepositStructureKubeBuilder block(Context cx, String blockId) {
        return sourced(cx, "block", () -> {
            var result = block(blockId);
            if (!BuiltInRegistries.BLOCK.containsKey(this.blockId)) {
                throw new IllegalArgumentException("Unknown block: " + blockId);
            }
            return result;
        });
    }

    @HideFromJS
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

    @Info("Sets the Y detectionOffset from the world surface for the deposit.")
    public DepositStructureKubeBuilder height(int value) {
        height = value;
        heightDeviation = 0;
        return this;
    }

    @Info("Sets the biome or biome tag in which this deposit structure may generate.")
    public DepositStructureKubeBuilder biome(Context cx, String biomeOrTagId) {
        return sourced(cx, "biome", () -> biome(biomeOrTagId));
    }

    @HideFromJS
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

        biomeSelector = new ResourceOrTagLocation(id, isTag);
        return this;
    }

    @Info("""
            Sets the weight used when this deposit structure is selected in StartupEvents.rnsEnableDeposits.
            Preset structures already assign a default.
            """)
    public DepositStructureKubeBuilder weight(Context cx, int value) {
        return sourced(cx, "weight", () -> weight(value));
    }

    @HideFromJS
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
    public DepositStructureKubeBuilder scannerIcon(Context cx, String candidateId) {
        scannerIconSourceLine = methodSource(cx, "scannerIcon");
        return sourced(cx, "scannerIcon", () -> scannerIcon(candidateId));
    }

    @HideFromJS
    public DepositStructureKubeBuilder scannerIcon(String candidateId) {
        return scannerIcon(ResourceOrTagLocation.parse(candidateId));
    }

    private DepositStructureKubeBuilder scannerIcon(ResourceOrTagLocation candidate) {
        if (scannerIconsInherited) {
            scannerIconCandidates.clear();
            scannerIconsInherited = false;
        }
        scannerIconCandidates.add(candidate);
        return this;
    }

    @Info("""
            Adds all metal-related tags as scanner icon candidates.
            The first item of a first non-empty tag is selected.
            """)
    public DepositStructureKubeBuilder scannerIconMetal(Context cx, String material) {
        scannerIconSourceLine = methodSource(cx, "scannerIconMetal");
        return sourced(cx, "scannerIconMetal", () -> scannerIconMetal(material));
    }

    @HideFromJS
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
    public DepositStructureKubeBuilder scannerIconGem(Context cx, String material) {
        scannerIconSourceLine = methodSource(cx, "scannerIconGem");
        return sourced(cx, "scannerIconGem", () -> scannerIconGem(material));
    }

    @HideFromJS
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
    public DepositStructureKubeBuilder scannerIconDust(Context cx, String material) {
        scannerIconSourceLine = methodSource(cx, "scannerIconDust");
        return sourced(cx, "scannerIconDust", () -> scannerIconDust(material));
    }

    @HideFromJS
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
    public DepositStructureKubeBuilder mapIcon(Context cx, String candidateId) {
        mapIconSourceLine = methodSource(cx, "mapIcon");
        return sourced(cx, "mapIcon", () -> mapIcon(candidateId));
    }

    @HideFromJS
    public DepositStructureKubeBuilder mapIcon(String candidateId) {
        var candidate = ResourceOrTagLocation.parse(candidateId);
        if (mapIconsInherited) {
            mapIconCandidates.clear();
            mapIconsInherited = false;
        }
        mapIconCandidates.add(candidate);
        return this;
    }

    @Info("""
            Adds a structure nbt template to the generated deposit.
            Can be called multiple times to specify more templates.
            When multiple templates are specified, the chance of picking one is determined by the weight.
            """)
    public DepositStructureKubeBuilder nbt(Context cx, String templateId, int weight) {
        return sourced(cx, "nbt", () -> nbt(templateId, weight));
    }

    @HideFromJS
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
    public DepositStructureKubeBuilder preset(Context cx, String presetId) {
        return sourced(cx, "preset", () -> preset(presetId));
    }

    @HideFromJS
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

    void generateData(KubeDataGenerator generator) {
        generateData(generator, true);
    }

    void generateData(KubeDataGenerator generator, boolean scannable) {
        if (!validate()) return;
        generator.json(depositSpecPath(), depositSpecJson(scannable));
        generator.json(processorPath(), DepositJson.processorList(DepositData.PROCESSOR_INPUT_BLOCK, blockId()));
        generator.json(structurePath(), structureJson());
    }

    void generateLang(LangKubeEvent lang) {
        if (!validate()) return;
        if (displayName != null) {
            lang.add(id.getNamespace(), DepositData.structureLangKey(id), displayName);
        }
    }

    ResourceLocation id() {
        return id;
    }

    ResourceKey<Level> dimension() {
        return dimension;
    }

    void setDimension(ResourceKey<Level> dimension) {
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

    boolean validateForAvailability() {
        if (!hasKnownCreationSource()) return true;
        return validate();
    }

    private boolean validate() {
        if (isInvalid()) return false;
        if (blockId == null) {
            return reportDeferredError("Deposit structure " + id + " must specify a deposit block");
        }

        if (scannerIconCandidates.isEmpty()) {
            return reportDeferredError("Deposit structure " + id + " must specify at least one scanner icon");
        }
        if (hasKnownCreationSource() && !hasResolvableItem(scannerIconCandidates)) {
            return reportDeferredError(
                    "None of the scanner icon items exist for deposit structure " + id + ": " + scannerIconCandidates,
                    scannerIconSourceLine);
        }

        if (weightedTemplates.isEmpty()) {
            return reportDeferredError("Deposit structure " + id + " must specify at least one nbt template");
        }
        if (hasKnownCreationSource() && !mapIconCandidates.isEmpty() && !hasResolvableItem(mapIconCandidates)) {
            return reportDeferredError(
                    "None of the map icon items exist for deposit structure " + id + ": " + mapIconCandidates,
                    mapIconSourceLine);
        }
        return true;
    }

    private JsonObject depositSpecJson(boolean scannable) {
        var mapIcons = mapIconCandidates.isEmpty()
                ? List.of(new ResourceOrTagLocation(blockId(), false))
                : mapIconCandidates;
        return DepositJson.spec(id, dimension, scannerIconCandidates, mapIcons, scannable);
    }

    private JsonObject structureJson() {
        var biomes = biomeSelector != null
                ? biomeSelector
                : new ResourceOrTagLocation(DepositData.biomeTagId(CreateRNS.ID, dimension), true);
        var placement = builtInStructure != null
                ? builtInStructure.placementStrategy()
                : DepositData.defaultPlacementStrategy(dimension);

        var preset = dimension.equals(Level.NETHER)
                ? DepositData.PRESET_NETHER_COMMON
                : DepositData.PRESET_OVERWORLD_COMMON;
        int resolvedHeight = height != null ? height : -preset.depth();

        return DepositJson.structure(biomes, placement, resolvedHeight - heightDeviation,
                resolvedHeight + heightDeviation, weightedTemplates, processorId());
    }

    private static boolean hasResolvableItem(List<ResourceOrTagLocation> candidates) {
        for (var candidate : candidates) {
            if (candidate.isTag()) return true;

            var item = BuiltInRegistries.ITEM.getOptional(candidate.id()).orElse(null);
            if (item != null && item != Items.AIR) return true;
        }
        return false;
    }
}
