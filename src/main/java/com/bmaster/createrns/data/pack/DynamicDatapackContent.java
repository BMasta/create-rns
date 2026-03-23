package com.bmaster.createrns.data.pack;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.data.pack.DynamicDatapack.DatapackFile;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class DynamicDatapackContent {
    public static final String DEPOSIT_STRUCTURE_SET_NAME = "deposits";
    public static final int DEFAULT_DEP_SET_SALT = 591646342;

    private static final String HAS_DEPOSIT_TAG_PATH = "%s/tags/worldgen/biome/has_deposit.json";
    private static final String DEPOSIT_STRUCTURE_TAG_PATH = "%s/tags/worldgen/structure/deposits.json";
    private static final String DEPOSIT_STRUCTURE_SET_PATH = "%s/worldgen/structure_set/" + DEPOSIT_STRUCTURE_SET_NAME + ".json";
    private static final String DEPOSIT_STRUCTURE_PATH = "%s/worldgen/structure/deposit_%s.json";
    private static final String DEPOSIT_TEMPLATE_POOL_PATH = "%s/worldgen/template_pool/deposit_%s/start.json";
    private static final String DEPOSIT_PROCESSOR_LIST_PATH = "%s/worldgen/processor_list/%s.json";

    public static List<DatapackFile> depositBiomeTag(boolean disableGeneration) {
        var values = new JsonArray();
        var root = new JsonObject();
        if (!disableGeneration) {
            values.add("#minecraft:is_forest");
            values.add("#minecraft:is_jungle");
            values.add("#minecraft:is_taiga");
            values.add("#minecraft:is_badlands");
            values.add("#minecraft:is_hill");
            values.add("#minecraft:is_savanna");
        } else {
            root.addProperty("replace", true);
        }

        root.add("values", values);

        return List.of(new DatapackFile(HAS_DEPOSIT_TAG_PATH.formatted(CreateRNS.ID), root));
    }

    public static List<DatapackFile> depositProcessorLists() {
        var depositEntries = getEnabledDeposits();
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
            outputState.addProperty("Name", def.depositBlock().toString());
            rule.add("output_state", outputState);

            rules.add(rule);
            ruleProcessor.add("rules", rules);
            processors.add(ruleProcessor);
            root.add("processors", processors);

            files.add(new DatapackFile(
                    DEPOSIT_PROCESSOR_LIST_PATH.formatted(CreateRNS.ID, processorName(def.depositBlock())),
                    root
            ));
        }
        return files;
    }

    public static List<DatapackFile> depositTemplatePools() {
        var depositEntries = getEnabledDeposits();
        var files = new ArrayList<DatapackFile>(depositEntries.size());
        for (var def : depositEntries) {
            var root = new JsonObject();
            var elements = new JsonArray();

            for (var tw : def.weightedTemplates()) {
                var weighted = new JsonObject();
                var element = new JsonObject();
                element.addProperty("element_type", "minecraft:single_pool_element");
                element.addProperty("location", tw.template().toString());
                element.addProperty("processors", CreateRNS.ID + ":" + processorName(def.depositBlock()));
                element.addProperty("projection", "rigid");
                weighted.add("element", element);
                weighted.addProperty("weight", tw.weight());
                elements.add(weighted);
            }

            root.add("elements", elements);
            root.addProperty("fallback", "minecraft:empty");
            files.add(new DatapackFile(DEPOSIT_TEMPLATE_POOL_PATH.formatted(CreateRNS.ID, def.name()), root));
        }
        return files;
    }

    public static List<DatapackFile> depositStructures() {
        var depositEntries = getEnabledDeposits();
        var files = new ArrayList<DatapackFile>(depositEntries.size());
        for (var def : depositEntries) {
            var root = new JsonObject();
            root.addProperty("type", "minecraft:jigsaw");
            root.addProperty("biomes", "#" + CreateRNS.ID + ":has_deposit");
            root.addProperty("max_distance_from_center", 80);
            root.addProperty("project_start_to_heightmap", "OCEAN_FLOOR_WG");
            root.addProperty("size", 1);
            root.add("spawn_overrides", new JsonObject());

            var startHeight = new JsonObject();
            startHeight.addProperty("absolute", -def.depth());
            root.add("start_height", startHeight);

            root.addProperty("start_pool", CreateRNS.ID + ":deposit_" + def.name() + "/start");
            root.addProperty("step", "underground_ores");
            root.addProperty("terrain_adaptation", "none");
            root.addProperty("use_expansion_hack", false);

            files.add(new DatapackFile(DEPOSIT_STRUCTURE_PATH.formatted(CreateRNS.ID, def.name()), root));
        }
        return files;
    }

    public static DatapackFile depositStructureTag() {
        var root = new JsonObject();
        var values = new JsonArray();
        for (var def : getEnabledDeposits()) {
            values.add(CreateRNS.ID + ":deposit_" + def.name());
        }
        root.add("values", values);
        return new DatapackFile(DEPOSIT_STRUCTURE_TAG_PATH.formatted(CreateRNS.ID), root);
    }

    public static DatapackFile depositStructureSet(int separation, int spacing) {
        return depositStructureSet(separation, spacing, DEFAULT_DEP_SET_SALT);
    }

    public static DatapackFile depositStructureSet(int separation, int spacing, int salt) {
        var root = new JsonObject();

        var placement = new JsonObject();
        placement.addProperty("type", "minecraft:random_spread");
        placement.addProperty("separation", separation);
        placement.addProperty("spacing", spacing);
        placement.addProperty("salt", salt);
        root.add("placement", placement);

        var structures = new JsonArray();
        for (var def : getEnabledDeposits()) {
            var e = new JsonObject();
            e.addProperty("structure", CreateRNS.ID + ":deposit_" + def.name());
            e.addProperty("weight", def.weight());
            structures.add(e);
        }
        root.add("structures", structures);

        return new DatapackFile(DEPOSIT_STRUCTURE_SET_PATH.formatted(CreateRNS.ID), root);
    }

    private static String processorName(ResourceLocation depositBlock) {
        return "replace_with_" + depositBlock.getNamespace() + "_" + depositBlock.getPath();
    }

    private static List<DynamicDatapackDepositEntry.ConfiguredEntry> getEnabledDeposits() {
        return DynamicDatapackDepositEntry.getDeposits().stream()
                .filter(DynamicDatapackDepositEntry.ConfiguredEntry::isEnabled)
                .toList();
    }
}
