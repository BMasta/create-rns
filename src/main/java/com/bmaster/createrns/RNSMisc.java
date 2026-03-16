package com.bmaster.createrns;

import com.bmaster.createrns.content.deposit.info.IDepositIndex;
import com.bmaster.createrns.infrastructure.command.DepositCommand;
import com.bmaster.createrns.infrastructure.command.ScannerCommand;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.simibubi.create.foundation.item.ItemDescription;
import com.simibubi.create.foundation.item.KineticStats;
import com.simibubi.create.foundation.item.TooltipModifier;
import com.tterrag.registrate.util.entry.RegistryEntry;
import net.createmod.catnip.lang.FontHelper;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

public class RNSMisc {
    // Item tooltips
    static {
        CreateRNS.REGISTRATE.setTooltipModifierFactory(item ->
                new ItemDescription.Modifier(item, FontHelper.Palette.STANDARD_CREATE)
                        .andThen(TooltipModifier.mapNull(KineticStats.create(item))));
    }

    // Capabilities
    public static final Capability<IDepositIndex> DEPOSIT_INDEX =
            CapabilityManager.get(new CapabilityToken<>() {});

    // Creative tab
    public static final RegistryEntry<CreativeModeTab> MAIN_TAB = CreateRNS.REGISTRATE.defaultCreativeTab(
                    CreateRNS.MOD_ID, c -> c
                            .icon(() -> new ItemStack(RNSBlocks.MINER_MK2_BLOCK.getDefaultState().getBlock()))
                            .title(Component.translatable("creativetab.%s".formatted(CreateRNS.MOD_ID)))
                            .displayItems((pParameters, pOutput) -> {
                                pOutput.accept(RNSBlocks.MINER_MK1_BLOCK.get().asItem());
                                pOutput.accept(RNSBlocks.MINER_MK2_BLOCK.get().asItem());
                                pOutput.accept(RNSBlocks.MINER_BEARING_BLOCK.get().asItem());
                                pOutput.accept(RNSBlocks.DRILL_HEAD_BLOCK.get().asItem());
                                pOutput.accept(RNSBlocks.RESONATOR_BLOCK.get().asItem());
                                pOutput.accept(RNSItems.DEPOSIT_SCANNER_ITEM.get());
                                pOutput.accept(RNSBlocks.IRON_DEPOSIT_BLOCK.get().asItem());
                                pOutput.accept(RNSBlocks.COPPER_DEPOSIT_BLOCK.get().asItem());
                                pOutput.accept(RNSBlocks.ZINC_DEPOSIT_BLOCK.get().asItem());
                                pOutput.accept(RNSBlocks.GOLD_DEPOSIT_BLOCK.get().asItem());
                                pOutput.accept(RNSBlocks.REDSTONE_DEPOSIT_BLOCK.get().asItem());
                                pOutput.accept(RNSBlocks.DEPLETED_DEPOSIT_BLOCK.get().asItem());
                                pOutput.accept(RNSItems.IMPURE_IRON_ORE.get());
                                pOutput.accept(RNSItems.IMPURE_COPPER_ORE.get());
                                pOutput.accept(RNSItems.IMPURE_ZINC_ORE.get());
                                pOutput.accept(RNSItems.IMPURE_GOLD_ORE.get());
                                pOutput.accept(RNSItems.IMPURE_REDSTONE_DUST.get());
                                pOutput.accept(RNSItems.REDSTONE_SMALL_DUST.get());
                            })
                            .build())
            .register();

    // Commands
    public static final LiteralArgumentBuilder<CommandSourceStack> RNS_COMMAND = Commands.literal("rns")
            .then(DepositCommand.CMD)
            .then(ScannerCommand.CMD);

    public static void register() {
    }
}
