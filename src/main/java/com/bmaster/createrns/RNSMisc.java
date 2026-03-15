package com.bmaster.createrns;

import com.bmaster.createrns.content.deposit.info.LevelDepositData;
import com.bmaster.createrns.infrastructure.command.DepositCommand;
import com.bmaster.createrns.infrastructure.command.ScannerCommand;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.simibubi.create.foundation.item.ItemDescription;
import com.simibubi.create.foundation.item.KineticStats;
import com.simibubi.create.foundation.item.TooltipModifier;
import com.tterrag.registrate.util.entry.RegistryEntry;
import net.createmod.catnip.lang.FontHelper;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Supplier;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class RNSMisc {
    // Item tooltips
    static {
        CreateRNS.REGISTRATE.setTooltipModifierFactory(item ->
                new ItemDescription.Modifier(item, FontHelper.Palette.STANDARD_CREATE)
                        .andThen(TooltipModifier.mapNull(KineticStats.create(item))));
    }

    // Level attachments
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, CreateRNS.ID);

    public static final Supplier<AttachmentType<LevelDepositData>> LEVEL_DEPOSIT_DATA = ATTACHMENT_TYPES.register(
            "level_deposit_data", () -> AttachmentType.serializable(holder -> {
                if (!(holder instanceof ServerLevel level)) {
                    throw new IllegalStateException("Level deposit data holder is not a server level: " + holder);
                }
                return new LevelDepositData(level);
            }).build());

    // Creative tab
    public static final RegistryEntry<CreativeModeTab, CreativeModeTab> MAIN_TAB = CreateRNS.REGISTRATE.defaultCreativeTab(
                    CreateRNS.ID, c -> c
                            .icon(RNSItems.DEPOSIT_SCANNER_ITEM::asStack)
                            .title(CreateRNS.translatable("creativetab"))
                            .build())
            .register();

    // Commands
    public static final LiteralArgumentBuilder<CommandSourceStack> RNS_COMMAND = Commands.literal("rns")
            .then(DepositCommand.CMD)
            .then(ScannerCommand.CMD);

    public static void register(IEventBus modBus) {
        ATTACHMENT_TYPES.register(modBus);
    }

}
