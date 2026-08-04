package com.bmaster.createrns.compat.kubejs;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.data.pack.DepositDimension;
import com.bmaster.createrns.data.pack.DepositSpecBuilder;
import com.bmaster.createrns.data.pack.DepositStructureBuilder;
import com.bmaster.createrns.data.pack.DynamicDatapackContent;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.latvian.mods.kubejs.KubeJSPlugin;
import dev.latvian.mods.kubejs.client.LangEventJS;
import dev.latvian.mods.kubejs.generator.DataJsonGenerator;
import dev.latvian.mods.kubejs.recipe.schema.RegisterRecipeSchemasEvent;
import dev.latvian.mods.kubejs.registry.RegistryInfo;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class RNSKubeJSPlugin extends KubeJSPlugin {
    private static final int OVERWORLD_STRUCTURE_SET_SEPARATION = 4;
    private static final int OVERWORLD_STRUCTURE_SET_SPACING = 24;
    private static final int OVERWORLD_STRUCTURE_SET_SALT = 591646342;
    private static final int NETHER_STRUCTURE_SET_SEPARATION = 2;
    private static final int NETHER_STRUCTURE_SET_SPACING = 8;
    private static final int NETHER_STRUCTURE_SET_SALT = 781087034;

    @Override
    public void init() {
        RegistryInfo.BLOCK.addType(
                CreateRNS.asResource("deposit").toString(),
                DepositBlockKubeBuilder.class,
                DepositBlockKubeBuilder::new);
    }

    @Override
    public void registerEvents() {
        RNSStartupKubeEvents.init();
    }

    @Override
    public void registerRecipeSchemas(RegisterRecipeSchemasEvent event) {
        event.register(CreateRNS.asResource("mining"), MiningRecipeKubeSchema.schema());
    }

    @Override
    public void generateDataJsons(DataJsonGenerator generator) {
        var allCustomStructures = allCustomDepositStructures();
        if (!RNSStartupKubeEvents.CREATE_RNS_STRUCTURE_SET.hasListeners()) return;

        var customDepositStructures = customDepositStructuresById(allCustomStructures);
        var structureSetEvent = new DepositStructureSetKubeEvent(availableStructures(allCustomStructures));
        RNSStartupKubeEvents.CREATE_RNS_STRUCTURE_SET.post(structureSetEvent);

        var assignedDimensions = new HashMap<ResourceLocation, DepositDimension>();
        var overworldStructureSet = structureSetEvent.overworld();
        applyStructureSetDimension(overworldStructureSet,
                DepositDimension.OVERWORLD, customDepositStructures, assignedDimensions);
        var netherStructureSet = structureSetEvent.nether();
        applyStructureSetDimension(netherStructureSet,
                DepositDimension.NETHER, customDepositStructures, assignedDimensions);

        var selectedCustomStructures = selectedCustomStructures(overworldStructureSet, netherStructureSet, customDepositStructures);
        for (var structure : selectedCustomStructures) {
            structure.generateData(generator);
        }

        var selectedStructureIds = selectedStructureIds(overworldStructureSet, netherStructureSet);
        generateBuiltInDepositSpecOverrides(generator, selectedStructureIds);

        generateStructureSet(generator, overworldStructureSet,
                OVERWORLD_STRUCTURE_SET_SEPARATION, OVERWORLD_STRUCTURE_SET_SPACING, OVERWORLD_STRUCTURE_SET_SALT);
        generateStructureSet(generator, netherStructureSet,
                NETHER_STRUCTURE_SET_SEPARATION, NETHER_STRUCTURE_SET_SPACING, NETHER_STRUCTURE_SET_SALT);

        var values = new JsonArray();
        for (var structureId : selectedStructureIds) {
            values.add(structureId.toString());
        }

        var json = new JsonObject();
        json.addProperty("replace", true);
        json.add("values", values);
        generator.json(CreateRNS.asResource("tags/worldgen/structure/deposits"), json);
    }

    @Override
    public void generateLang(LangEventJS event) {
        if (!RNSStartupKubeEvents.CREATE_RNS_STRUCTURE_SET.hasListeners()) return;

        var customDepositStructures = customDepositStructuresById(allCustomDepositStructures());
        var structureSetEvent = new DepositStructureSetKubeEvent(availableStructures(List.copyOf(customDepositStructures.values())));
        RNSStartupKubeEvents.CREATE_RNS_STRUCTURE_SET.post(structureSetEvent);
        var assignedDimensions = new HashMap<ResourceLocation, DepositDimension>();
        applyStructureSetDimension(structureSetEvent.overworld(),
                DepositDimension.OVERWORLD, customDepositStructures, assignedDimensions);
        applyStructureSetDimension(structureSetEvent.nether(),
                DepositDimension.NETHER, customDepositStructures, assignedDimensions);

        for (var structure : selectedCustomStructures(
                structureSetEvent.overworld(),
                structureSetEvent.nether(),
                customDepositStructures
        )) {
            structure.generateLang(event);
        }
    }

    private static Map<ResourceLocation, DepositStructureSetKubeEvent.AvailableStructure> availableStructures(
            List<DepositStructureKubeBuilder> depositStructures
    ) {
        var structures = new LinkedHashMap<ResourceLocation, DepositStructureSetKubeEvent.AvailableStructure>();

        for (var entry : DepositStructureBuilder.getEnabledDeposits()) {
            var structureId = DepositStructureBuilder.structureId(entry);
            putAvailableStructure(structures, structureId, entry.structure().weight(), true);
        }

        for (var builder : depositStructures) {
            putAvailableStructure(structures, builder.id(), builder.structureSetWeight(), false);
        }

        return structures;
    }

    private static List<DepositStructureKubeBuilder> selectedCustomStructures(
            DepositStructureSetKubeBuilder overworldStructureSet,
            DepositStructureSetKubeBuilder netherStructureSet,
            Map<ResourceLocation, DepositStructureKubeBuilder> customDepositStructures
    ) {
        var selected = new LinkedHashMap<ResourceLocation, DepositStructureKubeBuilder>();

        collectSelectedCustomStructures(selected, overworldStructureSet, customDepositStructures);
        collectSelectedCustomStructures(selected, netherStructureSet, customDepositStructures);

        return List.copyOf(selected.values());
    }

    private static void collectSelectedCustomStructures(
            Map<ResourceLocation, DepositStructureKubeBuilder> selected,
            DepositStructureSetKubeBuilder structureSet,
            Map<ResourceLocation, DepositStructureKubeBuilder> customDepositStructures
    ) {
        for (var structure : structureSet.selectedStructures()) {
            var customStructure = customDepositStructures.get(structure.id());
            if (customStructure == null) continue;

            selected.putIfAbsent(customStructure.id(), customStructure);
        }
    }

    private static List<ResourceLocation> selectedStructureIds(
            DepositStructureSetKubeBuilder overworldStructureSet,
            DepositStructureSetKubeBuilder netherStructureSet
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
            DepositStructureSetKubeBuilder structureSet
    ) {
        for (var structure : structureSet.selectedStructures()) {
            selected.putIfAbsent(structure.id(), structure.id());
        }
    }

    private static Map<ResourceLocation, DepositStructureKubeBuilder> customDepositStructuresById(
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
            DepositStructureSetKubeBuilder structureSet,
            DepositDimension dimension,
            Map<ResourceLocation, DepositStructureKubeBuilder> customDepositStructures,
            Map<ResourceLocation, DepositDimension> assignedDimensions
    ) {
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
            DepositStructureSetKubeBuilder structureSet,
            int defaultSeparation,
            int defaultSpacing,
            int defaultSalt
    ) {
        generator.json(structureSetPath(structureSet.id()),
                structureSetJson(structureSet.worldgenStructures(),
                        structureSet.resolvedSeparation(defaultSeparation),
                        structureSet.resolvedSpacing(defaultSpacing),
                        structureSet.resolvedSalt(defaultSalt)));
    }

    private static void generateBuiltInDepositSpecOverrides(
            DataJsonGenerator generator,
            List<ResourceLocation> selectedStructureIds
    ) {
        for (var entry : DepositSpecBuilder.getEnabledSpecs()) {
            var scannable = selectedStructureIds.contains(entry.spec().structureId());
            generator.json(DynamicDatapackContent.depositSpecPath(entry),
                    DynamicDatapackContent.depositSpecJson(entry, scannable));
        }
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

    private static List<DepositStructureKubeBuilder> allCustomDepositStructures() {
        if (!RNSStartupKubeEvents.CREATE_RNS_DEPOSIT_STRUCTURES.hasListeners()) return List.of();

        var event = new DepositStructuresKubeEvent();
        RNSStartupKubeEvents.CREATE_RNS_DEPOSIT_STRUCTURES.post(event);
        return event.created();
    }
}
