package com.bmaster.createrns.datapack;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class DepositStructureSet {
    @SerializedName("structures")
    public List<WeightedStructure> structures;

    @SerializedName("placement")
    public Placement placement = new Placement();

    public DepositStructureSet(List<WeightedStructure> structures) {
        this.structures = structures;
    }

    public static final class WeightedStructure {
        @SerializedName("structure")
        public String structure;

        @SerializedName("weight")
        public int weight;

        public WeightedStructure(String structure, int weight) {
            this.structure = structure;
            this.weight = weight;
        }
    }

    public static final class Placement {
        @SerializedName("type")
        public String type = "minecraft:random_spread";

        @SerializedName("spacing")
        public int spacing = 32;

        @SerializedName("separation")
        public int separation = 6;

        @SerializedName("salt")
        public int salt = 591646342;
    }
}
