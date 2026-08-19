package com.bmaster.createrns.infrastructure.command;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.content.deposit.operating.DepositDetectionOutlineRenderer;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

import javax.annotation.ParametersAreNonnullByDefault;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public final class DebugCommand {
    private static final LiteralArgumentBuilder<CommandSourceStack> DEPOSIT_DETECTION_CMD =
            Commands.literal("deposit_detection")
                    .then(Commands.argument("enabled", BoolArgumentType.bool())
                            .executes(DebugCommand::setDepositDetection));

    public static final LiteralArgumentBuilder<CommandSourceStack> CMD = Commands.literal("debug")
            .then(TemplateCommands.PLACE_CMD)
            .then(TemplateCommands.REMOVE_CMD);

    public static final LiteralArgumentBuilder<CommandSourceStack> CLIENT_CMD = Commands.literal("debug")
            .then(DEPOSIT_DETECTION_CMD);

    private DebugCommand() {
    }

    private static int setDepositDetection(CommandContext<CommandSourceStack> context) {
        var enabled = BoolArgumentType.getBool(context, "enabled");
        var source = context.getSource();
        DepositDetectionOutlineRenderer.setEnabled(enabled);
        source.sendSuccess(() -> CreateRNS.translatable("command.debug.deposit_detection", enabled), false);
        return SINGLE_SUCCESS;
    }
}
