package com.bmaster.createrns.data.pack;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.data.pack.DynamicDatapack.DatapackFile;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class DynamicDatapackContent {
    private static final String HAS_DEPOSIT_TAG_PATH = "%s/tags/worldgen/biome/has_deposit.json";

    public static List<DatapackFile> standardDepositBiomeTag() {
        var values = new JsonArray();
        values.add("#minecraft:is_forest");
        values.add("#minecraft:is_jungle");
        values.add("#minecraft:is_taiga");
        values.add("#minecraft:is_badlands");
        values.add("#minecraft:is_hill");
        values.add("#minecraft:is_savanna");

        var root = new JsonObject();
        root.add("values", values);

        return List.of(new DatapackFile(HAS_DEPOSIT_TAG_PATH.formatted(CreateRNS.ID), root));
    }

    /// Override the deposit biome tag to an empty tag that contains no biomes.
    /// This effectively disables deposit generation.
    public static List<DatapackFile> emptyDepositBiomeTag() {
        var values = new JsonArray();

        var root = new JsonObject();
        root.addProperty("replace", true);
        root.add("values", values);

        return List.of(new DatapackFile(HAS_DEPOSIT_TAG_PATH.formatted(CreateRNS.ID), root));
    }
}
