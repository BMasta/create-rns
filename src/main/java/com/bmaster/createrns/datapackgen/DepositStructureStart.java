package com.bmaster.createrns.datapackgen;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class DepositStructureStart {
    public String fallback;
    public List<WeightedElement> elements;

    public DepositStructureStart(List<WeightedElement> elements) {
        this.fallback = "minecraft:empty";
        this.elements = elements;
    }

    // ---------- nested types ----------

    public static final class WeightedElement {
        public int weight;
        public Element element;

        public WeightedElement(int weight, Element element) {
            this.weight = weight;
            this.element = element;
        }
    }

    public static final class Element {
        @SerializedName("element_type")
        public String elementType = "minecraft:single_pool_element";

        public String location = "create_rns:ore_deposit_medium";

        public String projection = "rigid";

        public String processors = "create_rns:deposit/iron";

        public Element() {
        }
    }
}
