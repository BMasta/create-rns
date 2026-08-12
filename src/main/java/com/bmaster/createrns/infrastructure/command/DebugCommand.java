package com.bmaster.createrns.infrastructure.command;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.content.deposit.operating.DepositDetectionOutlineRenderer;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

import javax.annotation.ParametersAreNonnullByDefault;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public final class DebugCommand {
    private DebugCommand() {}

    public static void register(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("rns")
                .then(Commands.literal("debug")
                        .then(Commands.literal("deposit_detection")
                                .then(Commands.argument("enabled", BoolArgumentType.bool())
                                        .executes(DebugCommand::setDepositDetection)))));
    }

    private static int setDepositDetection(CommandContext<CommandSourceStack> context) {
        var enabled = BoolArgumentType.getBool(context, "enabled");
        var source = context.getSource();
        DepositDetectionOutlineRenderer.setEnabled(enabled);
        source.sendSuccess(() -> CreateRNS.translatable("command.debug.deposit_detection", enabled), false);
        return SINGLE_SUCCESS;
    }
}
