package com.bmaster.createrns.data.pack;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.content.deposit.DepositBlock;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.nullness.NonNullFunction;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class DynamicDatapackDepositEntry {
    public static final ResourceLocation DEP_SMALL = CreateRNS.asResource("ore_deposit_small");
    public static final ResourceLocation DEP_MEDIUM = CreateRNS.asResource("ore_deposit_medium");
    public static final ResourceLocation DEP_LARGE = CreateRNS.asResource("ore_deposit_large");
    public static boolean dumpMode = false;

    private static final List<ConfiguredEntry> DEPOSITS = new ArrayList<>();

    public static DynamicDatapackDepositEntry create(String structureId) {
        return new DynamicDatapackDepositEntry(structureId);
    }

    public static DepositBlockBuilder blockOnly(String name) {
        return new DepositBlockBuilder(name, null);
    }

    public static List<ConfiguredEntry> getDeposits() {
        return Collections.unmodifiableList(DEPOSITS);
    }

    private final String structureId;
    private final List<WeightedTemplate> weightedTemplates = new ArrayList<>();

    private int depth = 8;
    private int weight = 2;
    private @Nullable String requiredModId;

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

    public DynamicDatapackDepositEntry requireMod(String modId) {
        if (modId.isBlank()) throw new IllegalArgumentException("Required mod id cannot be blank");
        requiredModId = modId;
        return this;
    }

    public DepositBlockBuilder block(String name) {
        var depositBlock = CreateRNS.asResource(name);

        if (weightedTemplates.isEmpty()) {
            throw new IllegalStateException("At least one template must be configured before registering");
        }
        var candidate = new ConfiguredEntry(
                structureId, depositBlock, depth, weight, requiredModId, List.copyOf(weightedTemplates));
        var existing = DEPOSITS.stream()
                .filter(d -> d.name().equals(structureId))
                .findFirst()
                .orElse(null);
        if (existing == null) {
            DEPOSITS.add(candidate);
        } else if (!existing.equals(candidate)) {
            throw new IllegalStateException("Conflicting dynamic deposit definition already exists: " + structureId);
        }

        return new DepositBlockBuilder(name, requiredModId);
    }

    private DynamicDatapackDepositEntry(String structureId) {
        this.structureId = structureId;
    }

    public record ConfiguredEntry(
            String name, ResourceLocation depositBlock, int depth, int weight, @Nullable String requiredModId,
            List<WeightedTemplate> weightedTemplates
    ) {
        public boolean isEnabled() {
            if (dumpMode) {
                var enabledMods = DynamicDatapackDumpTool.getEnabledMods();
                return requiredModId == null || enabledMods == null || enabledMods.contains(requiredModId);
            }
            var modList = ModList.get();
            return requiredModId == null || modList.isLoaded(requiredModId);
        }
    }

    public record WeightedTemplate(ResourceLocation template, int weight) {}

    public static class DepositBlockBuilder {
        private final ResourceLocation depositBlockId;
        private @Nullable String requiredModId;
        private @Nullable BlockBuilder<DepositBlock, CreateRegistrate> delegate;

        public DepositBlockBuilder(String depositName, @Nullable String requiredModId) {
            this.depositBlockId = CreateRNS.asResource(depositName);
            this.requiredModId = requiredModId;
            if (!dumpMode) {
                delegate = CreateRNS.REGISTRATE.block(depositName, DepositBlock::new);
            }
        }

        public DepositBlockBuilder transform(
                NonNullFunction<BlockBuilder<DepositBlock, CreateRegistrate>,
                        BlockBuilder<DepositBlock, CreateRegistrate>> transform
        ) {
            if (delegate != null && isRequiredModSatisfied()) {
                delegate = transform.apply(delegate);
            }
            return this;
        }

        public DepositBlockBuilder requireMod(String modId) {
            if (modId.isBlank()) throw new IllegalArgumentException("Required mod id cannot be blank");
            requiredModId = modId;
            return this;
        }

        public DepositBlockBuilder recipe(Consumer<DepositBlockBuildingContext> ctx) {
            ctx.accept(new DepositBlockBuildingContext(depositBlockId, requiredModId));
            return this;
        }

        public @Nullable BlockEntry<DepositBlock> registerOrNull() {
            if (delegate == null) return null;
            if (!isRequiredModSatisfied()) return null;
            return delegate.register();
        }

        /// Returns null only when in dump mode
        public @Nullable BlockEntry<DepositBlock> registerOrThrow() {
            if (delegate == null) return null;
            if (!isRequiredModSatisfied()) {
                throw new IllegalStateException(
                        "Cannot register compat deposit block " + depositBlockId + " because required mod "
                                + requiredModId + " is not loaded");
            }
            return delegate.register();
        }

        private boolean isRequiredModSatisfied() {
            return requiredModId == null || ModList.get().isLoaded(requiredModId);
        }
    }

    public record DepositBlockBuildingContext(ResourceLocation depositBlockId, @Nullable String requiredModId) {
    }
}
