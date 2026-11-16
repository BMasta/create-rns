package com.bmaster.createrns.data.pack;

import com.bmaster.createrns.content.deposit.mining.recipe.Yield.WeightedItem;
import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.data.deposit.DepositData;
import com.bmaster.createrns.data.DumpSafeOverrides;
import com.bmaster.createrns.data.pack.DynamicDatapack.DatapackFile;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class DynamicDatapackContent {
    public static Supplier<List<DatapackFile>> depositBiomeTag(ResourceLocation dimension, boolean disableGeneration) {
        return () -> {
            var values = new JsonArray();
            var root = new JsonObject();
            if (disableGeneration) {
                root.addProperty("replace", true);
            } else if (dimension.equals(DumpSafeOverrides.OVERWORLD)) {
                values.add("#minecraft:is_forest");
                values.add("#minecraft:is_jungle");
                values.add("#minecraft:is_taiga");
                values.add("#minecraft:is_badlands");
                values.add("#minecraft:is_hill");
                values.add("#minecraft:is_savanna");
            } else if (dimension.equals(DumpSafeOverrides.NETHER)) {
                values.add("#minecraft:is_nether");
            }

            root.add("values", values);

            var biomeSelectorId = DepositData.biomeTagId(CreateRNS.ID, dimension);
            return List.of(DatapackFile.ofTag(DumpSafeOverrides.BIOME, biomeSelectorId, root));
        };
    }

    public static Supplier<List<DatapackFile>> depositProcessorLists() {
        return () -> {
            var blockEntries = DepositData.getBlocks();
            var files = new ArrayList<DatapackFile>(blockEntries.size());
            for (var b : blockEntries) {
                var root = new JsonObject();
                var processors = new JsonArray();
                var ruleProcessor = new JsonObject();
                ruleProcessor.addProperty("processor_type", "minecraft:rule");

                var rules = new JsonArray();
                var rule = new JsonObject();

                var inputPredicate = new JsonObject();
                inputPredicate.addProperty("block", DepositData.PROCESSOR_INPUT_BLOCK.toString());
                inputPredicate.addProperty("predicate_type", "minecraft:block_match");
                rule.add("input_predicate", inputPredicate);

                var locationPredicate = new JsonObject();
                locationPredicate.addProperty("predicate_type", "minecraft:always_true");
                rule.add("location_predicate", locationPredicate);

                var outputState = new JsonObject();
                outputState.addProperty("Name", b.blockId().toString());
                rule.add("output_state", outputState);

                rules.add(rule);
                ruleProcessor.add("rules", rules);
                processors.add(ruleProcessor);
                root.add("processors", processors);

                files.add(DatapackFile.ofResource(DumpSafeOverrides.PROCESSOR_LIST, b.processorId(), root));
            }
            return files;
        };
    }

    public static Supplier<List<DatapackFile>> depositStructures() {
        return () -> {
            var files = new ArrayList<DatapackFile>();
            var depositEntries = DepositData.getStructures();
            for (var s : depositEntries) {

                var root = new JsonObject();
                root.addProperty("type", CreateRNS.ID + ":deposit");
                root.addProperty("biomes", "#" + DepositData.biomeTagId(CreateRNS.ID, s.dimension()));
                root.addProperty("placement_strategy", s.placementStrategy().getSerializedName());

                int height = -s.depth();
                if (s.depthDeviation() == 0) {
                    root.addProperty("height", height);
                } else {
                    var heightRange = new JsonObject();
                    heightRange.addProperty("min", height - s.depthDeviation());
                    heightRange.addProperty("max", height + s.depthDeviation());
                    root.add("height", heightRange);
                }

                var structures = new JsonArray();
                for (var tw : s.templates()) {
                    var structure = new JsonObject();
                    structure.addProperty("id", tw.template().toString());
                    structure.addProperty("weight", tw.weight());
                    structure.addProperty("processor", DepositData.processorId(s.namespace(), s.keyword()).toString());
                    structures.add(structure);
                }
                root.add("structures", structures);

                files.add(DatapackFile.ofResource(DumpSafeOverrides.STRUCTURE, s.structureId(), root));
            }
            return files;
        };
    }

    public static Supplier<List<DatapackFile>> depositStructureTag() {
        return () -> {
            var root = new JsonObject();
            var values = new JsonArray();
            for (var s : DepositData.getStructures()) {
                values.add(s.structureId().toString());
            }
            root.add("values", values);
            return List.of(DatapackFile.ofTag(DumpSafeOverrides.STRUCTURE, DepositData.structureTagId(), root));
        };
    }

    public static Supplier<List<DatapackFile>> depositStructureSet(
            ResourceLocation dimension, int separation, int spacing, int salt
    ) {
        return () -> {
            var root = new JsonObject();

            var placement = new JsonObject();
            placement.addProperty("type", "minecraft:random_spread");
            placement.addProperty("separation", separation);
            placement.addProperty("spacing", spacing);
            placement.addProperty("salt", salt);
            root.add("placement", placement);

            var structures = new JsonArray();
            for (var s : DepositData.getScannableStructures(dimension)) {
                var e = new JsonObject();
                e.addProperty("structure", s.structureId().toString());
                e.addProperty("weight", s.weight());
                structures.add(e);
            }
            root.add("structures", structures);

            var ssId = DepositData.structureSetId(CreateRNS.ID, dimension);
            return List.of(DatapackFile.ofResource(DumpSafeOverrides.STRUCTURE_SET, ssId, root));
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
                if (!dim.equals(DumpSafeOverrides.OVERWORLD)) {
                    root.addProperty("dimension", dim.toString());
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
                            itemJson.addProperty("item", candidateIds.get(0));
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

                files.add(DatapackFile.of(r.recipeId().withPrefix("recipes/"), root));
            }

            return files;
        };
    }

    public static Supplier<List<DatapackFile>> depositSpecs() {
        return () -> {
            var structures = DepositData.getStructures();
            var files = new ArrayList<DatapackFile>(structures.size());
            for (var s : structures) {
                files.add(DatapackFile.ofResource(DumpSafeOverrides.DEPOSIT_SPEC, s.specId(), depositSpecJson(s)));
            }
            return files;
        };
    }

    public static JsonObject depositSpecJson(DepositData.StructureEntry s) {
        var root = new JsonObject();

        var scannerIconCandidates = s.scannerIconCandidates().stream()
                .map(c -> c.contains(":") ? c : "minecraft:" + c)
                .toList();
        if (scannerIconCandidates.size() == 1) {
            root.addProperty("scanner_icon_item", scannerIconCandidates.get(0));
        } else {
            var candidates = new JsonArray();
            for (var id : scannerIconCandidates) {
                candidates.add(id);
            }
            root.add("scanner_icon_item", candidates);
        }

        var mapIconCandidates = s.mapIconCandidates().stream()
                .map(c -> c.contains(":") ? c : "minecraft:" + c)
                .toList();
        if (mapIconCandidates.size() == 1) {
            root.addProperty("map_icon_item", mapIconCandidates.get(0));
        } else {
            var candidates = new JsonArray();
            for (var id : mapIconCandidates) {
                candidates.add(id);
            }
            root.add("map_icon_item", candidates);
        }

        root.addProperty("structure", s.structureId().toString());

        if (!s.dimension().equals(DumpSafeOverrides.OVERWORLD)) {
            root.addProperty("dimension", s.dimension().toString());
        }
        root.addProperty("scannable", s.isScannable().get());

        return root;
    }
}
