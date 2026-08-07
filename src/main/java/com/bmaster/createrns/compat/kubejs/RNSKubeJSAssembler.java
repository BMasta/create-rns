package com.bmaster.createrns.compat.kubejs;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.data.pack.DepositBlockBuilder.DepositBuildingContext;
import com.bmaster.createrns.data.pack.DepositDimension;
import com.bmaster.createrns.data.pack.DepositSpecBuilder;
import com.bmaster.createrns.data.pack.DepositStructureBuilder;
import com.bmaster.createrns.data.pack.DynamicDatapackContent;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.latvian.mods.kubejs.client.LangKubeEvent;
import dev.latvian.mods.kubejs.generator.KubeDataGenerator;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class RNSKubeJSAssembler {
    private static final int OVERWORLD_STRUCTURE_SET_SEPARATION = 4;
    private static final int OVERWORLD_STRUCTURE_SET_SPACING = 24;
    private static final int OVERWORLD_STRUCTURE_SET_SALT = 591646342;
    private static final int NETHER_STRUCTURE_SET_SEPARATION = 2;
    private static final int NETHER_STRUCTURE_SET_SPACING = 8;
    private static final int NETHER_STRUCTURE_SET_SALT = 781087034;
    private static final Object DEPOSIT_SELECTION_LOCK = new Object();
    private static final Set<ResourceLocation> DEFAULT_ENABLED_DEPOSIT_BLOCKS =
            Set.of(new DepositBuildingContext("depleted").depositBlockId());

    private static @Nullable DepositSelectionState cachedDepositSelection;

    private final Supplier<List<CatalystKubeBuilder>> customCatalysts;
    private final Supplier<List<DepositStructureKubeBuilder>> allCustomStructures;
    private final @Nullable DepositStructureSetKubeEvent structureSetEvent;

    public RNSKubeJSAssembler(
            Supplier<List<CatalystKubeBuilder>> customCatalysts,
            Supplier<List<DepositStructureKubeBuilder>> allCustomStructures,
            @Nullable DepositStructureSetKubeEvent structureSetEvent
    ) {
        this.customCatalysts = customCatalysts;
        this.allCustomStructures = allCustomStructures;
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
        var customCatalystEvent = currentCatalystEvent();
        var customStructureEvent = currentDepositStructureEvent();
        var structureSetEvent = currentStructureSetEvent(customStructureEvent);
        return new RNSKubeJSAssembler(customCatalystEvent::created, customStructureEvent::created, structureSetEvent);
    }

    static void resetDepositSelectionCache() {
        synchronized (DEPOSIT_SELECTION_LOCK) {
            cachedDepositSelection = null;
        }
    }

    public void generateData(KubeDataGenerator generator) {
        var customCatalysts = List.copyOf(this.customCatalysts.get());
        for (var catalyst : customCatalysts) {
            catalyst.generateData(generator);
        }
        generateMinerAttachmentTag(generator, customCatalysts);

        if (structureSetEvent == null) return;
        if (!structureSetEvent.hasConfiguredDimensions()) return;

        var allCustomStructures = List.copyOf(this.allCustomStructures.get());
        var customDepositStructures = customDepositStructuresById(allCustomStructures);
        var assignedDimensions = new HashMap<ResourceLocation, DepositDimension>();
        var overworldStructureSet = structureSetEvent.configuredOverworld();
        applyStructureSetDimension(overworldStructureSet,
                DepositDimension.OVERWORLD, customDepositStructures, assignedDimensions);
        var netherStructureSet = structureSetEvent.configuredNether();
        applyStructureSetDimension(netherStructureSet,
                DepositDimension.NETHER, customDepositStructures, assignedDimensions);

        var selectedCustomStructures = selectedCustomStructures(overworldStructureSet, netherStructureSet, customDepositStructures);
        for (var structure : selectedCustomStructures) {
            structure.generateData(generator);
        }

        generateBuiltInDepositSpecOverrides(generator, overworldStructureSet, netherStructureSet);

        generateStructureSet(generator, overworldStructureSet,
                OVERWORLD_STRUCTURE_SET_SEPARATION, OVERWORLD_STRUCTURE_SET_SPACING, OVERWORLD_STRUCTURE_SET_SALT);
        generateStructureSet(generator, netherStructureSet,
                NETHER_STRUCTURE_SET_SEPARATION, NETHER_STRUCTURE_SET_SPACING, NETHER_STRUCTURE_SET_SALT);

        var selectedStructureIds = selectedStructureIds(overworldStructureSet, netherStructureSet);
        var values = new JsonArray();
        for (var structureId : selectedStructureIds) {
            values.add(structureId.toString());
        }

        var json = new JsonObject();
        json.addProperty("replace", true);
        json.add("values", values);
        generator.json(CreateRNS.asResource("tags/worldgen/structure/deposits"), json);
    }

    public void generateLang(LangKubeEvent event) {
        var customCatalysts = List.copyOf(this.customCatalysts.get());
        for (var catalyst : customCatalysts) {
            catalyst.generateLang(event);
        }

        if (structureSetEvent == null) return;
        if (!structureSetEvent.hasConfiguredDimensions()) return;

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

    private static @Nullable DepositStructureSetKubeEvent currentStructureSetEvent(DepositStructuresKubeEvent customStructureEvent) {
        if (!RNSStartupKubeEvents.CREATE_RNS_STRUCTURE_SET.hasListeners()) return null;

        var structureSetEvent = new DepositStructureSetKubeEvent(
                () -> availableStructures(customStructureEvent.created()));
        RNSStartupKubeEvents.CREATE_RNS_STRUCTURE_SET.post(structureSetEvent);
        return structureSetEvent;
    }

    static Map<ResourceLocation, DepositStructureSetKubeEvent.AvailableStructure> availableStructures(
            List<DepositStructureKubeBuilder> depositStructures
    ) {
        var structures = new LinkedHashMap<ResourceLocation, DepositStructureSetKubeEvent.AvailableStructure>();

        for (var entry : DepositStructureBuilder.getDeposits()) {
            var structureId = DepositStructureBuilder.structureId(entry);
            putAvailableStructure(structures, structureId, entry.structure().weight(), true);
        }

        for (var builder : depositStructures) {
            putAvailableStructure(structures, builder.id(), builder.structureSetWeight(), false);
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
        if (!RNSStartupKubeEvents.CREATE_RNS_STRUCTURE_SET.hasListeners()) {
            return new DepositSelectionState(false, Set.of(), DEFAULT_ENABLED_DEPOSIT_BLOCKS);
        }

        var customStructureEvent = currentDepositStructureEvent();
        var customStructures = customStructureEvent.created();
        var structureSetEvent = new DepositStructureSetKubeEvent(() -> availableStructures(customStructures));
        RNSStartupKubeEvents.CREATE_RNS_STRUCTURE_SET.post(structureSetEvent);

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
            @Nullable DepositStructureSetKubeBuilder overworldStructureSet,
            @Nullable DepositStructureSetKubeBuilder netherStructureSet,
            Map<ResourceLocation, DepositStructureKubeBuilder> customDepositStructures
    ) {
        var selected = new LinkedHashMap<ResourceLocation, DepositStructureKubeBuilder>();

        collectSelectedCustomStructures(selected, overworldStructureSet, customDepositStructures);
        collectSelectedCustomStructures(selected, netherStructureSet, customDepositStructures);

        return List.copyOf(selected.values());
    }

    private static void collectSelectedCustomStructures(
            Map<ResourceLocation, DepositStructureKubeBuilder> selected,
            @Nullable DepositStructureSetKubeBuilder structureSet,
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
            @Nullable DepositStructureSetKubeBuilder overworldStructureSet,
            @Nullable DepositStructureSetKubeBuilder netherStructureSet
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
            @Nullable DepositStructureSetKubeBuilder structureSet
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

    private static void putAvailableStructure(
            Map<ResourceLocation, DepositStructureSetKubeEvent.AvailableStructure> structures,
            ResourceLocation structureId, Integer weight, boolean builtIn
    ) {
        var previous = structures.putIfAbsent(structureId,
                new DepositStructureSetKubeEvent.AvailableStructure(structureId, weight, builtIn));
        if (previous != null) {
            throw new IllegalStateException("Duplicate deposit structure id registered for KubeJS structure set selection: "
                    + structureId);
        }
    }

    private static void applyStructureSetDimension(
            @Nullable DepositStructureSetKubeBuilder structureSet,
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
            KubeDataGenerator generator,
            @Nullable DepositStructureSetKubeBuilder structureSet,
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

    private static void generateBuiltInDepositSpecOverrides(
            KubeDataGenerator generator,
            @Nullable DepositStructureSetKubeBuilder overworldStructureSet,
            @Nullable DepositStructureSetKubeBuilder netherStructureSet
    ) {
        var selectedStructureIds = Set.copyOf(selectedStructureIds(overworldStructureSet, netherStructureSet));
        for (var entry : DepositSpecBuilder.getSpecs()) {
            if (entry.dimension() == DepositDimension.OVERWORLD && overworldStructureSet == null) continue;
            if (entry.dimension() == DepositDimension.NETHER && netherStructureSet == null) continue;

            var scannable = selectedStructureIds.contains(entry.spec().structureId());
            generator.json(DynamicDatapackContent.depositSpecPath(entry),
                    DynamicDatapackContent.depositSpecJson(entry, scannable));
        }
    }

    private static void generateMinerAttachmentTag(KubeDataGenerator generator, List<CatalystKubeBuilder> customCatalysts) {
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
            List<DepositStructureSetKubeBuilder.SelectedStructure> structures,
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
        if (!RNSStartupKubeEvents.CREATE_RNS_DEPOSIT_STRUCTURES.hasListeners()) return event;

        RNSStartupKubeEvents.CREATE_RNS_DEPOSIT_STRUCTURES.post(event);
        return event;
    }

    private static CatalystsKubeEvent currentCatalystEvent() {
        var event = new CatalystsKubeEvent();
        if (!RNSStartupKubeEvents.CREATE_RNS_CATALYSTS.hasListeners()) return event;

        RNSStartupKubeEvents.CREATE_RNS_CATALYSTS.post(event);
        return event;
    }

    private record DepositSelectionState(
            boolean managing,
            Set<ResourceLocation> selectedStructureIds,
            Set<ResourceLocation> enabledDepositBlocks
    ) {
    }
}
