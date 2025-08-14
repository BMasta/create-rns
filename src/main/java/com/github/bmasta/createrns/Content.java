package com.github.bmasta.createrns;

import com.github.bmasta.createrns.block.excavator.*;
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


public class Content {

    // Blocks
    public static final BlockEntry<ExcavatorBlock> EXCAVATOR_BLOCK = CreateRNS.REGISTRATE.block("excavator",
                    ExcavatorBlock::new)
            .initialProperties(SharedProperties::stone)
            .properties(BlockBehaviour.Properties::noOcclusion)
            .onRegister((b) ->
                    BlockStressValues.IMPACTS.register(b, () -> 32.0))
            .item().build()
            .register();

    // Block entities
    public static final BlockEntityEntry<ExcavatorBlockEntity> EXCAVATOR_BE = CreateRNS.REGISTRATE.blockEntity("excavator",
                    (BlockEntityType<ExcavatorBlockEntity> t, BlockPos p, BlockState s) ->
                            new ExcavatorBlockEntity(t, p, s))
            .visual(() -> ExcavatorVisual::new)
            .validBlock(EXCAVATOR_BLOCK)
            .renderer(() -> ExcavatorRenderer::new)
            .register();

    // Creative tabs
    public static final RegistryEntry<CreativeModeTab> MAIN_TAB = CreateRNS.REGISTRATE.defaultCreativeTab(
                    CreateRNS.MOD_ID, c -> c
                            .icon(() -> new ItemStack(EXCAVATOR_BLOCK.getDefaultState().getBlock()))
                            .title(Component.translatable("creativetab.%s".formatted(CreateRNS.MOD_ID)))
                            .displayItems((pParameters, pOutput) -> {
                                pOutput.accept(EXCAVATOR_BLOCK.get().asItem());
                            })
                            .build())
            .register();

    // Menus
    public static final MenuEntry<ExcavatorMenu> EXCAVATOR_MENU =
            CreateRNS.REGISTRATE.menu("excavator",
                    ExcavatorMenu::new,
                    () -> ExcavatorScreen::new
            ).register();

    public static void register() {
    }
}
