package com.bmaster.createrns.data.pack;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.content.deposit.mining.recipe.Yield.WeightedItem;
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
    private static final String DEPOSIT_BIOME_TAG_PATH = "%s/tags/worldgen/biome/%s.json";
    private static final String DEPOSIT_STRUCTURE_TAG_PATH = "%s/tags/worldgen/structure/deposits.json";
    private static final String DEPOSIT_STRUCTURE_SET_PATH = "%s/worldgen/structure_set/%s.json";
    private static final String DEPOSIT_STRUCTURE_PATH = "%s/worldgen/structure/deposit_%s.json";
    private static final String DEPOSIT_PROCESSOR_LIST_PATH = "%s/worldgen/processor_list/%s.json";
    private static final String MINING_RECIPE_PATH = "%s/recipes/%s.json";
    private static final String DEPOSIT_SPEC_PATH = "%s/%s/%s.json";

    private static final String BIOME_TAG_PREFIX = "has_deposit";

    private static final ResourceLocation DEPOSIT_SPEC_REGISTRY = ResourceLocation.fromNamespaceAndPath(
            CreateRNS.ID, "deposit_spec");

    public static Supplier<List<DatapackFile>> depositBiomeTag(DepositDimension dimension, boolean disableGeneration) {
        return () -> {
            var values = new JsonArray();
            var root = new JsonObject();
            if (disableGeneration) {
                root.addProperty("replace", true);
            } else {
                switch (dimension) {
                    case OVERWORLD -> {
                        values.add("#minecraft:is_forest");
                        values.add("#minecraft:is_jungle");
                        values.add("#minecraft:is_taiga");
                        values.add("#minecraft:is_badlands");
                        values.add("#minecraft:is_hill");
                        values.add("#minecraft:is_savanna");
                    }
                    case NETHER -> {
                        values.add("#minecraft:is_nether");
                    }
                }
            }

            root.add("values", values);

            return List.of(new DatapackFile(DEPOSIT_BIOME_TAG_PATH.formatted(
                    CreateRNS.ID, BIOME_TAG_PREFIX + dimension.suffix()), root));
        };
    }

    public static Supplier<List<DatapackFile>> depositProcessorLists() {
        return () -> {
            var depositEntries = DepositStructureBuilder.getEnabledDeposits();
            var files = new ArrayList<DatapackFile>(depositEntries.size());
            for (var def : depositEntries) {
                var root = new JsonObject();
                var processors = new JsonArray();
                var ruleProcessor = new JsonObject();
                ruleProcessor.addProperty("processor_type", "minecraft:rule");

                var rules = new JsonArray();
                var rule = new JsonObject();

                var inputPredicate = new JsonObject();
                inputPredicate.addProperty("block", "minecraft:end_stone");
                inputPredicate.addProperty("predicate_type", "minecraft:block_match");
                rule.add("input_predicate", inputPredicate);

                var locationPredicate = new JsonObject();
                locationPredicate.addProperty("predicate_type", "minecraft:always_true");
                rule.add("location_predicate", locationPredicate);

                var outputState = new JsonObject();
                outputState.addProperty("Name", def.structure().depositBlock().toString());
                rule.add("output_state", outputState);

                rules.add(rule);
                ruleProcessor.add("rules", rules);
                processors.add(ruleProcessor);
                root.add("processors", processors);

                files.add(new DatapackFile(
                        DEPOSIT_PROCESSOR_LIST_PATH.formatted(CreateRNS.ID, processorName(def.structure().depositBlock())),
                        root
                ));
            }
            return files;
        };
    }

    public static Supplier<List<DatapackFile>> depositStructures() {
        return () -> {
            var files = new ArrayList<DatapackFile>();
            var depositEntries = DepositStructureBuilder.getEnabledDeposits();
            for (var def : depositEntries) {
                var filename = def.structure().dimension().prefix() + def.name();
                var root = new JsonObject();
                root.addProperty("type", CreateRNS.ID + ":deposit");
                root.addProperty("biomes", "#" + CreateRNS.ID + ":" + BIOME_TAG_PREFIX + def.structure().dimension().suffix());
                root.addProperty("placement_strategy", def.structure().dimension().placement().getSerializedName());

                int height = -def.structure().depth();
                if (def.structure().depthDeviation() == 0) {
                    root.addProperty("height", height);
                } else {
                    var heightRange = new JsonObject();
                    heightRange.addProperty("min", height - def.structure().depthDeviation());
                    heightRange.addProperty("max", height + def.structure().depthDeviation());
                    root.add("height", heightRange);
                }

                var structures = new JsonArray();
                for (var tw : def.structure().weightedTemplates()) {
                    var structure = new JsonObject();
                    structure.addProperty("id", tw.template().toString());
                    structure.addProperty("weight", tw.weight());
                    structure.addProperty("processor", CreateRNS.ID + ":" + processorName(def.structure().depositBlock()));
                    structures.add(structure);
                }
                root.add("structures", structures);

                files.add(new DatapackFile(DEPOSIT_STRUCTURE_PATH.formatted(CreateRNS.ID, filename), root));
            }
            return files;
        };
    }

    public static Supplier<List<DatapackFile>> depositStructureTag() {
        return () -> {
            var root = new JsonObject();
            var values = new JsonArray();
            for (var def : DepositStructureBuilder.getEnabledDeposits()) {
                values.add(CreateRNS.ID + ":deposit_" + def.structure().dimension().prefix() + def.name());
            }
            root.add("values", values);
            return List.of(new DatapackFile(DEPOSIT_STRUCTURE_TAG_PATH.formatted(CreateRNS.ID), root));
        };
    }

    public static Supplier<List<DatapackFile>> depositStructureSet(
            DepositDimension dimension, int separation, int spacing, int salt
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
            for (var def : DepositStructureBuilder.getEnabledDeposits(dimension)) {
                var e = new JsonObject();
                e.addProperty("structure", CreateRNS.ID +
                        ":deposit_" + def.structure().dimension().prefix() + def.name());
                e.addProperty("weight", def.structure().weight());
                structures.add(e);
            }
            root.add("structures", structures);

            return List.of(new DatapackFile(DEPOSIT_STRUCTURE_SET_PATH.formatted(CreateRNS.ID,
                    dimension.prefix() + "deposits"), root));
        };
    }

    public static Supplier<List<DatapackFile>> miningRecipes() {
        return () -> {
            var recipes = MiningRecipeBuilder.getEnabledRecipes();
            var files = new ArrayList<DatapackFile>(recipes.size());
            for (var r : recipes) {
                var dim = r.recipe().dimension();
                var filename = r.recipe().dimension().prefix() + r.recipeId().getPath();
                var root = new JsonObject();

                root.addProperty("type", CreateRNS.ID + ":mining");
                root.addProperty("deposit_block", r.recipe().depositBlockId().toString());
                if (dim != DepositDimension.OVERWORLD) {
                    root.addProperty("dimension", dim.levelDimension().toString());
                }

                if (r.recipe().replacementBlockId() != null) {
                    root.addProperty("replace_when_depleted", r.recipe().replacementBlockId().toString());
                }
                if (r.recipe().durability() != null) {
                    var durability = new JsonObject();
                    durability.addProperty("core", r.recipe().durability().core());
                    durability.addProperty("edge", r.recipe().durability().edge());
                    durability.addProperty("random_spread", r.recipe().durability().randomSpread());
                    root.add("durability", durability);
                }

                var yields = new JsonArray();
                for (var yield : r.recipe().yields()) {
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
                            catalysts.add(catalyst);
                        }
                        yieldJson.add("catalysts", catalysts);
                    }
                    if (yield.jeiSlotColor() != 0) {
                        yieldJson.addProperty("jei_slot_color", yield.jeiSlotColor());
                    }

                    yields.add(yieldJson);
                }
                root.add("yields", yields);

                files.add(new DatapackFile(
                        MINING_RECIPE_PATH.formatted(r.recipeId().getNamespace(), filename),
                        root
                ));
            }

            return files;
        };
    }

    public static Supplier<List<DatapackFile>> depositSpecs() {
        return () -> {
            var specEntries = DepositSpecBuilder.getEnabledSpecs();
            var files = new ArrayList<DatapackFile>(specEntries.size());
            for (var def : specEntries) {
                var filename = def.dimension().prefix() + def.specId().getPath();
                var root = new JsonObject();
                var spec = def.spec();

                var scannerIconCandidates = spec.scannerIconCandidates().stream()
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

                var mapIconCandidates = spec.mapIconCandidates().stream()
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

                root.addProperty("structure", spec.structureId().toString());

                if (def.dimension() != DepositDimension.OVERWORLD) {
                    root.addProperty("dimension", def.dimension().levelDimension().toString());
                }

                var path = DEPOSIT_SPEC_PATH.formatted(def.specId().getNamespace(), CreateRNS.ID,
                        DEPOSIT_SPEC_REGISTRY.getPath() + "/" + filename);
                files.add(new DatapackFile(path, root));
            }
            return files;
        };
    }

    private static String processorName(ResourceLocation depositBlock) {
        return "replace_with_" + depositBlock.getNamespace() + "_" + depositBlock.getPath();
    }
}
