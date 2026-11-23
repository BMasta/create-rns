package com.bmaster.createrns.compat.kubejs.deposit.structures;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.compat.kubejs.util.JSMethod;
import com.bmaster.createrns.compat.kubejs.util.StartupKubeBuilder;
import com.bmaster.createrns.content.deposit.worldgen.PlacementStrategy;
import com.bmaster.createrns.data.deposit.DepositResources;
import com.bmaster.createrns.data.deposit.DepositResources.WeightedTemplate;
import com.bmaster.createrns.util.Utils;
import com.bmaster.createrns.util.codec.ResourceOrTagLocation;
import dev.latvian.mods.kubejs.typings.Info;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public abstract class StructureKubeBuilder<B extends StructureKubeBuilder<B>> extends StartupKubeBuilder {
    private final ResourceLocation id;
    private final List<WeightedTemplate> weightedTemplates = new ArrayList<>();
    private final List<ResourceOrTagLocation> scannerIconCandidates = new ArrayList<>();
    private final List<ResourceOrTagLocation> mapIconCandidates = new ArrayList<>();

    protected @Nullable ResourceLocation blockId;
    protected ResourceKey<Level> dimension = Level.OVERWORLD;
    protected @Nullable Integer configuredWeight;
    private @Nullable ResourceOrTagLocation biomeSelector;
    private Integer height;
    private int heightDeviation;
    private String displayName;
    private boolean scannerIconsInherited;
    private boolean mapIconsInherited;
    private boolean templatesInherited;

    protected StructureKubeBuilder(ResourceLocation id) {
        super("rnsDepositStructures", "create|tweak");
        this.id = id;
    }

    protected StructureKubeBuilder(DepositResources.Structure structure, DepositResources.Spec spec) {
        this(structure.id());
        dimension = structure.dimension();
        biomeSelector = structure.biomeSelector();
        height = -structure.depth();
        heightDeviation = structure.depthDeviation();
        scannerIconCandidates.addAll(spec.scannerIconCandidates());
        mapIconCandidates.addAll(spec.mapIconCandidates());
        weightedTemplates.addAll(structure.templates());
        scannerIconsInherited = true;
        mapIconsInherited = true;
        templatesInherited = true;
    }

    @Info("""
            Sets the structure name shown by the deposit scanner.
            If omitted, make sure to supply the matching `<namespace>.structure.<path>` lang entry yourself.
            """)
    @JSMethod
    public B displayName(String name) {
        displayName = name;
        return self();
    }

    @Info("Sets the Y offset from the world surface for the deposit.")
    @JSMethod
    public B height(int value) {
        height = value;
        heightDeviation = 0;
        return self();
    }

    @Info("""
            Sets the biome or biome tag in which the deposit structure may generate.
            Only one biome or biome tag can be specified. Unless you need a structure-specific biome selector,
            prefer using biome() from rnsEnableDeposits, which allows multiple biomes or biome tags.
            """)
    @JSMethod
    public B biome(String biomeOrTagId) {
        biomeSelector = Utils.parseBiome(biomeOrTagId);
        return self();
    }

    @Info("""
            When generating a deposit, the mod first picks a spot where it should be generated,
            and then decides which deposit type should be placed.
            Deposit structures with a higher weight are selected more often.
            More formally, assuming 3 total structures with weight w1, w2, w3,
            the chance of selecting structure 1 is w1/(w1+w2+w3)
            """)
    @JSMethod
    public B weight(int value) {
        if (value <= 0) {
            error("Structure set weight must be positive", true);
            return self();
        }

        configuredWeight = value;
        return self();
    }

    @Info("""
            Uses the specified item or item tag as an icon in deposit scanner for the respective deposit.
            If tag is specified, the first item with that tag is picked.
            Can be called multiple times to add more items as fallbacks in case the original item does not exist.
            """)
    @JSMethod
    public B scannerIcon(String candidateId) {
        return scannerIcon(ResourceOrTagLocation.parse(candidateId));
    }

    private B scannerIcon(ResourceOrTagLocation candidate) {
        if (scannerIconsInherited) {
            scannerIconCandidates.clear();
            scannerIconsInherited = false;
        }
        scannerIconCandidates.add(candidate);
        return self();
    }

    @Info("""
            Shortcut that expands to:
            .scannerIcon("#c:raw_materials/<material>")
            .scannerIcon("#c:ores/<material>")
            .scannerIcon("#c:ingots/<material>")
            .scannerIcon("#c:nuggets/<material>")
            """)
    @JSMethod
    public B scannerIconMetal(String material) {
        for (var candidate : DepositResources.metalScannerIconCandidates(material)) {
            scannerIcon(candidate);
        }
        return self();
    }

    @Info("""
            Shortcut that expands to:
            .scannerIcon("#c:gems/<material>")
            """)
    @JSMethod
    public B scannerIconGem(String material) {
        for (var candidate : DepositResources.gemScannerIconCandidates(material)) {
            scannerIcon(candidate);
        }
        return self();
    }

    @Info("""
            Shortcut that expands to:
            .scannerIcon("#c:dusts/<material>")
            """)
    @JSMethod
    public B scannerIconDust(String material) {
        for (var candidate : DepositResources.dustScannerIconCandidates(material)) {
            scannerIcon(candidate);
        }
        return self();
    }

    @Info("""
            Uses the specified item or item tag as a map icon for a found deposit.
            Only relevant if Xaero's Map or JourneyMap is installed.
            Can be called multiple times to add more items as fallbacks in case the original item does not exist.
            """)
    @JSMethod
    public B mapIcon(String candidateId) {
        var candidate = ResourceOrTagLocation.parse(candidateId);
        if (mapIconsInherited) {
            mapIconCandidates.clear();
            mapIconsInherited = false;
        }
        mapIconCandidates.add(candidate);
        return self();
    }

    @Info("""
            Adds a structure nbt template to the generated deposit.
            Can be called multiple times to add more templates.
            When multiple templates are added, the chance of picking one is determined by the weight.
            """)
    @JSMethod
    public B nbt(String templateId, int weight) {
        if (weight <= 0) {
            error("Template weight must be positive", true);
            return self();
        }
        var template = ResourceLocation.parse(templateId);

        if (templatesInherited) {
            weightedTemplates.clear();
            templatesInherited = false;
        }
        weightedTemplates.add(new WeightedTemplate(template, weight));
        return self();
    }

    @Info("""
            Applies a preset that covers height, weighted nbts, and the default structure set weight.
            These presets are used by the default deposits (iron - common, nickel - uncommon, redstone - precious).
            Supported values: overworld_common, overworld_uncommon, overworld_rare,
            nether_common, nether_uncommon, nether_rare, end_common, end_uncommon, end_rare
            """)
    @JSMethod
    public B preset(String presetId) {
        var preset = DepositResources.getStructurePreset(presetId);
        weightedTemplates.clear();
        templatesInherited = false;
        weight(preset.weight());
        height(-preset.depth());
        heightDeviation = preset.depthDeviation();
        for (var template : preset.templates()) {
            nbt(template.template().toString(), template.weight());
        }
        return self();
    }

    public void gatherResources(DepositResources resources, boolean scannable) {
        if (!isValid()) return;
        var mapIcons = mapIconCandidates.isEmpty()
                ? List.of(new ResourceOrTagLocation(blockId(), false)) : mapIconCandidates;
        resources.specs.add(new DepositResources.Spec(specId(), id, dimension, () -> scannable,
                scannerIconCandidates, mapIcons));

        var processor = new DepositResources.Processor(processorId(), blockId());
        if (!resources.processors.contains(processor)) resources.processors.add(processor);
        var biomes = biomeSelector != null
                ? biomeSelector : new ResourceOrTagLocation(DepositResources.biomeTagId(CreateRNS.ID, dimension), true);
        var preset = dimension.equals(Level.NETHER)
                ? DepositResources.PRESET_NETHER_COMMON : DepositResources.PRESET_OVERWORLD_COMMON;
        int resolvedHeight = height != null ? height : -preset.depth();
        resources.structures.add(new DepositResources.Structure(id, processor.id(), dimension, biomes,
                -resolvedHeight, heightDeviation, structureSetWeight(), placementStrategy(), weightedTemplates));
        if (displayName != null) {
            resources.langEntries.add(new DepositResources.LangEntry(
                    id.getNamespace(), DepositResources.structureLangKey(id), displayName));
        }
    }

    boolean hasBiome() {
        return biomeSelector != null;
    }

    public ResourceLocation id() {
        return id;
    }

    public ResourceKey<Level> dimension() {
        return dimension;
    }

    @Nullable
    public Integer structureSetWeight() {
        return configuredWeight;
    }

    public ResourceLocation blockId() {
        if (blockId == null) {
            throw new IllegalStateException("Deposit structure " + id + " must specify a deposit block");
        }

        return blockId;
    }

    protected abstract B self();

    protected ResourceLocation specId() {
        return DepositResources.specId(id);
    }

    protected abstract PlacementStrategy placementStrategy();

    protected ResourceLocation processorId() {
        return DepositResources.processorId(id.getNamespace(), blockId());
    }

    @Override
    protected void assertValid() {
        if (blockId == null) {
            error("Deposit structure " + id + " must specify a deposit block", true);
        }

        if (scannerIconCandidates.isEmpty()) {
            error("Deposit structure " + id + " must specify at least one scanner icon", true);
        } else if (!hasResolvableItem(scannerIconCandidates)) {
            error("None of the scanner icon items exist for deposit structure " + id + ": " + scannerIconCandidates, true);
        }

        if (weightedTemplates.isEmpty()) {
            error("Deposit structure " + id + " must specify at least one nbt template", true);
        }
        if (!mapIconCandidates.isEmpty() && !hasResolvableItem(mapIconCandidates)) {
            error("None of the map icon items exist for deposit structure " + id + ": " + mapIconCandidates, true);
        }
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
