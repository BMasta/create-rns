package com.bmaster.createrns.data.pack;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.RNSRegistries;
import com.bmaster.createrns.content.deposit.mining.recipe.Yield.WeightedItem;
import com.bmaster.createrns.data.deposit.DepositData;
import com.bmaster.createrns.data.deposit.DepositJson;
import com.bmaster.createrns.util.codec.ResourceOrTagLocation;
import com.bmaster.createrns.data.pack.DynamicDatapack.DatapackFile;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class DynamicDatapackContent {
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

            var biomeSelectorId = DepositData.biomeTagId(CreateRNS.ID, dimension);
            return List.of(DatapackFile.ofTag(Registries.BIOME, biomeSelectorId,
                    DepositJson.tag(biomes, disableGeneration)));
        };
    }

    public static Supplier<List<DatapackFile>> depositProcessorLists() {
        return () -> {
            var blockEntries = DepositData.getBlocks();
            var files = new ArrayList<DatapackFile>(blockEntries.size());
            for (var block : blockEntries) {
                files.add(DatapackFile.ofResource(Registries.PROCESSOR_LIST, block.processorId(),
                        DepositJson.processorList(DepositData.PROCESSOR_INPUT_BLOCK, block.blockId())));
            }
            return files;
        };
    }

    public static Supplier<List<DatapackFile>> depositStructures() {
        return () -> {
            var files = new ArrayList<DatapackFile>();
            for (var structure : DepositData.getStructures()) {
                var biomes = new ResourceOrTagLocation(DepositData.biomeTagId(CreateRNS.ID, structure.dimension()), true);
                var processorId = DepositData.processorId(structure.namespace(), structure.keyword());
                var json = DepositJson.structure(biomes, structure.placementStrategy(),
                        -structure.depth() - structure.depthDeviation(), -structure.depth() + structure.depthDeviation(),
                        structure.templates(), processorId);
                files.add(DatapackFile.ofResource(Registries.STRUCTURE, structure.structureId(), json));
            }
            return files;
        };
    }

    public static Supplier<List<DatapackFile>> depositStructureTag() {
        return () -> {
            var structures = DepositData.getStructures().stream()
                    .map(structure -> new ResourceOrTagLocation(structure.structureId(), false))
                    .toList();
            return List.of(DatapackFile.ofTag(Registries.STRUCTURE, DepositData.structureTagId(),
                    DepositJson.tag(structures, false)));
        };
    }

    public static Supplier<List<DatapackFile>> depositStructureSet(
            ResourceKey<Level> dimension, int separation, int spacing, int salt
    ) {
        return () -> {
            var structures = DepositData.getScannableStructures(dimension).stream()
                    .map(structure -> new DepositData.WeightedStructure(structure.structureId(), structure.weight()))
                    .toList();
            var json = DepositJson.structureSet(structures, separation, spacing, salt);
            var id = DepositData.structureSetId(CreateRNS.ID, dimension);
            return List.of(DatapackFile.ofResource(Registries.STRUCTURE_SET, id, json));
        };
    }

    public static Supplier<List<DatapackFile>> miningRecipes() {
        return () -> {
            var recipes = DepositData.getEnabledRecipes();
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

                files.add(DatapackFile.ofResource(Registries.RECIPE, r.recipeId(), root));
            }

            return files;
        };
    }

    public static Supplier<List<DatapackFile>> depositSpecs() {
        return () -> {
            var structures = DepositData.getStructures();
            var files = new ArrayList<DatapackFile>(structures.size());
            for (var structure : structures) {
                var json = DepositJson.spec(structure.structureId(), structure.dimension(),
                        structure.scannerIconCandidates(), structure.mapIconCandidates(),
                        structure.isScannable().get());
                files.add(DatapackFile.ofResource(RNSRegistries.DEPOSIT_SPEC, structure.specId(), json));
            }
            return files;
        };
    }
}
