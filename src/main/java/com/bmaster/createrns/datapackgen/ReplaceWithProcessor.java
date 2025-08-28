package com.bmaster.createrns.datapackgen;

import com.google.gson.annotations.SerializedName;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public class ReplaceWithProcessor {
    public List<Processor> processors;

    public ReplaceWithProcessor(ResourceLocation matchBlockId, ResourceLocation replaceWithBlockId) {
        var rule = new Rule(
                new BlockMatchPredicate(matchBlockId.toString()),
                new AlwaysTruePredicate(),
                new OutputState(replaceWithBlockId.toString())
        );
        var processor = new Processor(List.of(rule));
        this.processors = List.of(processor);
    }

    public static final class Processor {
        @SerializedName("processor_type")
        public String processorType = "minecraft:rule";

        public List<Rule> rules;

        public Processor(List<Rule> rules) {
            this.rules = rules;
        }
    }

    public static final class Rule {
        @SerializedName("input_predicate")
        public BlockMatchPredicate inputPredicate;

        @SerializedName("location_predicate")
        public AlwaysTruePredicate locationPredicate;

        @SerializedName("output_state")
        public OutputState outputState;

        public Rule(BlockMatchPredicate in, AlwaysTruePredicate loc, OutputState out) {
            this.inputPredicate = in;
            this.locationPredicate = loc;
            this.outputState = out;
        }
    }

    public static final class BlockMatchPredicate {
        @SerializedName("predicate_type")
        public String predicateType = "minecraft:block_match";

        public String block;

        public BlockMatchPredicate(String block) {
            this.block = block;
        }
    }

    public static final class AlwaysTruePredicate {
        @SerializedName("predicate_type")
        public String predicateType = "minecraft:always_true";
    }

    public static final class OutputState {
        @SerializedName("Name")
        public String name;

        public OutputState(String name) {
            this.name = name;
        }
    }
}
