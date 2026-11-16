package com.bmaster.createrns.data.deposit;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.content.deposit.worldgen.PlacementStrategy;
import com.bmaster.createrns.util.codec.ResourceOrTagLocation;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.logging.annotations.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class DepositJson {
    public static JsonObject processorList(ResourceLocation inputBlock, ResourceLocation outputBlock) {
        var inputPredicate = new JsonObject();
        inputPredicate.addProperty("block", inputBlock.toString());
        inputPredicate.addProperty("predicate_type", "minecraft:block_match");
        var locationPredicate = new JsonObject();
        locationPredicate.addProperty("predicate_type", "minecraft:always_true");
        var outputState = new JsonObject();
        outputState.addProperty("Name", outputBlock.toString());

        var rule = new JsonObject();
        rule.add("input_predicate", inputPredicate);
        rule.add("location_predicate", locationPredicate);
        rule.add("output_state", outputState);
        var rules = new JsonArray();
        rules.add(rule);

        var processor = new JsonObject();
        processor.addProperty("processor_type", "minecraft:rule");
        processor.add("rules", rules);
        var processors = new JsonArray();
        processors.add(processor);
        var root = new JsonObject();
        root.add("processors", processors);
        return root;
    }

    public static JsonObject structure(
            ResourceOrTagLocation biomes, PlacementStrategy placementStrategy, int minHeight, int maxHeight,
            List<DepositData.WeightedTemplate> templates, ResourceLocation processorId
    ) {
        if (minHeight > maxHeight) throw new IllegalArgumentException("Minimum height must not exceed maximum height");

        var root = new JsonObject();
        root.addProperty("type", CreateRNS.ID + ":deposit");
        root.addProperty("biomes", biomes.toString());
        root.addProperty("placement_strategy", placementStrategy.getSerializedName());
        if (minHeight == maxHeight) {
            root.addProperty("height", minHeight);
        } else {
            var height = new JsonObject();
            height.addProperty("min", minHeight);
            height.addProperty("max", maxHeight);
            root.add("height", height);
        }

        var structures = new JsonArray();
        for (var template : templates) {
            var entry = new JsonObject();
            entry.addProperty("id", template.template().toString());
            entry.addProperty("weight", template.weight());
            entry.addProperty("processor", processorId.toString());
            structures.add(entry);
        }
        root.add("structures", structures);
        return root;
    }

    public static JsonObject spec(
            ResourceLocation structureId, ResourceKey<Level> dimension,
            List<ResourceOrTagLocation> scannerIconCandidates, List<ResourceOrTagLocation> mapIconCandidates,
            boolean scannable
    ) {
        var root = new JsonObject();
        root.add("scanner_icon_item", resourceOrArray(scannerIconCandidates));
        root.add("map_icon_item", resourceOrArray(mapIconCandidates));
        root.addProperty("structure", structureId.toString());
        if (!dimension.equals(Level.OVERWORLD)) {
            root.addProperty("dimension", dimension.location().toString());
        }
        root.addProperty("scannable", scannable);
        return root;
    }

    public static JsonObject structureSet(
            List<DepositData.WeightedStructure> structures, int separation, int spacing, int salt
    ) {
        var placement = new JsonObject();
        placement.addProperty("type", "minecraft:random_spread");
        placement.addProperty("separation", separation);
        placement.addProperty("spacing", spacing);
        placement.addProperty("salt", salt);

        var entries = new JsonArray();
        for (var structure : structures) {
            var entry = new JsonObject();
            entry.addProperty("structure", structure.structureId().toString());
            entry.addProperty("weight", structure.weight());
            entries.add(entry);
        }
        var root = new JsonObject();
        root.add("placement", placement);
        root.add("structures", entries);
        return root;
    }

    public static JsonObject tag(List<ResourceOrTagLocation> entries, boolean replace) {
        var root = new JsonObject();
        if (replace) root.addProperty("replace", true);
        var values = new JsonArray();
        for (var entry : entries) values.add(entry.toString());
        root.add("values", values);
        return root;
    }

    public static JsonElement resourceOrArray(List<ResourceOrTagLocation> entries) {
        if (entries.size() == 1) return new JsonPrimitive(entries.getFirst().toString());
        var values = new JsonArray();
        for (var entry : entries) values.add(entry.toString());
        return values;
    }

    private DepositJson() {
    }
}
