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
import net.minecraft.world.item.CreativeModeTab;
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
                    CreateRNS.ID, c -> c
                            .icon(RNSItems.DEPOSIT_SCANNER_ITEM::asStack)
                            .title(CreateRNS.translatable("creativetab"))
                            .build())
            .register();

    // Commands
    public static final LiteralArgumentBuilder<CommandSourceStack> RNS_COMMAND = Commands.literal("rns")
            .then(DepositCommand.CMD)
            .then(ScannerCommand.CMD);

    public static void register() {
    }
}
