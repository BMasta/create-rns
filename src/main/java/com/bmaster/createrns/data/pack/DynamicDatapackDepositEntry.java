package com.bmaster.createrns.data.pack;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.content.deposit.DepositBlock;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.builders.BlockBuilder;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class DynamicDatapackDepositEntry {
    public static final ResourceLocation DEP_SMALL = CreateRNS.asResource("ore_deposit_small");
    public static final ResourceLocation DEP_MEDIUM = CreateRNS.asResource("ore_deposit_medium");
    public static final ResourceLocation DEP_LARGE = CreateRNS.asResource("ore_deposit_large");

    private static final List<ConfiguredEntry> DEPOSITS = new ArrayList<>();

    public static DynamicDatapackDepositEntry create(String structureId) {
        return new DynamicDatapackDepositEntry(structureId);
    }

    public static BlockBuilder<DepositBlock, CreateRegistrate> blockOnly(String name) {
        return CreateRNS.REGISTRATE.block(name, DepositBlock::new);
    }

    public static List<ConfiguredEntry> getDeposits() {
        return Collections.unmodifiableList(DEPOSITS);
    }

    private final String structureId;
    private final List<WeightedTemplate> weightedTemplates = new ArrayList<>();

    private int depth = 8;
    private int weight = 2;

    public DynamicDatapackDepositEntry depth(int depth) {
        this.depth = depth;
        return this;
    }

    public DynamicDatapackDepositEntry weight(int weight) {
        if (weight <= 0) throw new IllegalArgumentException("Deposit weight must be positive");
        this.weight = weight;
        return this;
    }

    public DynamicDatapackDepositEntry nbt(ResourceLocation template, int weight) {
        if (weight <= 0) throw new IllegalArgumentException("Template weight must be positive");
        weightedTemplates.add(new WeightedTemplate(template, weight));
        return this;
    }

    public BlockBuilder<DepositBlock, CreateRegistrate> block(String name) {
        var depositBlock = CreateRNS.asResource(name);

        if (weightedTemplates.isEmpty()) {
            throw new IllegalStateException("At least one template must be configured before registering");
        }
        var candidate = new ConfiguredEntry(structureId, depositBlock, depth, weight, List.copyOf(weightedTemplates));
        var existing = DEPOSITS.stream()
                .filter(d -> d.name().equals(structureId))
                .findFirst()
                .orElse(null);
        if (existing == null) {
            DEPOSITS.add(candidate);
        } else if (!existing.equals(candidate)) {
            throw new IllegalStateException("Conflicting dynamic deposit definition already exists: " + structureId);
        }

        return CreateRNS.REGISTRATE.block(name, DepositBlock::new);
    }

    private DynamicDatapackDepositEntry(String structureId) {
        this.structureId = structureId;
    }

    public record ConfiguredEntry(
            String name, ResourceLocation depositBlock, int depth, int weight, List<WeightedTemplate> weightedTemplates
    ) {}

    public record WeightedTemplate(ResourceLocation template, int weight) {}
}
