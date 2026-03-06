package com.bmaster.createrns;

import com.bmaster.createrns.content.deposit.DepositBlock;
import com.bmaster.createrns.content.deposit.mining.contraption.MinerBearingBlock;
import com.bmaster.createrns.content.deposit.mining.contraption.attachment.drillhead.DrillHeadBlock;
import com.bmaster.createrns.content.deposit.mining.contraption.attachment.resonance.buffer.ResonanceBufferBlock;
import com.bmaster.createrns.content.deposit.mining.contraption.attachment.resonance.propagator.ResonancePropagatorBlock;
import com.bmaster.createrns.content.deposit.mining.contraption.attachment.resonance.propagator.ResonancePropagatorMovementBehaviour;
import com.bmaster.createrns.content.deposit.mining.contraption.attachment.resonance.resonator.ResonatorBlock;
import com.bmaster.createrns.content.deposit.mining.contraption.attachment.resonance.resonator.ResonatorMovementBehaviour;
import com.bmaster.createrns.content.deposit.mining.contraption.attachment.resonance.resonator.ShatteringResonatorBlock;
import com.bmaster.createrns.content.deposit.mining.contraption.attachment.resonance.resonator.StabilizingResonatorBlock;
import com.bmaster.createrns.data.gen.depositworldgen.DepositSetConfigBuilder;
import com.bmaster.createrns.data.gen.depositworldgen.DepositStructureConfigBuilder;
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
import net.minecraftforge.registries.ForgeRegistries;

import static com.simibubi.create.api.behaviour.movement.MovementBehaviour.movementBehaviour;
import static com.simibubi.create.foundation.data.TagGen.axeOrPickaxe;
import static com.simibubi.create.foundation.data.TagGen.pickaxeOnly;

public class RNSBlocks {
    public static final BlockEntry<MinerBearingBlock> MINER_BEARING_BLOCK = CreateRNS.REGISTRATE.block(
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

    public static final BlockEntry<DrillHeadBlock> DRILL_HEAD_BLOCK = CreateRNS.REGISTRATE.block(
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

    public static final BlockEntry<ResonatorBlock> RESONATOR_BLOCK = CreateRNS.REGISTRATE.block(
                    "resonator", ResonatorBlock::new)
            .transform(resonatorBlock(MapColor.COLOR_PURPLE))
            .recipe((c, p) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, c.get())
                    .define('A', Items.AMETHYST_SHARD)
                    .define('R', AllItems.PRECISION_MECHANISM)
                    .define('C', AllBlocks.BRASS_CASING)
                    .pattern("A")
                    .pattern("R")
                    .pattern("C")
                    .unlockedBy("has_item", RegistrateRecipeProvider.has(AllItems.PRECISION_MECHANISM))
                    .save(p))
            .register();

    public static final BlockEntry<StabilizingResonatorBlock> STABILIZING_RESONATOR_BLOCK = CreateRNS.REGISTRATE.block(
                    "stabilizing_resonator", StabilizingResonatorBlock::new)
            .transform(resonatorBlock(MapColor.COLOR_CYAN))
            .register();

    public static final BlockEntry<ShatteringResonatorBlock> SHATTERING_RESONATOR_BLOCK = CreateRNS.REGISTRATE.block(
                    "shattering_resonator", ShatteringResonatorBlock::new)
            .transform(resonatorBlock(MapColor.COLOR_RED))
            .register();

    public static final BlockEntry<ResonanceBufferBlock> RESONANCE_BUFFER = CreateRNS.REGISTRATE.block(
                    "resonance_buffer", ResonanceBufferBlock::new)
            .initialProperties(SharedProperties::softMetal)
            .properties(p -> p.mapColor(MapColor.COLOR_PURPLE))
            .transform(pickaxeOnly())
            .simpleItem()
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
            .item()
            .model(AssetLookup::customItemModel)
            .build()
            .register();

    public static final BlockEntry<DepositBlock> IRON_DEPOSIT_BLOCK = CreateRNS.REGISTRATE.block(
                    "iron_deposit_block", DepositBlock::new)
            .transform(depositBlock(MapColor.RAW_IRON))
            .onRegister(d -> DepositStructureConfigBuilder
                    .create("iron")
                    .depositBlock(ForgeRegistries.BLOCKS.getKey(d))
                    .depth(8)
                    .weight(10)
                    .nbt(DepositStructureConfigBuilder.DEP_MEDIUM, 70)
                    .nbt(DepositStructureConfigBuilder.DEP_LARGE, 30)
                    .save())
            .register();

    public static final BlockEntry<DepositBlock> COPPER_DEPOSIT_BLOCK = CreateRNS.REGISTRATE.block(
                    "copper_deposit_block", DepositBlock::new)
            .transform(depositBlock(MapColor.COLOR_ORANGE))
            .onRegister(d -> DepositStructureConfigBuilder
                    .create("copper")
                    .depositBlock(ForgeRegistries.BLOCKS.getKey(d))
                    .depth(8)
                    .weight(5)
                    .nbt(DepositStructureConfigBuilder.DEP_MEDIUM, 70)
                    .nbt(DepositStructureConfigBuilder.DEP_LARGE, 30)
                    .save())
            .register();

    public static final BlockEntry<DepositBlock> ZINC_DEPOSIT_BLOCK = CreateRNS.REGISTRATE.block(
                    "zinc_deposit_block", DepositBlock::new)
            .transform(depositBlock(MapColor.GLOW_LICHEN))
            .onRegister(d -> DepositStructureConfigBuilder
                    .create("zinc")
                    .depositBlock(ForgeRegistries.BLOCKS.getKey(d))
                    .depth(8)
                    .weight(2)
                    .nbt(DepositStructureConfigBuilder.DEP_SMALL, 70)
                    .nbt(DepositStructureConfigBuilder.DEP_MEDIUM, 28)
                    .nbt(DepositStructureConfigBuilder.DEP_LARGE, 2)
                    .save())
            .register();

    public static final BlockEntry<DepositBlock> GOLD_DEPOSIT_BLOCK = CreateRNS.REGISTRATE.block(
                    "gold_deposit_block", DepositBlock::new)
            .transform(depositBlock(MapColor.GOLD))
            .onRegister(d -> DepositStructureConfigBuilder
                    .create("gold")
                    .depositBlock(ForgeRegistries.BLOCKS.getKey(d))
                    .depth(12)
                    .weight(2)
                    .nbt(DepositStructureConfigBuilder.DEP_SMALL, 70)
                    .nbt(DepositStructureConfigBuilder.DEP_MEDIUM, 28)
                    .nbt(DepositStructureConfigBuilder.DEP_LARGE, 2)
                    .save())
            .register();

    public static final BlockEntry<DepositBlock> REDSTONE_DEPOSIT_BLOCK = CreateRNS.REGISTRATE.block(
                    "redstone_deposit_block", DepositBlock::new)
            .transform(depositBlock(MapColor.FIRE))
            .onRegister(d -> DepositStructureConfigBuilder
                    .create("redstone")
                    .depositBlock(ForgeRegistries.BLOCKS.getKey(d))
                    .depth(12)
                    .weight(2)
                    .nbt(DepositStructureConfigBuilder.DEP_SMALL, 70)
                    .nbt(DepositStructureConfigBuilder.DEP_MEDIUM, 28)
                    .nbt(DepositStructureConfigBuilder.DEP_LARGE, 2)
                    .save())
            .register();

    public static final BlockEntry<DepositBlock> DEPLETED_DEPOSIT_BLOCK = CreateRNS.REGISTRATE.block(
                    "depleted_deposit_block", DepositBlock::new)
            .transform(depositBlock(MapColor.COLOR_BLACK))
            .register();

    static {
        // Must run after all deposit configs are saved
        DepositSetConfigBuilder
                .create()
                .save();
    }

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
