package com.bmaster.createrns;

import com.bmaster.createrns.compat.ponder.RNSPonderPlugin;
import com.bmaster.createrns.data.gen.depositworldgen.DepositSetConfigBuilder;
import com.bmaster.createrns.data.gen.depositworldgen.DepositStructureConfigBuilder;
import com.bmaster.createrns.data.pack.DynamicDatapack;
import com.bmaster.createrns.data.pack.DynamicDatapackContent;
import com.bmaster.createrns.deposit.DepositBlock;
import com.bmaster.createrns.mining.miner.*;
import com.bmaster.createrns.deposit.capability.IDepositIndex;
import com.bmaster.createrns.item.DepositScanner.DepositScannerItem;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.foundation.data.AssetLookup;
import com.simibubi.create.foundation.data.SharedProperties;
import com.simibubi.create.foundation.item.ItemDescription;
import com.simibubi.create.foundation.item.KineticStats;
import com.simibubi.create.foundation.item.TooltipModifier;
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
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.registries.ForgeRegistries;

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
    public static final BlockEntry<MinerBlock> MINER_MK1_BLOCK = CreateRNS.REGISTRATE.block("miner_mk1",
                    MinerBlock::new)
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

    public static final BlockEntry<MinerBlock> MINER_MK2_BLOCK = CreateRNS.REGISTRATE.block("miner_mk2",
                    MinerBlock::new)
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
            .transform(deposit(MapColor.RAW_IRON))
            .onRegister(d -> DepositStructureConfigBuilder
                    .create("iron")
                    .depositBlock(ForgeRegistries.BLOCKS.getKey(d))
                    .depth(8)
                    .weight(10)
                    .nbt(RNSContent.DEP_MEDIUM, 70)
                    .nbt(RNSContent.DEP_LARGE, 30)
                    .save())
            .register();

    public static final BlockEntry<DepositBlock> COPPER_DEPOSIT_BLOCK = CreateRNS.REGISTRATE.block(
                    "copper_deposit_block", DepositBlock::new)
            .transform(deposit(MapColor.COLOR_ORANGE))
            .onRegister(d -> DepositStructureConfigBuilder
                    .create("copper")
                    .depositBlock(ForgeRegistries.BLOCKS.getKey(d))
                    .depth(8)
                    .weight(5)
                    .nbt(RNSContent.DEP_MEDIUM, 70)
                    .nbt(RNSContent.DEP_LARGE, 30)
                    .save())
            .register();

    public static final BlockEntry<DepositBlock> ZINC_DEPOSIT_BLOCK = CreateRNS.REGISTRATE.block(
                    "zinc_deposit_block", DepositBlock::new)
            .transform(deposit(MapColor.GLOW_LICHEN))
            .onRegister(d -> DepositStructureConfigBuilder
                    .create("zinc")
                    .depositBlock(ForgeRegistries.BLOCKS.getKey(d))
                    .depth(8)
                    .weight(2)
                    .nbt(RNSContent.DEP_SMALL, 70)
                    .nbt(RNSContent.DEP_MEDIUM, 28)
                    .nbt(RNSContent.DEP_LARGE, 2)
                    .save())
            .register();

    public static final BlockEntry<DepositBlock> GOLD_DEPOSIT_BLOCK = CreateRNS.REGISTRATE.block(
                    "gold_deposit_block", DepositBlock::new)
            .transform(deposit(MapColor.GOLD))
            .onRegister(d -> DepositStructureConfigBuilder
                    .create("gold")
                    .depositBlock(ForgeRegistries.BLOCKS.getKey(d))
                    .depth(12)
                    .weight(2)
                    .nbt(RNSContent.DEP_SMALL, 70)
                    .nbt(RNSContent.DEP_MEDIUM, 28)
                    .nbt(RNSContent.DEP_LARGE, 2)
                    .save())
            .register();

    public static final BlockEntry<DepositBlock> REDSTONE_DEPOSIT_BLOCK = CreateRNS.REGISTRATE.block(
                    "redstone_deposit_block", DepositBlock::new)
            .transform(deposit(MapColor.FIRE))
            .onRegister(d -> DepositStructureConfigBuilder
                    .create("redstone")
                    .depositBlock(ForgeRegistries.BLOCKS.getKey(d))
                    .depth(12)
                    .weight(2)
                    .nbt(RNSContent.DEP_SMALL, 70)
                    .nbt(RNSContent.DEP_MEDIUM, 28)
                    .nbt(RNSContent.DEP_LARGE, 2)
                    .save())
            .register();

    public static final BlockEntry<DepositBlock> DEPLETED_DEPOSIT_BLOCK = CreateRNS.REGISTRATE.block(
                    "depleted_deposit_block", DepositBlock::new)
            .transform(deposit(MapColor.COLOR_BLACK))
            .register();

    static {
        // Must run after all deposit configs are saved
        DepositSetConfigBuilder
                .create()
                .save();
    }

    // Block entities
    public static final BlockEntityEntry<MinerBlockEntity> MINER_BE = CreateRNS.REGISTRATE.blockEntity("miner",
                    (BlockEntityType<MinerBlockEntity> t, BlockPos p, BlockState s) ->
                            new MinerBlockEntity(p, s))
            .visual(() -> MinerVisual::new)
            .validBlock(MINER_MK1_BLOCK)
            .validBlock(MINER_MK2_BLOCK)
            .renderer(() -> MinerRenderer::new)
            .register();

    // Creative tabs
    public static final RegistryEntry<CreativeModeTab> MAIN_TAB = CreateRNS.REGISTRATE.defaultCreativeTab(
                    CreateRNS.MOD_ID, c -> c
                            .icon(() -> new ItemStack(MINER_MK2_BLOCK.getDefaultState().getBlock()))
                            .title(Component.translatable("creativetab.%s".formatted(CreateRNS.MOD_ID)))
                            .displayItems((pParameters, pOutput) -> {
                                pOutput.accept(MINER_MK1_BLOCK.get().asItem());
                                pOutput.accept(MINER_MK2_BLOCK.get().asItem());
                                pOutput.accept(DEPOSIT_SCANNER_ITEM.get());
                                pOutput.accept(RESONANT_MECHANISM.get());
                                pOutput.accept(IRON_DEPOSIT_BLOCK.get().asItem());
                                pOutput.accept(COPPER_DEPOSIT_BLOCK.get().asItem());
                                pOutput.accept(ZINC_DEPOSIT_BLOCK.get().asItem());
                                pOutput.accept(GOLD_DEPOSIT_BLOCK.get().asItem());
                                pOutput.accept(REDSTONE_DEPOSIT_BLOCK.get().asItem());
                                pOutput.accept(IMPURE_IRON_ORE.get());
                                pOutput.accept(IMPURE_COPPER_ORE.get());
                                pOutput.accept(IMPURE_ZINC_ORE.get());
                                pOutput.accept(IMPURE_GOLD_ORE.get());
                                pOutput.accept(IMPURE_REDSTONE_DUST.get());
                                pOutput.accept(REDSTONE_SMALL_DUST.get());
                            })
                            .build())
            .register();

    // Capabilities
    public static final Capability<IDepositIndex> DEPOSIT_INDEX =
            CapabilityManager.get(new CapabilityToken<>() {});

    // Dynamic packs
    public static Pack MAIN_PACK = DynamicDatapack.createDatapack("dynamic_data")
            .title(Component.literal("Dynamic mod data for Create: Rock & Stone"))
            .addContent(DynamicDatapackContent.standardDepositBiomeTag())
            .buildAndRegister();

    public static Pack NO_DEPOSIT_PACK = DynamicDatapack.createDatapack("no_deposit_worldgen")
            .title(Component.literal("Disable deposit generation"))
            .source(PackSource.FEATURE)
            .optional()
            .overwritesLoadedPacks()
            .addContent(DynamicDatapackContent.emptyDepositBiomeTag())
            .buildAndRegister();

    private static final ResourceLocation DEP_SMALL =
            ResourceLocation.fromNamespaceAndPath(CreateRNS.MOD_ID, "ore_deposit_small");
    private static final ResourceLocation DEP_MEDIUM =
            ResourceLocation.fromNamespaceAndPath(CreateRNS.MOD_ID, "ore_deposit_medium");
    private static final ResourceLocation DEP_LARGE =
            ResourceLocation.fromNamespaceAndPath(CreateRNS.MOD_ID, "ore_deposit_large");

    public static void register() {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> RNSPonderPlugin::register);
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
