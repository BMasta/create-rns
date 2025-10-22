package com.bmaster.createrns;

import com.bmaster.createrns.compat.ponder.RNSPonderPlugin;
import com.bmaster.createrns.deposit.DepositBlock;
import com.bmaster.createrns.deposit.LevelDepositData;
import com.bmaster.createrns.item.DepositScanner.DepositScannerItem;
import com.bmaster.createrns.mining.miner.impl.*;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.foundation.data.AssetLookup;
import com.simibubi.create.foundation.data.SharedProperties;
import com.simibubi.create.foundation.item.ItemDescription;
import com.simibubi.create.foundation.item.KineticStats;
import com.simibubi.create.foundation.item.TooltipModifier;
import com.simibubi.create.foundation.utility.DistExecutor;
import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import com.tterrag.registrate.util.entry.*;
import com.tterrag.registrate.util.nullness.NonNullFunction;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.lang.FontHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

import static com.simibubi.create.foundation.data.TagGen.*;

public class RNSContent {
    // Partial models
    public static final PartialModel MINER_MK1_DRILL = PartialModel.of(
            ResourceLocation.fromNamespaceAndPath(CreateRNS.MOD_ID, "block/miner_mk1/drill_head"));

    public static final PartialModel MINER_MK2_DRILL = PartialModel.of(
            ResourceLocation.fromNamespaceAndPath(CreateRNS.MOD_ID, "block/miner_mk2/drill_head"));

    // Item tooltips
    static {
        CreateRNS.REGISTRATE.setTooltipModifierFactory(item ->
                new ItemDescription.Modifier(item, FontHelper.Palette.STANDARD_CREATE)
                        .andThen(TooltipModifier.mapNull(KineticStats.create(item))));
    }

    // Level attachments
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, CreateRNS.MOD_ID);

    public static final Supplier<AttachmentType<LevelDepositData>> LEVEL_DEPOSIT_DATA = ATTACHMENT_TYPES.register(
            "level_deposit_data", () -> AttachmentType.serializable(LevelDepositData::new).build());

    // Creative tabs
    public static final RegistryEntry<CreativeModeTab, CreativeModeTab> MAIN_TAB = CreateRNS.REGISTRATE.defaultCreativeTab(
                    CreateRNS.MOD_ID, c -> c
                            .icon(RNSContent.RESONANT_MECHANISM::asStack)
                            .title(Component.translatable("creativetab.%s".formatted(CreateRNS.MOD_ID)))
                            .build())
            .register();

    // Items
    public static final ItemEntry<DepositScannerItem> DEPOSIT_SCANNER_ITEM = CreateRNS.REGISTRATE.item(
                    "deposit_scanner", DepositScannerItem::new)
            .properties(p -> p.stacksTo(1))
            .model(AssetLookup.itemModelWithPartials())
            .recipe((c, p) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, c.get())
                    .define('E', AllItems.ELECTRON_TUBE)
                    .define('W', AllBlocks.COGWHEEL)
                    .define('C', AllBlocks.ANDESITE_CASING)
                    .define('T', AllItems.TRANSMITTER)
                    .pattern(" E ")
                    .pattern("TWT")
                    .pattern(" C ")
                    .unlockedBy("has_electron_tube", RegistrateRecipeProvider.has(AllItems.ELECTRON_TUBE))
                    .save(p))
            .register();

    public static final ItemEntry<Item> RESONANT_MECHANISM = CreateRNS.REGISTRATE.item(
                    "resonant_mechanism", Item::new)
            .recipe((c, p) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, c.get())
                    .define('A', Items.AMETHYST_SHARD)
                    .define('M', AllItems.PRECISION_MECHANISM)
                    .pattern(" A ")
                    .pattern("AMA")
                    .pattern(" A ")
                    .unlockedBy("has_item", RegistrateRecipeProvider.has(AllItems.PRECISION_MECHANISM))
                    .save(p))
            .register();

    public static final ItemEntry<Item> IMPURE_IRON_ORE = CreateRNS.REGISTRATE.item(
            "impure_iron_ore", Item::new).tag(RNSTags.Item.IMPURE_ORES).register();
    public static final ItemEntry<Item> IMPURE_COPPER_ORE = CreateRNS.REGISTRATE.item(
            "impure_copper_ore", Item::new).tag(RNSTags.Item.IMPURE_ORES).register();
    public static final ItemEntry<Item> IMPURE_ZINC_ORE = CreateRNS.REGISTRATE.item(
            "impure_zinc_ore", Item::new).tag(RNSTags.Item.IMPURE_ORES).register();
    public static final ItemEntry<Item> IMPURE_GOLD_ORE = CreateRNS.REGISTRATE.item(
            "impure_gold_ore", Item::new).tag(RNSTags.Item.IMPURE_ORES).register();
    public static final ItemEntry<Item> IMPURE_REDSTONE_DUST = CreateRNS.REGISTRATE.item(
            "impure_redstone_dust", Item::new).tag(RNSTags.Item.IMPURE_ORES).register();

    // Yoinked from tech reborn
    public static final ItemEntry<Item> REDSTONE_SMALL_DUST = CreateRNS.REGISTRATE.item(
                    "redstone_small_dust", Item::new).register();

    // Blocks
    public static final BlockEntry<MinerMk1Block> MINER_MK1_BLOCK = CreateRNS.REGISTRATE.block("miner_mk1",
                    MinerMk1Block::new)
            .transform(minerBlockCommon())
            .onRegister((b) -> BlockStressValues.IMPACTS.register(b, () -> 2))
            .item()
            .model(AssetLookup::customItemModel)
            .recipe((c, p) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, c.get())
                    .define('F', AllBlocks.ANDESITE_FUNNEL)
                    .define('C', AllBlocks.COGWHEEL)
                    .define('D', AllBlocks.MECHANICAL_DRILL)
                    .pattern("F")
                    .pattern("C")
                    .pattern("D")
                    .unlockedBy("has_item", RegistrateRecipeProvider.has(AllBlocks.MECHANICAL_DRILL))
                    .save(p))
            .build()
            .register();

    public static final BlockEntry<MinerMk2Block> MINER_MK2_BLOCK = CreateRNS.REGISTRATE.block("miner_mk2",
                    MinerMk2Block::new)
            .transform(minerBlockCommon())
            .onRegister((b) -> BlockStressValues.IMPACTS.register(b, () -> 2))
            .item()
            .model(AssetLookup::customItemModel)
            .recipe((c, p) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, c.get())
                    .define('F', AllBlocks.BRASS_FUNNEL)
                    .define('R', RESONANT_MECHANISM)
                    .define('M', MINER_MK1_BLOCK)
                    .pattern("F")
                    .pattern("R")
                    .pattern("M")
                    .unlockedBy("has_item", RegistrateRecipeProvider.has(AllItems.PRECISION_MECHANISM))
                    .save(p))
            .build()
            .register();

    public static final BlockEntry<DepositBlock> IRON_DEPOSIT_BLOCK = CreateRNS.REGISTRATE.block(
                    "iron_deposit_block", DepositBlock::new)
            .transform(deposit(MapColor.RAW_IRON)).register();
    public static final BlockEntry<DepositBlock> COPPER_DEPOSIT_BLOCK = CreateRNS.REGISTRATE.block(
                    "copper_deposit_block", DepositBlock::new)
            .transform(deposit(MapColor.COLOR_ORANGE)).register();
    public static final BlockEntry<DepositBlock> ZINC_DEPOSIT_BLOCK = CreateRNS.REGISTRATE.block(
                    "zinc_deposit_block", DepositBlock::new)
            .transform(deposit(MapColor.GLOW_LICHEN)).register();
    public static final BlockEntry<DepositBlock> GOLD_DEPOSIT_BLOCK = CreateRNS.REGISTRATE.block(
                    "gold_deposit_block", DepositBlock::new)
            .transform(deposit(MapColor.GOLD)).register();
    public static final BlockEntry<DepositBlock> REDSTONE_DEPOSIT_BLOCK = CreateRNS.REGISTRATE.block(
                    "redstone_deposit_block", DepositBlock::new)
            .transform(deposit(MapColor.FIRE)).register();

    // Block entities
    public static final BlockEntityEntry<MinerMk1BlockEntity> MINER_MK1_BE = CreateRNS.REGISTRATE.blockEntity("miner_mk1",
                    (BlockEntityType<MinerMk1BlockEntity> t, BlockPos p, BlockState s) ->
                            new MinerMk1BlockEntity(t, p, s))
            .visual(() -> MinerMk1Visual::new)
            .validBlock(MINER_MK1_BLOCK)
            .renderer(() -> MinerMk1Renderer::new)
            .register();

    public static final BlockEntityEntry<MinerMk2BlockEntity> MINER_MK2_BE = CreateRNS.REGISTRATE.blockEntity("miner_mk2",
                    (BlockEntityType<MinerMk2BlockEntity> t, BlockPos p, BlockState s) ->
                            new MinerMk2BlockEntity(t, p, s))
            .visual(() -> MinerMk2Visual::new)
            .validBlock(MINER_MK2_BLOCK)
            .renderer(() -> MinerMk2Renderer::new)
            .register();

    public static void register(IEventBus modBus) {
        ATTACHMENT_TYPES.register(modBus);
    }

    public static <T extends Block, P> NonNullFunction<BlockBuilder<T, P>, BlockBuilder<T, P>> deposit(MapColor mapColor) {
        return b -> b
                .initialProperties(() -> Blocks.RAW_IRON_BLOCK)
                .properties(p -> p
                        .mapColor(mapColor)
                        .strength(50.0F, 1200f)
                        .pushReaction(PushReaction.BLOCK)
                        .noLootTable())
                .transform(pickaxeOnly())
                .tag(BlockTags.MINEABLE_WITH_PICKAXE)
                .tag(BlockTags.NEEDS_DIAMOND_TOOL)
                .tag(RNSTags.Block.DEPOSIT_BLOCKS)
                .item()
                .tag(RNSTags.Item.DEPOSIT_BLOCKS)
                .build();
    }

    public static <T extends Block, P> NonNullFunction<BlockBuilder<T, P>, BlockBuilder<T, P>> minerBlockCommon() {
        return builder -> builder
                .initialProperties(SharedProperties::stone)
                .properties(p -> p
                        .noOcclusion()
                        .mapColor(MapColor.PODZOL)
                        .pushReaction(PushReaction.BLOCK))
                .transform(axeOrPickaxe())
                .blockstate((c, p) ->
                        p.simpleBlock(c.get(), AssetLookup.partialBaseModel(c, p)));
    }
}
