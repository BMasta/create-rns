package com.bmaster.createrns.infrastructure.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public final class DebugCommand {
    public static final LiteralArgumentBuilder<CommandSourceStack> CMD = Commands.literal("debug")
            .then(TemplateCommands.PLACE_CMD)
            .then(TemplateCommands.REMOVE_CMD);

    private DebugCommand() {
    }
}
