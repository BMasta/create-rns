package com.bmaster.createrns.datapackgen;

import java.util.List;

public class DepositStructureSet {
    public DepositStructureSet() {
    }

    public List<WeightedStructure> structures = List.of(
            new WeightedStructure("create_rns:deposit_iron", 1)
    );

    public Placement placement = new Placement();


    public static final class WeightedStructure {
        public String structure;
        public int weight;

        public WeightedStructure(String structure, int weight) {
            this.structure = structure;
            this.weight = weight;
        }
    }

    public static final class Placement {
        public String type = "minecraft:random_spread";
        public int spacing = 32;
        public int separation = 6;
        public int salt = 591646342;
    }
}
