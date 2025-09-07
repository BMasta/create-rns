package com.bmaster.createrns.datagen.pack.json;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class HasDepositBiomeTag {
    @SerializedName("values")
    public List<String> values;

    public HasDepositBiomeTag() {
        // Nothing water-related (draining water is a pain in the ass).
        // No steep mountains (building infrastructure is awkward).
        values = List.of(
            "#minecraft:is_forest",
            "#minecraft:is_jungle",
            "#minecraft:is_taiga",
            "#minecraft:is_badlands",
            "#minecraft:is_hill",
            "#minecraft:is_savanna"
        );
    }
}
