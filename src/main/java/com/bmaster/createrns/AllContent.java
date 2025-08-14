package com.bmaster.createrns;

import com.bmaster.createrns.block.miner.*;
import com.bmaster.createrns.capability.orechunkdata.IOreChunkData;
import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.foundation.data.SharedProperties;
import com.tterrag.registrate.util.entry.BlockEntityEntry;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.entry.MenuEntry;
import com.tterrag.registrate.util.entry.RegistryEntry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;


public class AllContent {

    // Blocks
    public static final BlockEntry<MinerBlock> MINER_BLOCK = CreateRNS.REGISTRATE.block("miner",
                    MinerBlock::new)
            .initialProperties(SharedProperties::stone)
            .properties(BlockBehaviour.Properties::noOcclusion)
            .onRegister((b) ->
                    BlockStressValues.IMPACTS.register(b, () -> 32.0))
            .item().build()
            .register();

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
    public static final Capability<IOreChunkData> ORE_CHUNK_DATA =
            CapabilityManager.get(new CapabilityToken<IOreChunkData>() {});

    public static void register() {
    }
}
