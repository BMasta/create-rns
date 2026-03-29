package com.bmaster.createrns.data.gen.depositworldgen;

import com.bmaster.createrns.CreateRNS;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.ParametersAreNonnullByDefault;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class DepositWorldgenProvider implements DataProvider {
    public record NBT(ResourceLocation loc, int weight) {}

    public record Deposit(String name, ResourceLocation depositBlock, List<NBT> nbts, int depth, int weight) {}

    public record DepositSet(int separation, int spacing, int salt) {}

    protected static DepositSet setConf;
    protected static List<Deposit> depConf = new ArrayList<>();

    private static final Function<ResourceLocation, String> procName = (depBlock) ->
            "replace_with_" + depBlock.getNamespace() + "_" + depBlock.getPath();
    private static final Function<String, String> structName = (name) ->
            "deposit_" + name;

    private static final String DEPOSIT_PLACEHOLDER_BLOCK = "minecraft:end_stone";
    private static final String ALLOWED_BIOMES_TAG_PATH = "has_deposit";

    private final PackOutput output;

    public DepositWorldgenProvider(PackOutput output) {
        this.output = output;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        if (depConf.isEmpty() || setConf == null) return CompletableFuture.allOf();
        var futures = new java.util.ArrayList<CompletableFuture<?>>();
        var dataRoot = output.getOutputFolder(PackOutput.Target.DATA_PACK).resolve(CreateRNS.ID);

        for (var dc : depConf) {
            Path processorPath = dataRoot.resolve("worldgen/processor_list/" + procName.apply(dc.depositBlock) + ".json");
            futures.add(DataProvider.saveStable(cache, generateProcessor(dc), processorPath));

            Path structurePath = dataRoot.resolve("worldgen/structure/" + structName.apply(dc.name) + ".json");
            futures.add(DataProvider.saveStable(cache, generateStructure(dc), structurePath));
        }

        Path depTagPath = dataRoot.resolve("tags/worldgen/structure/deposits.json");
        futures.add(DataProvider.saveStable(cache, generateStructureTag(), depTagPath));

        Path setPath = dataRoot.resolve("worldgen/structure_set/deposits.json");
        futures.add(DataProvider.saveStable(cache, generateStructureSet(), setPath));

        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    @Override
    public String getName() {
        return CreateRNS.ID + "_deposit_worldgen";
    }

    private static JsonObject generateProcessor(Deposit conf) {
        JsonObject root = new JsonObject();
        JsonArray procs = new JsonArray();
        JsonObject procType = new JsonObject();
        procType.addProperty("processor_type", "minecraft:rule");

        JsonArray rules = new JsonArray();
        JsonObject rule = new JsonObject();

        JsonObject inputPredicate = new JsonObject();
        inputPredicate.addProperty("block", DEPOSIT_PLACEHOLDER_BLOCK);
        inputPredicate.addProperty("predicate_type", "minecraft:block_match");

        JsonObject locationPredicate = new JsonObject();
        locationPredicate.addProperty("predicate_type", "minecraft:always_true");

        JsonObject outputState = new JsonObject();
        outputState.addProperty("Name", conf.depositBlock.toString());

        rule.add("input_predicate", inputPredicate);
        rule.add("location_predicate", locationPredicate);
        rule.add("output_state", outputState);
        rules.add(rule);

        procType.add("rules", rules);
        procs.add(procType);
        root.add("processors", procs);
        return root;
    }

    private JsonObject generateStructure(Deposit conf) {
        JsonObject root = new JsonObject();
        root.addProperty("type", CreateRNS.ID + ":deposit");
        root.addProperty("biomes", "#" + CreateRNS.ID + ":" + ALLOWED_BIOMES_TAG_PATH);
        root.addProperty("placement_strategy", "overworld");
        root.addProperty("height", -conf.depth);

        var structures = new JsonArray();
        for (var nbt : conf.nbts) {
            var structure = new JsonObject();
            structure.addProperty("id", nbt.loc.toString());
            structure.addProperty("weight", nbt.weight);
            structure.addProperty("processor", CreateRNS.ID + ":" + procName.apply(conf.depositBlock));
            structures.add(structure);
        }
        root.add("structures", structures);

        return root;
    }

    private JsonObject generateStructureTag() {
        JsonArray values = new JsonArray();
        for (var dc : depConf) {
            values.add(CreateRNS.ID + ":" + structName.apply(dc.name));
        }
        JsonObject root = new JsonObject();
        root.add("values", values);
        return root;
    }

    private JsonObject generateStructureSet() {

        JsonArray entries = new JsonArray();
        for (var dc : depConf) {
            JsonObject e = new JsonObject();
            e.addProperty("structure", CreateRNS.ID + ":" + structName.apply(dc.name));
            e.addProperty("weight", dc.weight);
            entries.add(e);
        }

        JsonObject placement = new JsonObject();
        placement.addProperty("type", "minecraft:random_spread");
        placement.addProperty("salt", setConf.salt);
        placement.addProperty("spacing", setConf.spacing);
        placement.addProperty("separation", setConf.separation);

        JsonObject root = new JsonObject();
        root.add("structures", entries);
        root.add("placement", placement);

        return root;
    }
}
