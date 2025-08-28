package com.bmaster.createrns.datapack;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class DepositStructureStart {
    @SerializedName("fallback")
    public String fallback;

    @SerializedName("elements")
    public List<WeightedElement> elements;

    public DepositStructureStart(List<WeightedElement> elements) {
        this.fallback = "minecraft:empty";
        this.elements = elements;
    }

    public static final class WeightedElement {
        @SerializedName("weight")
        public int weight;

        @SerializedName("element")
        public Element element;

        public WeightedElement(int weight, String nbt, String processor) {
            this.weight = weight;

            this.element = new Element(nbt, processor);
        }
    }

    public static final class Element {
        @SerializedName("element_type")
        public String elementType = "minecraft:single_pool_element";

        @SerializedName("location")
        public String location;

        @SerializedName("projection")
        public String projection = "rigid";

        @SerializedName("processors")
        public String processors;

        public Element(String nbt, String processor) {
            this.location = nbt;
            this.processors = processor;
        }
    }
}
