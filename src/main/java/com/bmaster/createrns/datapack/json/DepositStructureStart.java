package com.bmaster.createrns.datapack.json;

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
        @SerializedName("element")
        public Element element;

        @SerializedName("weight")
        public int weight;


        public WeightedElement(String nbt, int weight, String processor) {
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
