package com.bmaster.createrns.datapack;

import com.google.gson.annotations.SerializedName;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Set;

public class ReplaceWithProcessor {
    @SerializedName("processors")
    public List<Processor> processors;

    public ReplaceWithProcessor(String matchBlockId, String replaceWithBlockId) {
        var processor = new Processor(matchBlockId, replaceWithBlockId);
        this.processors = List.of(processor);
    }

    public static final class Processor {
        @SerializedName("processor_type")
        public String processorType = "minecraft:rule";

        @SerializedName("rules")
        public List<Rule> rules;

        public Processor(String matchBlockId, String replaceWithBlockId) {
            var rule = new Rule(
                    new BlockMatchPredicate(matchBlockId),
                    new AlwaysTruePredicate(),
                    new OutputState(replaceWithBlockId)
            );
            this.rules = List.of(rule);
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
