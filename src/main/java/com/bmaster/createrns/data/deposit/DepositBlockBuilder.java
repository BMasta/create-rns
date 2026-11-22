package com.bmaster.createrns.data.deposit;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.content.deposit.DepositBlock;
import com.bmaster.createrns.data.deposit.DepositResources.Recipe;
import com.bmaster.createrns.data.deposit.DepositResources.Structure;
import com.bmaster.createrns.data.pack.DynamicDatapackContent;
import com.bmaster.createrns.data.pack.DynamicDatapackDumpTool;
import com.bmaster.createrns.util.codec.ResourceOrTagLocation;
import com.mojang.logging.annotations.MethodsReturnNonnullByDefault;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.nullness.NonNullFunction;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.core.registries.BuiltInRegistries;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class DepositBlockBuilder {
    public static boolean dumpMode = false;

    public static DepositBlockBuilder create(String keyword) {
        return new DepositBlockBuilder(new DepositBuildingContext(keyword));
    }

    private final DepositBuildingContext ctx;
    private @Nullable BlockBuilder<DepositBlock, CreateRegistrate> delegate;
    private boolean disabled = false;
    private final ObjectOpenHashSet<String> compatIndicatorBlocks = new ObjectOpenHashSet<>();

    public DepositBlockBuilder disableByDefault() {
        this.disabled = true;
        return this;
    }

    public DepositBlockBuilder enableWhenBlockPresent(String name) {
        compatIndicatorBlocks.add(name);
        return this;
    }

    public DepositBlockBuilder transform(
            NonNullFunction<BlockBuilder<DepositBlock, CreateRegistrate>,
                    BlockBuilder<DepositBlock, CreateRegistrate>> transform
    ) {
        if (delegate != null) {
            delegate = transform.apply(delegate);
        }
        return this;
    }

    public DepositBlockBuilder attach(Consumer<DepositBuildingContext> attachment) {
        attachment.accept(ctx);
        return this;
    }

    /// Returns null only when in dump mode
    public BlockEntry<DepositBlock> register() {
        var blockId = DepositResources.blockId(CreateRNS.ID, ctx.depositKeyword);

        // Register block
        for (var existing : DynamicDatapackContent.RESOURCES.blocks) {
            if (existing.id().equals(blockId)) {
                throw new IllegalStateException("Conflicting deposit block entry already exists: " + ctx.depositKeyword);
            }
        }
        DynamicDatapackContent.RESOURCES.blocks.add(new DepositResources.Block(
                blockId,
                ctx.isEnabled
        ));
        var processorId = DepositResources.processorId(CreateRNS.ID, blockId);
        DynamicDatapackContent.RESOURCES.processors.add(new DepositResources.Processor(processorId, blockId));

        // Register structure
        for (var sb : ctx.structureBuilders) {
            if (sb.templates.isEmpty()) {
                throw new IllegalStateException("At least one template must be configured before registering");
            }
            if (sb.scannerIconItemCandidates.isEmpty()) {
                throw new IllegalStateException("At least one scanner icon must be configured before registering");
            }
            var structureId = DepositResources.structureId(CreateRNS.ID, sb.dimension, ctx.depositKeyword);
            for (var existing : DynamicDatapackContent.RESOURCES.structures) {
                if (existing.id().equals(structureId)) {
                    throw new IllegalStateException("Conflicting deposit structure entry already exists: " +
                            ctx.depositKeyword + "(" + sb.dimension.location() + ")");
                }
            }
            DynamicDatapackContent.RESOURCES.structures.add(new Structure(
                    structureId,
                    processorId,
                    sb.dimension,
                    new ResourceOrTagLocation(DepositResources.biomeTagId(CreateRNS.ID, sb.dimension), true),
                    sb.depth,
                    sb.depthDeviation,
                    sb.weight,
                    DepositResources.defaultPlacementStrategy(sb.dimension),
                    sb.templates
            ));
            DynamicDatapackContent.RESOURCES.specs.add(new DepositResources.Spec(
                    DepositResources.specId(structureId),
                    structureId,
                    sb.dimension,
                    // Enabled deposits are set as scannable
                    ctx.isEnabled,
                    sb.scannerIconItemCandidates,
                    // Use deposit block as map icon
                    List.of(new ResourceOrTagLocation(blockId, false))
            ));
        }

        // Register recipe
        for (var rb : ctx.recipeBuilders) {
            if (rb.yields.isEmpty()) throw new IllegalStateException("Mining recipe must define at least one yield");
            var recipeId = DepositResources.recipeId(CreateRNS.ID, rb.dimension, ctx.depositKeyword);
            for (var existing : DynamicDatapackContent.RESOURCES.recipes) {
                if (existing.id().equals(recipeId)) {
                    throw new IllegalStateException("Conflicting mining recipe entry already exists: " +
                            ctx.depositKeyword + "(" + rb.dimension.location() + ")");
                }
            }
            DynamicDatapackContent.RESOURCES.recipes.add(new Recipe(
                    recipeId,
                    blockId,
                    rb.dimension,
                    ctx.isEnabled,
                    rb.replacementBlockId,
                    rb.durability,
                    rb.yields
            ));
        }

        // Block is not registered with the registrate in dump mode
        // noinspection DataFlowIssue
        return delegate != null ? delegate.register() : null;
    }

    private DepositBlockBuilder(DepositBuildingContext ctx) {
        this.ctx = ctx;
        if (dumpMode) {
            ctx.isEnabled = () -> (!disabled && compatIndicatorBlocks.isEmpty()) || DynamicDatapackDumpTool.includeCompat();
        } else {
            delegate = CreateRNS.REGISTRATE.block(ctx.depositKeyword + "_deposit_block", DepositBlock::new);
            ctx.isEnabled = () -> BuiltInRegistries.BLOCK.keySet().stream().anyMatch(rl ->
                    !disabled && (compatIndicatorBlocks.isEmpty() || compatIndicatorBlocks.contains(rl.getPath())));
        }
    }

    public static class DepositBuildingContext {
        public final String depositKeyword;
        public Supplier<Boolean> isEnabled;

        public final List<DepositStructureBuilder> structureBuilders = new ArrayList<>();
        public final List<MiningRecipeBuilder> recipeBuilders = new ArrayList<>();

        public DepositBuildingContext(String depositKeyword) {
            this.depositKeyword = depositKeyword;
            isEnabled = () -> true;
        }
    }
}
