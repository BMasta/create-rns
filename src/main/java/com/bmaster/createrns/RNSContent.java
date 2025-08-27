package com.bmaster.createrns;

import com.bmaster.createrns.block.DepositBlock;
import com.bmaster.createrns.block.miner.*;
import com.bmaster.createrns.capability.depositindex.IDepositIndex;
import com.bmaster.createrns.item.DepositScanner.DepositScannerItem;
import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.foundation.data.AssetLookup;
import com.simibubi.create.foundation.data.SharedProperties;
import com.simibubi.create.foundation.item.ItemDescription;
import com.simibubi.create.foundation.item.KineticStats;
import com.simibubi.create.foundation.item.TooltipModifier;
import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.util.entry.*;
import com.tterrag.registrate.util.nullness.NonNullFunction;
import net.createmod.catnip.lang.FontHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

import static com.simibubi.create.foundation.data.TagGen.*;

public class RNSContent {
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
            .register();

    // Blocks
    public static final BlockEntry<MinerBlock> MINER_BLOCK = CreateRNS.REGISTRATE.block("miner",
                    MinerBlock::new)
            .initialProperties(SharedProperties::stone)
            .properties(p -> p
                    .noOcclusion()
                    .mapColor(MapColor.PODZOL)
                    .pushReaction(PushReaction.BLOCK))
            .transform(axeOrPickaxe())
            .blockstate((c, p) ->
                    p.simpleBlock(c.get(), AssetLookup.standardModel(c, p)))
            .onRegister((b) -> BlockStressValues.IMPACTS.register(b, () -> 4))
            .item()
            .model(AssetLookup.existingItemModel())
            .build()
            .register();

    public static final BlockEntry<DepositBlock> IRON_DEPOSIT_BLOCK = CreateRNS.REGISTRATE.block(
                    "iron_deposit_block", DepositBlock::new)
            .transform(deposit(MapColor.RAW_IRON)).item().build().register();

    public static final BlockEntry<DepositBlock> COPPER_DEPOSIT_BLOCK = CreateRNS.REGISTRATE.block(
                    "copper_deposit_block", DepositBlock::new)
            .transform(deposit(MapColor.COLOR_ORANGE)).item().build().register();

    public static final BlockEntry<DepositBlock> GOLD_DEPOSIT_BLOCK = CreateRNS.REGISTRATE.block(
                    "gold_deposit_block", DepositBlock::new)
            .transform(deposit(MapColor.GOLD)).item().build().register();

    public static final BlockEntry<DepositBlock> REDSTONE_DEPOSIT_BLOCK = CreateRNS.REGISTRATE.block(
                    "redstone_deposit_block", DepositBlock::new)
            .transform(deposit(MapColor.FIRE)).item().build().register();

    // Block entities
    public static final BlockEntityEntry<MinerBlockEntity> MINER_BE = CreateRNS.REGISTRATE.blockEntity("miner",
                    (BlockEntityType<MinerBlockEntity> t, BlockPos p, BlockState s) ->
                            new MinerBlockEntity(t, p, s))
            .visual(() -> MinerVisual::new)
            .validBlock(MINER_BLOCK)
            .renderer(() -> MinerRenderer::new)
            .register();

    // Creative tabs
    public static final RegistryEntry<CreativeModeTab> MAIN_TAB = CreateRNS.REGISTRATE.defaultCreativeTab(
                    CreateRNS.MOD_ID, c -> c
                            .icon(() -> new ItemStack(MINER_BLOCK.getDefaultState().getBlock()))
                            .title(Component.translatable("creativetab.%s".formatted(CreateRNS.MOD_ID)))
                            .displayItems((pParameters, pOutput) -> {
                                pOutput.accept(MINER_BLOCK.get().asItem());
                                pOutput.accept(DEPOSIT_SCANNER_ITEM.get().asItem());
                                pOutput.accept(IRON_DEPOSIT_BLOCK.get().asItem());
                                pOutput.accept(COPPER_DEPOSIT_BLOCK.get().asItem());
                                pOutput.accept(GOLD_DEPOSIT_BLOCK.get().asItem());
                                pOutput.accept(REDSTONE_DEPOSIT_BLOCK.get().asItem());
                            })
                            .build())
            .register();

    // Menus
    public static final MenuEntry<MinerMenu> MINER_MENU =
            CreateRNS.REGISTRATE.menu("miner",
                    MinerMenu::new,
                    () -> MinerScreen::new
            ).register();

    // Capabilities
    public static final Capability<IDepositIndex> DEPOSIT_INDEX =
            CapabilityManager.get(new CapabilityToken<>() {});

    public static void register() {
    }

    public static <T extends Block, P> NonNullFunction<BlockBuilder<T, P>, BlockBuilder<T, P>> deposit(
            MapColor mapColor
    ) {
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
                .getParent();
    }
}
