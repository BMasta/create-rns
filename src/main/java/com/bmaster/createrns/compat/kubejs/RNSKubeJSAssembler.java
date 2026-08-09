package com.bmaster.createrns.compat.kubejs;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.RNSPacks;
import com.bmaster.createrns.data.pack.DepositBlockBuilder.DepositBuildingContext;
import com.bmaster.createrns.data.pack.DepositDimension;
import com.bmaster.createrns.data.pack.DepositSpecBuilder;
import com.bmaster.createrns.data.pack.DepositStructureBuilder;
import com.bmaster.createrns.data.pack.DynamicDatapackContent;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.latvian.mods.kubejs.client.LangEventJS;
import dev.latvian.mods.kubejs.generator.DataJsonGenerator;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class RNSKubeJSAssembler {
    private static final Object CURRENT_ASSEMBLER_LOCK = new Object();
    private static final Object DEPOSIT_SELECTION_LOCK = new Object();
    private static final Set<ResourceLocation> DEFAULT_ENABLED_DEPOSIT_BLOCKS =
            Set.of(new DepositBuildingContext("depleted").depositBlockId());

    private static @Nullable RNSKubeJSAssembler cachedCurrentAssembler;
    private static @Nullable DepositSelectionState cachedDepositSelection;

    private final Supplier<List<CatalystKubeBuilder>> customCatalysts;
    private final Supplier<List<DepositStructureKubeBuilder>> allCustomStructures;
    private final Supplier<List<DepositStructureKubeBuilder>> allTweakedStructures;
    private final @Nullable EnableDepositsKubeEvent structureSetEvent;

    public RNSKubeJSAssembler(
            Supplier<List<CatalystKubeBuilder>> customCatalysts,
            Supplier<List<DepositStructureKubeBuilder>> allCustomStructures,
            Supplier<List<DepositStructureKubeBuilder>> allTweakedStructures,
            @Nullable EnableDepositsKubeEvent structureSetEvent
    ) {
        this.customCatalysts = customCatalysts;
        this.allCustomStructures = allCustomStructures;
        this.allTweakedStructures = allTweakedStructures;
        this.structureSetEvent = structureSetEvent;
    }

    public static boolean isManagingDeposits() {
        return currentDepositSelection().managing();
    }

    public static Set<ResourceLocation> getEnabledDepositBlocks() {
        return currentDepositSelection().enabledDepositBlocks();
    }

    public static Set<ResourceLocation> getSelectedStructureIds() {
        return currentDepositSelection().selectedStructureIds();
    }

    public static RNSKubeJSAssembler fromCurrentEvents() {
        synchronized (CURRENT_ASSEMBLER_LOCK) {
            if (cachedCurrentAssembler == null) {
                var customCatalystEvent = currentCatalystEvent();
                var customStructureEvent = currentDepositStructureEvent();
                var structureSetEvent = currentStructureSetEvent(customStructureEvent);
                cachedCurrentAssembler = new RNSKubeJSAssembler(customCatalystEvent::created,
                        customStructureEvent::created, customStructureEvent::tweaked, structureSetEvent);
            }
            return cachedCurrentAssembler;
        }
    }

    static void resetCaches() {
        synchronized (CURRENT_ASSEMBLER_LOCK) {
            cachedCurrentAssembler = null;
        }
        synchronized (DEPOSIT_SELECTION_LOCK) {
            cachedDepositSelection = null;
        }
    }

    public void generateData(DataJsonGenerator generator) {
        var customCatalysts = List.copyOf(this.customCatalysts.get());
        for (var catalyst : customCatalysts) {
            catalyst.generateData(generator);
        }
        generateMinerAttachmentTag(generator, customCatalysts);

        var allCustomStructures = List.copyOf(this.allCustomStructures.get());
        var allTweakedStructures = this.allTweakedStructures.get().stream()
                .filter(structure -> !structure.isInvalid())
                .toList();
        var customDepositStructures = customDepositStructuresById(allCustomStructures);
        var tweakedDepositStructures = tweakedDepositStructuresById(allTweakedStructures);
        var assignedDimensions = new HashMap<ResourceLocation, DepositDimension>();
        var overworldStructureSet = structureSetEvent != null ? structureSetEvent.configuredOverworld() : null;
        applyStructureSetDimension(overworldStructureSet,
                DepositDimension.OVERWORLD, customDepositStructures, assignedDimensions);
        var netherStructureSet = structureSetEvent != null ? structureSetEvent.configuredNether() : null;
        applyStructureSetDimension(netherStructureSet,
                DepositDimension.NETHER, customDepositStructures, assignedDimensions);

        var selectedCustomStructures = selectedCustomStructures(
                overworldStructureSet, netherStructureSet, customDepositStructures);
        for (var structure : selectedCustomStructures) {
            structure.generateData(generator);
        }

        var selectedStructureIds = selectedStructureIds(overworldStructureSet, netherStructureSet);
        var selectedStructureIdSet = Set.copyOf(selectedStructureIds);
        for (var structure : allTweakedStructures) {
            var dimensionConfigured = structure.dimension() == DepositDimension.OVERWORLD
                    ? overworldStructureSet != null
                    : netherStructureSet != null;
            var scannable = dimensionConfigured
                    ? selectedStructureIdSet.contains(structure.id())
                    : structure.defaultScannable();
            structure.generateData(generator, scannable);
        }

        generateBuiltInDepositSpecOverrides(
                generator, overworldStructureSet, netherStructureSet, tweakedDepositStructures.keySet());

        generateStructureSet(generator, overworldStructureSet,
                RNSPacks.DEFAULT_SEPARATION, RNSPacks.DEFAULT_SPACING, RNSPacks.SALT);
        generateStructureSet(generator, netherStructureSet,
                RNSPacks.DEFAULT_NETHER_SEPARATION, RNSPacks.DEFAULT_NETHER_SPACING, RNSPacks.NETHER_SALT);
        generateDefaultStructureSetWeightOverrides(
                generator, allTweakedStructures, overworldStructureSet, netherStructureSet);

        if (structureSetEvent == null || !structureSetEvent.hasConfiguredDimensions()) return;

        var values = new JsonArray();
        for (var structureId : selectedStructureIds) {
            values.add(structureId.toString());
        }

        var json = new JsonObject();
        json.addProperty("replace", true);
        json.add("values", values);
        generator.json(CreateRNS.asResource("tags/worldgen/structure/deposits"), json);
    }

    public void generateLang(LangEventJS event) {
        var customCatalysts = List.copyOf(this.customCatalysts.get());
        for (var catalyst : customCatalysts) {
            catalyst.generateLang(event);
        }

        for (var structure : List.copyOf(this.allTweakedStructures.get())) {
            structure.generateLang(event);
        }

        if (structureSetEvent == null || !structureSetEvent.hasConfiguredDimensions()) return;

        var allCustomStructures = List.copyOf(this.allCustomStructures.get());
        var customDepositStructures = customDepositStructuresById(allCustomStructures);
        var assignedDimensions = new HashMap<ResourceLocation, DepositDimension>();
        applyStructureSetDimension(structureSetEvent.configuredOverworld(),
                DepositDimension.OVERWORLD, customDepositStructures, assignedDimensions);
        applyStructureSetDimension(structureSetEvent.configuredNether(),
                DepositDimension.NETHER, customDepositStructures, assignedDimensions);

        for (var structure : selectedCustomStructures(
                structureSetEvent.configuredOverworld(),
                structureSetEvent.configuredNether(),
                customDepositStructures
        )) {
            structure.generateLang(event);
        }
    }

    private static @Nullable EnableDepositsKubeEvent currentStructureSetEvent(
            DepositStructuresKubeEvent customStructureEvent
    ) {
        if (!RNSStartupKubeEvents.RNS_ENABLE_DEPOSITS.hasListeners()) return null;

        var structureSetEvent = new EnableDepositsKubeEvent(
                () -> availableStructures(customStructureEvent.created(), customStructureEvent.tweaked()));
        RNSStartupKubeEvents.RNS_ENABLE_DEPOSITS.post(structureSetEvent);
        return structureSetEvent;
    }

    static Map<ResourceLocation, EnableDepositsKubeEvent.AvailableStructure> availableStructures(
            List<DepositStructureKubeBuilder> depositStructures
    ) {
        return availableStructures(depositStructures, List.of());
    }

    static Map<ResourceLocation, EnableDepositsKubeEvent.AvailableStructure> availableStructures(
            List<DepositStructureKubeBuilder> depositStructures,
            List<DepositStructureKubeBuilder> tweakedStructures
    ) {
        var structures = new LinkedHashMap<ResourceLocation, EnableDepositsKubeEvent.AvailableStructure>();
        var tweaksById = tweakedDepositStructuresById(tweakedStructures);

        for (var entry : DepositStructureBuilder.getDeposits()) {
            var structureId = DepositStructureBuilder.structureId(entry);
            var tweak = tweaksById.get(structureId);
            var weight = tweak != null ? tweak.structureSetWeight() : entry.structure().weight();
            putAvailableStructure(structures, structureId, weight, true,
                    tweak == null || !tweak.isInvalid());
        }

        for (var builder : depositStructures) {
            putAvailableStructure(structures, builder.id(), builder.structureSetWeight(), false,
                    !builder.isInvalid());
        }

        return structures;
    }

    private static DepositSelectionState currentDepositSelection() {
        synchronized (DEPOSIT_SELECTION_LOCK) {
            if (cachedDepositSelection == null) {
                cachedDepositSelection = computeDepositSelection();
            }
            return cachedDepositSelection;
        }
    }

    private static DepositSelectionState computeDepositSelection() {
        var assembler = fromCurrentEvents();
        var structureSetEvent = assembler.structureSetEvent;
        if (structureSetEvent == null) {
            return new DepositSelectionState(false, Set.of(), DEFAULT_ENABLED_DEPOSIT_BLOCKS);
        }

        var customStructures = List.copyOf(assembler.allCustomStructures.get());

        if (!structureSetEvent.hasConfiguredDimensions()) {
            return new DepositSelectionState(false, Set.of(), DEFAULT_ENABLED_DEPOSIT_BLOCKS);
        }

        var selectedStructureIds = new LinkedHashSet<>(
                selectedStructureIds(structureSetEvent.configuredOverworld(), structureSetEvent.configuredNether()));
        var enabledDepositBlocks = new LinkedHashSet<>(DEFAULT_ENABLED_DEPOSIT_BLOCKS);
        var configuredOverworld = structureSetEvent.configuredOverworld() != null;
        var configuredNether = structureSetEvent.configuredNether() != null;
        var builtInDepositBlocks = DepositStructureBuilder.getDeposits().stream()
                .collect(Collectors.toMap(
                        DepositStructureBuilder::structureId,
                        entry -> entry.structure().depositBlock(),
                        (first, second) -> first));
        var customDepositBlocks = customDepositStructuresById(customStructures).entrySet().stream()
                .filter(entry -> !entry.getValue().isInvalid())
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().blockId()));

        for (var structureId : selectedStructureIds) {
            var builtInDepositBlock = builtInDepositBlocks.get(structureId);
            if (builtInDepositBlock != null) {
                enabledDepositBlocks.add(builtInDepositBlock);
                continue;
            }

            var customDepositBlock = customDepositBlocks.get(structureId);
            if (customDepositBlock != null) {
                enabledDepositBlocks.add(customDepositBlock);
            }
        }

        for (var entry : DepositStructureBuilder.getDeposits()) {
            if (entry.structure().dimension() == DepositDimension.OVERWORLD && configuredOverworld) continue;
            if (entry.structure().dimension() == DepositDimension.NETHER && configuredNether) continue;
            if (!entry.scannable().get()) continue;

            enabledDepositBlocks.add(entry.structure().depositBlock());
        }

        return new DepositSelectionState(true, Set.copyOf(selectedStructureIds), Set.copyOf(enabledDepositBlocks));
    }

    private static List<DepositStructureKubeBuilder> selectedCustomStructures(
            @Nullable EnableDepositsKubeBuilder overworldStructureSet,
            @Nullable EnableDepositsKubeBuilder netherStructureSet,
            Map<ResourceLocation, DepositStructureKubeBuilder> customDepositStructures
    ) {
        var selected = new LinkedHashMap<ResourceLocation, DepositStructureKubeBuilder>();

        collectSelectedCustomStructures(selected, overworldStructureSet, customDepositStructures);
        collectSelectedCustomStructures(selected, netherStructureSet, customDepositStructures);

        return List.copyOf(selected.values());
    }

    private static void collectSelectedCustomStructures(
            Map<ResourceLocation, DepositStructureKubeBuilder> selected,
            @Nullable EnableDepositsKubeBuilder structureSet,
            Map<ResourceLocation, DepositStructureKubeBuilder> customDepositStructures
    ) {
        if (structureSet == null) return;

        for (var structure : structureSet.selectedStructures()) {
            var customStructure = customDepositStructures.get(structure.id());
            if (customStructure == null) continue;

            selected.putIfAbsent(customStructure.id(), customStructure);
        }
    }

    static List<ResourceLocation> selectedStructureIds(
            @Nullable EnableDepositsKubeBuilder overworldStructureSet,
            @Nullable EnableDepositsKubeBuilder netherStructureSet
    ) {
        var selected = new LinkedHashMap<ResourceLocation, ResourceLocation>();

        collectSelectedStructureIds(selected, overworldStructureSet);
        collectSelectedStructureIds(selected, netherStructureSet);

        return selected.keySet().stream()
                .sorted(Comparator.comparing(ResourceLocation::toString))
                .toList();
    }

    private static void collectSelectedStructureIds(
            Map<ResourceLocation, ResourceLocation> selected,
            @Nullable EnableDepositsKubeBuilder structureSet
    ) {
        if (structureSet == null) return;

        for (var structure : structureSet.selectedStructures()) {
            selected.putIfAbsent(structure.id(), structure.id());
        }
    }

    static Map<ResourceLocation, DepositStructureKubeBuilder> customDepositStructuresById(
            List<DepositStructureKubeBuilder> depositStructures
    ) {
        var structures = new LinkedHashMap<ResourceLocation, DepositStructureKubeBuilder>();

        for (var structure : depositStructures) {
            var previous = structures.putIfAbsent(structure.id(), structure);
            if (previous != null) {
                throw new IllegalStateException("Duplicate custom deposit structure id registered for KubeJS: "
                        + structure.id());
            }
        }

        return structures;
    }

    static Map<ResourceLocation, DepositStructureKubeBuilder> tweakedDepositStructuresById(
            List<DepositStructureKubeBuilder> depositStructures
    ) {
        var structures = new LinkedHashMap<ResourceLocation, DepositStructureKubeBuilder>();

        for (var structure : depositStructures) {
            if (!structure.isTweak()) {
                throw new IllegalArgumentException("Expected a built-in deposit structure tweak: " + structure.id());
            }
            var previous = structures.putIfAbsent(structure.id(), structure);
            if (previous != null) {
                throw new IllegalStateException("Duplicate built-in deposit structure tweak registered for KubeJS: "
                        + structure.id());
            }
        }

        return structures;
    }

    private static void putAvailableStructure(
            Map<ResourceLocation, EnableDepositsKubeEvent.AvailableStructure> structures,
            ResourceLocation structureId, Integer weight, boolean builtIn, boolean valid
    ) {
        var previous = structures.putIfAbsent(structureId,
                new EnableDepositsKubeEvent.AvailableStructure(structureId, weight, builtIn, valid));
        if (previous != null) {
            throw new IllegalStateException("Duplicate deposit structure id registered for KubeJS structure set selection: "
                    + structureId);
        }
    }

    private static void applyStructureSetDimension(
            @Nullable EnableDepositsKubeBuilder structureSet,
            DepositDimension dimension,
            Map<ResourceLocation, DepositStructureKubeBuilder> customDepositStructures,
            Map<ResourceLocation, DepositDimension> assignedDimensions
    ) {
        if (structureSet == null) return;

        for (var selectedStructure : structureSet.selectedStructures()) {
            var customStructure = customDepositStructures.get(selectedStructure.id());
            if (customStructure == null) continue;

            var previousDimension = assignedDimensions.putIfAbsent(customStructure.id(), dimension);
            if (previousDimension != null && previousDimension != dimension) {
                throw new IllegalStateException("Custom deposit structure " + customStructure.id()
                        + " cannot belong to both overworld and nether KubeJS structure sets");
            }

            customStructure.setDimension(dimension);
        }
    }

    private static void generateStructureSet(
            DataJsonGenerator generator,
            @Nullable EnableDepositsKubeBuilder structureSet,
            int defaultSeparation,
            int defaultSpacing,
            int defaultSalt
    ) {
        if (structureSet == null) return;

        generator.json(structureSetPath(structureSet.id()),
                structureSetJson(structureSet.worldgenStructures(),
                        structureSet.resolvedSeparation(defaultSeparation),
                        structureSet.resolvedSpacing(defaultSpacing),
                        structureSet.resolvedSalt(defaultSalt)));
    }

    private static void generateDefaultStructureSetWeightOverrides(
            DataJsonGenerator generator,
            List<DepositStructureKubeBuilder> tweakedStructures,
            @Nullable EnableDepositsKubeBuilder overworldStructureSet,
            @Nullable EnableDepositsKubeBuilder netherStructureSet
    ) {
        var availableStructures = availableStructures(List.of(), tweakedStructures);
        if (overworldStructureSet == null && hasWeightOverride(tweakedStructures, DepositDimension.OVERWORLD)) {
            var structureSet = defaultStructureSet(DepositDimension.OVERWORLD, availableStructures);
            generateStructureSet(generator, structureSet,
                    RNSPacks.DEFAULT_SEPARATION, RNSPacks.DEFAULT_SPACING, RNSPacks.SALT);
        }
        if (netherStructureSet == null && hasWeightOverride(tweakedStructures, DepositDimension.NETHER)) {
            var structureSet = defaultStructureSet(DepositDimension.NETHER, availableStructures);
            generateStructureSet(generator, structureSet,
                    RNSPacks.DEFAULT_NETHER_SEPARATION, RNSPacks.DEFAULT_NETHER_SPACING, RNSPacks.NETHER_SALT);
        }
    }

    private static boolean hasWeightOverride(
            List<DepositStructureKubeBuilder> tweakedStructures, DepositDimension dimension
    ) {
        return tweakedStructures.stream()
                .anyMatch(structure -> structure.dimension() == dimension && structure.weightChanged());
    }

    private static EnableDepositsKubeBuilder defaultStructureSet(
            DepositDimension dimension,
            Map<ResourceLocation, EnableDepositsKubeEvent.AvailableStructure> availableStructures
    ) {
        var structureSet = new EnableDepositsKubeBuilder(
                CreateRNS.asResource(dimension.prefix() + "deposits"), availableStructures);
        for (var entry : DepositStructureBuilder.getScannableDeposits(dimension)) {
            structureSet.deposit(DepositStructureBuilder.structureId(entry).toString());
        }
        return structureSet;
    }

    private static void generateBuiltInDepositSpecOverrides(
            DataJsonGenerator generator,
            @Nullable EnableDepositsKubeBuilder overworldStructureSet,
            @Nullable EnableDepositsKubeBuilder netherStructureSet,
            Set<ResourceLocation> tweakedStructureIds
    ) {
        var selectedStructureIds = Set.copyOf(selectedStructureIds(overworldStructureSet, netherStructureSet));
        for (var entry : DepositSpecBuilder.getSpecs()) {
            if (entry.dimension() == DepositDimension.OVERWORLD && overworldStructureSet == null) continue;
            if (entry.dimension() == DepositDimension.NETHER && netherStructureSet == null) continue;
            if (tweakedStructureIds.contains(entry.spec().structureId())) continue;

            var scannable = selectedStructureIds.contains(entry.spec().structureId());
            generator.json(DynamicDatapackContent.depositSpecPath(entry),
                    DynamicDatapackContent.depositSpecJson(entry, scannable));
        }
    }

    private static void generateMinerAttachmentTag(
            DataJsonGenerator generator,
            List<CatalystKubeBuilder> customCatalysts
    ) {
        var attachmentBlocks = new LinkedHashSet<String>();
        for (var catalyst : customCatalysts) {
            attachmentBlocks.addAll(catalyst.attachmentBlocks());
        }
        if (attachmentBlocks.isEmpty()) return;

        var values = new JsonArray();
        for (var blockId : attachmentBlocks.stream().sorted().toList()) {
            values.add(blockId);
        }

        var json = new JsonObject();
        json.addProperty("replace", false);
        json.add("values", values);
        generator.json(CreateRNS.asResource("tags/block/miner_attachments"), json);
    }

    private static JsonObject structureSetJson(
            List<EnableDepositsKubeBuilder.SelectedStructure> structures,
            int separation, int spacing, int salt
    ) {
        var root = new JsonObject();
        var placement = new JsonObject();
        var structureList = new JsonArray();

        placement.addProperty("type", "minecraft:random_spread");
        placement.addProperty("separation", separation);
        placement.addProperty("spacing", spacing);
        placement.addProperty("salt", salt);
        root.add("placement", placement);

        for (var entry : structures) {
            var structure = new JsonObject();
            structure.addProperty("structure", entry.id().toString());
            structure.addProperty("weight", entry.resolvedWeight());
            structureList.add(structure);
        }

        root.add("structures", structureList);
        return root;
    }

    private static ResourceLocation structureSetPath(ResourceLocation structureSetId) {
        return ResourceLocation.fromNamespaceAndPath(structureSetId.getNamespace(),
                "worldgen/structure_set/" + structureSetId.getPath());
    }

    private static DepositStructuresKubeEvent currentDepositStructureEvent() {
        var event = new DepositStructuresKubeEvent();
        if (!RNSStartupKubeEvents.RNS_DEPOSIT_STRUCTURES.hasListeners()) return event;

        RNSStartupKubeEvents.RNS_DEPOSIT_STRUCTURES.post(event);
        event.validateForKubeJS();
        return event;
    }

    private static CatalystsKubeEvent currentCatalystEvent() {
        var event = new CatalystsKubeEvent();
        if (!RNSStartupKubeEvents.RNS_CATALYSTS.hasListeners()) return event;

        RNSStartupKubeEvents.RNS_CATALYSTS.post(event);
        return event;
    }

    private record DepositSelectionState(
            boolean managing,
            Set<ResourceLocation> selectedStructureIds,
            Set<ResourceLocation> enabledDepositBlocks
    ) {
    }
}
