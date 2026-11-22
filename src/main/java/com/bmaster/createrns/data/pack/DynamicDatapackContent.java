package com.bmaster.createrns.data.pack;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.RNSRegistries;
import com.bmaster.createrns.content.deposit.mining.recipe.Yield.WeightedItem;
import com.bmaster.createrns.data.deposit.DepositJson;
import com.bmaster.createrns.data.deposit.DepositResources;
import com.bmaster.createrns.data.pack.DynamicDatapack.DatapackFile;
import com.bmaster.createrns.util.codec.ResourceOrTagLocation;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class DynamicDatapackContent {
    public static final DepositResources RESOURCES = new DepositResources();

    public static Supplier<List<DatapackFile>> depositBiomeTag(ResourceKey<Level> dimension, boolean disableGeneration) {
        return () -> {
            var biomes = new ArrayList<ResourceOrTagLocation>();
            if (!disableGeneration) {
                if (dimension.equals(Level.OVERWORLD)) {
                    for (var path : List.of("is_forest", "is_jungle", "is_taiga", "is_badlands", "is_hill", "is_savanna")) {
                        biomes.add(ResourceOrTagLocation.withDefaultNamespace(path, true));
                    }
                } else if (dimension.equals(Level.NETHER)) {
                    biomes.add(ResourceOrTagLocation.withDefaultNamespace("is_nether", true));
                }
            }

            var biomeSelectorId = DepositResources.biomeTagId(CreateRNS.ID, dimension);
            return List.of(DatapackFile.ofTag(Registries.BIOME, biomeSelectorId,
                    DepositJson.tag(biomes, disableGeneration)));
        };
    }

    public static Supplier<List<DatapackFile>> depositProcessorLists() {
        return () -> {
            var processors = RESOURCES.processors;
            var files = new ArrayList<DatapackFile>(processors.size());
            for (var processor : processors) {
                files.add(DatapackFile.ofResource(Registries.PROCESSOR_LIST, processor.id(),
                        DepositJson.processorList(DepositResources.PROCESSOR_INPUT_BLOCK, processor.targetBlock())));
            }
            return files;
        };
    }

    public static Supplier<List<DatapackFile>> depositStructures() {
        return () -> {
            var files = new ArrayList<DatapackFile>();
            for (var structure : RESOURCES.structures) {
                var json = DepositJson.structure(structure.biomeSelector(), structure.placementStrategy(),
                        -structure.depth() - structure.depthDeviation(), -structure.depth() + structure.depthDeviation(),
                        structure.templates(), structure.processorId());
                files.add(DatapackFile.ofResource(Registries.STRUCTURE, structure.id(), json));
            }
            return files;
        };
    }

    public static Supplier<List<DatapackFile>> depositStructureTag() {
        return () -> {
            var structures = RESOURCES.structures.stream()
                    .map(structure -> new ResourceOrTagLocation(structure.id(), false))
                    .toList();
            return List.of(DatapackFile.ofTag(Registries.STRUCTURE, DepositResources.structureTagId(),
                    DepositJson.tag(structures, false)));
        };
    }

    public static Supplier<List<DatapackFile>> depositStructureSet(
            ResourceKey<Level> dimension, int separation, int spacing, int salt
    ) {
        return () -> {
            var structures = RESOURCES.getScannableStructures(dimension).stream()
                    .map(entry -> new DepositResources.WeightedStructure(entry.id(), Objects.requireNonNull(
                            entry.weight(), () -> "Missing weight for built-in structure " + entry.id())))
                    .toList();
            var json = DepositJson.structureSet(structures, separation, spacing, salt);
            var id = DepositResources.structureSetId(CreateRNS.ID, dimension);
            return List.of(DatapackFile.ofResource(Registries.STRUCTURE_SET, id, json));
        };
    }

    public static Supplier<List<DatapackFile>> miningRecipes() {
        return () -> {
            var recipes = RESOURCES.getEnabledRecipes();
            var files = new ArrayList<DatapackFile>(recipes.size());

            for (var r : recipes) {
                var dim = r.dimension();
                var root = new JsonObject();

                root.addProperty("type", CreateRNS.ID + ":mining");
                root.addProperty("deposit_block", r.sourceBlockId().toString());
                if (!dim.equals(Level.OVERWORLD)) {
                    root.addProperty("dimension", dim.location().toString());
                }

                if (r.replacementBlockId() != null) {
                    root.addProperty("replace_when_depleted", r.replacementBlockId().toString());
                }
                if (r.durability() != null) {
                    var durability = new JsonObject();
                    durability.addProperty("core", r.durability().core());
                    durability.addProperty("edge", r.durability().edge());
                    durability.addProperty("random_spread", r.durability().randomSpread());
                    root.add("durability", durability);
                }

                var yields = new JsonArray();
                for (var yield : r.yields()) {
                    var yieldJson = new JsonObject();
                    if (yield.chance() != 1) {
                        yieldJson.addProperty("chance", yield.chance());
                    }

                    var items = new JsonArray();
                    for (var item : yield.items()) {
                        var candidateIds = item.candidateIds().stream()
                                .map(c -> c.contains(":") ? c : "minecraft:" + c)
                                .toList();
                        var itemJson = new JsonObject();

                        // Item candidates
                        if (candidateIds.size() == 1) {
                            itemJson.addProperty("item", candidateIds.getFirst());
                        } else {
                            var icArr = new JsonArray();
                            for (var ic : candidateIds) {
                                icArr.add(ic);
                            }
                            itemJson.add("item", icArr);
                        }

                        if (item.weight() != WeightedItem.DEFAULT_WEIGHT) {
                            itemJson.addProperty("weight", item.weight());
                        }
                        items.add(itemJson);
                    }
                    yieldJson.add("items", items);

                    if (!yield.catalysts().isEmpty()) {
                        var catalysts = new JsonArray();
                        for (var catalyst : yield.catalysts()) {
                            catalysts.add(catalyst.toString());
                        }
                        yieldJson.add("catalysts", catalysts);
                    }
                    if (yield.jeiSlotColor() != 0) {
                        yieldJson.addProperty("jei_slot_color", yield.jeiSlotColor());
                    }

                    yields.add(yieldJson);
                }
                root.add("yields", yields);

                files.add(DatapackFile.ofResource(Registries.RECIPE, r.id(), root));
            }

            return files;
        };
    }

    public static Supplier<List<DatapackFile>> depositSpecs() {
        return () -> {
            var specs = RESOURCES.specs;
            var files = new ArrayList<DatapackFile>(specs.size());
            for (var spec : specs) {
                var json = DepositJson.spec(spec.structureId(), spec.dimension(),
                        spec.scannerIconCandidates(), spec.mapIconCandidates(), spec.isScannable().get());
                files.add(DatapackFile.ofResource(RNSRegistries.DEPOSIT_SPEC, spec.id(), json));
            }
            return files;
        };
    }
}
