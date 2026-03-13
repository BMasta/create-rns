package com.bmaster.createrns;

import com.bmaster.createrns.content.deposit.DepositBlock;
import com.bmaster.createrns.content.deposit.mining.contraption.MinerBearingBlock;
import com.bmaster.createrns.content.deposit.mining.contraption.attachment.drillhead.DrillHeadBlock;
import com.bmaster.createrns.content.deposit.mining.contraption.attachment.drillhead.DrillHeadPartBlock;
import com.bmaster.createrns.content.deposit.mining.contraption.attachment.resonance.buffer.ResonanceBufferBlock;
import com.bmaster.createrns.content.deposit.mining.contraption.attachment.resonance.propagator.ResonancePropagatorBlock;
import com.bmaster.createrns.content.deposit.mining.contraption.attachment.resonance.propagator.ResonancePropagatorMovementBehaviour;
import com.bmaster.createrns.content.deposit.mining.contraption.attachment.resonance.resonator.ResonatorBlock;
import com.bmaster.createrns.content.deposit.mining.contraption.attachment.resonance.resonator.ResonatorMovementBehaviour;
import com.bmaster.createrns.content.deposit.mining.contraption.attachment.resonance.resonator.ShatteringResonatorBlock;
import com.bmaster.createrns.content.deposit.mining.contraption.attachment.resonance.resonator.StabilizingResonatorBlock;
import com.bmaster.createrns.data.pack.DynamicDatapackDepositEntry;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllTags;
import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.content.contraptions.bearing.StabilizedBearingMovementBehaviour;
import com.simibubi.create.foundation.data.AssetLookup;
import com.simibubi.create.foundation.data.SharedProperties;
import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.nullness.NonNullFunction;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.common.Tags;

import javax.annotation.ParametersAreNonnullByDefault;

import static com.simibubi.create.api.behaviour.movement.MovementBehaviour.movementBehaviour;
import static com.simibubi.create.foundation.data.TagGen.axeOrPickaxe;
import static com.simibubi.create.foundation.data.TagGen.pickaxeOnly;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class RNSBlocks {
    public static final BlockEntry<MinerBearingBlock> MINER_BEARING = CreateRNS.REGISTRATE.block(
                    "miner_bearing", MinerBearingBlock::new)
            .initialProperties(SharedProperties::stone)
            .properties(p -> p
                    .mapColor(MapColor.PODZOL)
                    .noOcclusion())
            .transform(axeOrPickaxe())
            .tag(AllTags.AllBlockTags.SAFE_NBT.tag)
            .onRegister((b) -> BlockStressValues.IMPACTS.register(b, () -> 32))
            .onRegister(movementBehaviour(new StabilizedBearingMovementBehaviour()))
            .blockstate((c, p) ->
                    p.directionalBlock(c.get(), AssetLookup.partialBaseModel(c, p)))
            .item()
            .model(AssetLookup::customItemModel)
            .recipe((c, p) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, c.get())
                    .define('W', ItemTags.WOODEN_SLABS)
                    .define('S', Tags.Items.STONE)
                    .define('A', Items.AMETHYST_SHARD)
                    .define('F', AllBlocks.SHAFT)
                    .pattern(" W ")
                    .pattern("ASA")
                    .pattern(" F ")
                    .unlockedBy("has_item", RegistrateRecipeProvider.has(AllItems.ANDESITE_ALLOY))
                    .save(p))
            .build()
            .register();

    public static final BlockEntry<DrillHeadBlock> DRILL_HEAD = CreateRNS.REGISTRATE.block(
                    "drill_head", DrillHeadBlock::new)
            .initialProperties(() -> Blocks.IRON_BLOCK)
            .properties(p -> p
                    .mapColor(MapColor.COLOR_GRAY)
                    .noOcclusion())
            .transform(pickaxeOnly())
            .blockstate((c, p) ->
                    p.horizontalFaceBlock(c.get(), AssetLookup.standardModel(c, p)))
            .simpleItem()
            .recipe((c, p) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, c.get())
                    .define('I', Items.IRON_INGOT)
                    .define('A', AllItems.ANDESITE_ALLOY)
                    .pattern("III")
                    .pattern("AIA")
                    .pattern("AAA")
                    .unlockedBy("has_item", RegistrateRecipeProvider.has(AllItems.ANDESITE_ALLOY))
                    .save(p))
            .register();

    public static final BlockEntry<DrillHeadPartBlock> DRILL_HEAD_PART = CreateRNS.REGISTRATE.block(
                    "drill_head_part", DrillHeadPartBlock::new)
            .initialProperties(() -> Blocks.IRON_BLOCK)
            .properties(p -> p
                    .mapColor(MapColor.COLOR_GRAY)
                    .noOcclusion()
                    .noLootTable())
            .transform(pickaxeOnly())
            .blockstate((c, p) ->
                    p.simpleBlock(c.get(), p.models().getExistingFile(p.modLoc("block/drill_head"))))
            .register();

    public static final BlockEntry<ResonatorBlock> RESONATOR = CreateRNS.REGISTRATE.block(
                    "resonator", ResonatorBlock::new)
            .transform(resonatorBlock(MapColor.COLOR_PURPLE))
            .recipe((c, p) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, c.get())
                    .define('R', RNSItems.POLISHED_RESONANT_AMETHYST)
                    .define('A', AllItems.ANDESITE_ALLOY)
                    .define('I', AllItems.IRON_SHEET)
                    .pattern("AIA")
                    .pattern("IRI")
                    .pattern("AIA")
                    .unlockedBy("has_item", RegistrateRecipeProvider.has(RNSItems.POLISHED_RESONANT_AMETHYST))
                    .save(p))
            .register();

    public static final BlockEntry<StabilizingResonatorBlock> STABILIZING_RESONATOR = CreateRNS.REGISTRATE.block(
                    "stabilizing_resonator", StabilizingResonatorBlock::new)
            .transform(resonatorBlock(MapColor.COLOR_CYAN))
            .recipe((c, p) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, c.get())
                    .define('R', RNSBlocks.RESONATOR)
                    .define('S', AllItems.STURDY_SHEET)
                    .define('B', AllItems.BRASS_SHEET)
                    .define('D', Items.DIAMOND)
                    .pattern("SBS")
                    .pattern("DRD")
                    .pattern("SBS")
                    .unlockedBy("has_item", RegistrateRecipeProvider.has(RNSBlocks.RESONATOR))
                    .save(p))
            .register();

    public static final BlockEntry<ShatteringResonatorBlock> SHATTERING_RESONATOR = CreateRNS.REGISTRATE.block(
                    "shattering_resonator", ShatteringResonatorBlock::new)
            .transform(resonatorBlock(MapColor.COLOR_RED))
            .recipe((c, p) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, c.get())
                    .define('R', RNSBlocks.RESONATOR)
                    .define('S', AllItems.STURDY_SHEET)
                    .define('B', AllItems.BRASS_SHEET)
                    .define('Q', AllItems.POLISHED_ROSE_QUARTZ)
                    .pattern("SBS")
                    .pattern("QRQ")
                    .pattern("SBS")
                    .unlockedBy("has_item", RegistrateRecipeProvider.has(RNSBlocks.RESONATOR))
                    .save(p))
            .register();

    public static final BlockEntry<ResonancePropagatorBlock> RESONANCE_PROPAGATOR = CreateRNS.REGISTRATE.block(
                    "resonance_propagator", ResonancePropagatorBlock::new)
            .initialProperties(SharedProperties::softMetal)
            .properties(p -> p
                    .mapColor(MapColor.COLOR_PURPLE)
                    .noOcclusion())
            .transform(pickaxeOnly())
            .blockstate((c, p) ->
                    p.horizontalFaceBlock(c.get(), AssetLookup.partialBaseModel(c, p)))
            .onRegister(movementBehaviour(new ResonancePropagatorMovementBehaviour()))
            .recipe((c, p) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, c.get())
                    .define('A', RNSItems.POLISHED_RESONANT_AMETHYST)
                    .define('E', AllItems.ELECTRON_TUBE)
                    .define('C', AllBlocks.BRASS_CASING)
                    .pattern("A")
                    .pattern("E")
                    .pattern("C")
                    .unlockedBy("has_item", RegistrateRecipeProvider.has(RNSBlocks.RESONATOR))
                    .save(p))
            .item()
            .model(AssetLookup::customItemModel)
            .build()
            .register();

    public static final BlockEntry<ResonanceBufferBlock> RESONANCE_BUFFER = CreateRNS.REGISTRATE.block(
                    "resonance_buffer", ResonanceBufferBlock::new)
            .initialProperties(SharedProperties::softMetal)
            .properties(p -> p.mapColor(MapColor.COLOR_PURPLE))
            .transform(pickaxeOnly())
            .simpleItem()
            .register();

    public static final BlockEntry<DepositBlock> IRON_DEPOSIT = DynamicDatapackDepositEntry
            .create("iron")
            .depth(8)
            .weight(10)
            .nbt(DynamicDatapackDepositEntry.DEP_MEDIUM, 70)
            .nbt(DynamicDatapackDepositEntry.DEP_LARGE, 30)
            .block("iron_deposit_block")
            .transform(depositBlock(MapColor.RAW_IRON))
            .register();

    public static final BlockEntry<DepositBlock> COPPER_DEPOSIT = DynamicDatapackDepositEntry
            .create("copper")
            .depth(8)
            .weight(5)
            .nbt(DynamicDatapackDepositEntry.DEP_MEDIUM, 70)
            .nbt(DynamicDatapackDepositEntry.DEP_LARGE, 30)
            .block("copper_deposit_block")
            .transform(depositBlock(MapColor.COLOR_ORANGE))
            .register();

    public static final BlockEntry<DepositBlock> ZINC_DEPOSIT = DynamicDatapackDepositEntry
            .create("zinc")
            .depth(8)
            .weight(2)
            .nbt(DynamicDatapackDepositEntry.DEP_SMALL, 70)
            .nbt(DynamicDatapackDepositEntry.DEP_MEDIUM, 28)
            .nbt(DynamicDatapackDepositEntry.DEP_LARGE, 2)
            .block("zinc_deposit_block")
            .transform(depositBlock(MapColor.GLOW_LICHEN))
            .register();

    public static final BlockEntry<DepositBlock> GOLD_DEPOSIT = DynamicDatapackDepositEntry
            .create("gold")
            .depth(12)
            .weight(2)
            .nbt(DynamicDatapackDepositEntry.DEP_SMALL, 70)
            .nbt(DynamicDatapackDepositEntry.DEP_MEDIUM, 28)
            .nbt(DynamicDatapackDepositEntry.DEP_LARGE, 2)
            .block("gold_deposit_block")
            .transform(depositBlock(MapColor.GOLD))
            .register();

    public static final BlockEntry<DepositBlock> REDSTONE_DEPOSIT = DynamicDatapackDepositEntry
            .create("redstone")
            .depth(12)
            .weight(2)
            .nbt(DynamicDatapackDepositEntry.DEP_SMALL, 70)
            .nbt(DynamicDatapackDepositEntry.DEP_MEDIUM, 28)
            .nbt(DynamicDatapackDepositEntry.DEP_LARGE, 2)
            .block("redstone_deposit_block")
            .transform(depositBlock(MapColor.FIRE))
            .register();

    public static final BlockEntry<DepositBlock> DEPLETED_DEPOSIT = DynamicDatapackDepositEntry
            .blockOnly("depleted_deposit_block")
            .transform(depositBlock(MapColor.COLOR_BLACK))
            .register();

    public static <T extends Block, P> NonNullFunction<BlockBuilder<T, P>, BlockBuilder<T, P>> depositBlock(MapColor mapColor) {
        return b -> b
                .initialProperties(() -> Blocks.RAW_IRON_BLOCK)
                .properties(p -> p
                        .mapColor(mapColor)
                        .strength(50.0F, 1200f)
                        .pushReaction(PushReaction.BLOCK)
                        .noLootTable())
                .transform(pickaxeOnly())
                .tag(BlockTags.NEEDS_DIAMOND_TOOL)
                .tag(RNSTags.Block.DEPOSIT_BLOCKS)
                .item()
                .build();
    }

    public static <T extends Block, P> NonNullFunction<BlockBuilder<T, P>, BlockBuilder<T, P>> resonatorBlock(MapColor mapColor) {
        return builder -> builder
                .initialProperties(SharedProperties::softMetal)
                .properties(p -> p
                        .mapColor(mapColor)
                        .noOcclusion())
                .transform(pickaxeOnly())
                .blockstate((c, p) ->
                        p.simpleBlock(c.getEntry(), AssetLookup.partialBaseModel(c, p)))
                .onRegister(movementBehaviour(new ResonatorMovementBehaviour()))
                .item()
                .model(AssetLookup::customItemModel)
                .build();
    }

    public static void register() {
    }
}
