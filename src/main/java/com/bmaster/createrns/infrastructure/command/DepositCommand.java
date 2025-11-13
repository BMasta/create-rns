package com.bmaster.createrns.infrastructure.command;

import com.bmaster.createrns.RNSContent;
import com.bmaster.createrns.deposit.DepositBlock;
import com.bmaster.createrns.infrastructure.ServerConfig;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceKeyArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.Objects;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class DepositCommand {
    private enum ResourceAction {SET, SET_VEIN, UNSET, UNSET_VEIN, COMPUTE_VEIN}

    static final LiteralArgumentBuilder<CommandSourceStack> RESOURCES = Commands.literal("resources")
            .requires(css -> css.hasPermission(2))
            .then(Commands.literal("compute_vein")
                    .then(Commands.argument("dep_block_pos", BlockPosArgument.blockPos())
                            .executes(DepositCommand.execResources(ResourceAction.COMPUTE_VEIN, false))))
            .then(Commands.literal("set")
                    .then(Commands.literal("block")
                            .then(Commands.argument("dep_block_pos", BlockPosArgument.blockPos())
                                    .then(Commands.argument("resource_amount", LongArgumentType.longArg(1, Long.MAX_VALUE))
                                            .executes(DepositCommand.execResources(ResourceAction.SET, false)))
                                    .then(Commands.literal("infinite")
                                            .executes(DepositCommand.execResources(ResourceAction.SET, true)))))
                    .then(Commands.literal("vein")
                            .then(Commands.argument("dep_block_pos", BlockPosArgument.blockPos())
                                    .then(Commands.argument("resource_amount", LongArgumentType.longArg(1, Long.MAX_VALUE))
                                            .executes(DepositCommand.execResources(ResourceAction.SET_VEIN, false)))
                                    .then(Commands.literal("infinite")
                                            .executes(DepositCommand.execResources(ResourceAction.SET_VEIN, true))))))
            .then(Commands.literal("unset")
                    .then(Commands.literal("block")
                            .then(Commands.argument("dep_block_pos", BlockPosArgument.blockPos())
                                    .executes(DepositCommand.execResources(ResourceAction.UNSET, false))))
                    .then(Commands.literal("vein")
                            .then(Commands.argument("dep_block_pos", BlockPosArgument.blockPos())
                                    .executes(DepositCommand.execResources(ResourceAction.UNSET_VEIN, false)))));

    public static final LiteralArgumentBuilder<CommandSourceStack> CMD = Commands.literal("deposit")
            .then(RESOURCES);

    private static Command<CommandSourceStack> execResources(ResourceAction action, boolean infinite) {
        return ctx -> {
            var src = ctx.getSource();
            if (ServerConfig.infiniteDeposits) {
                src.sendFailure(Component.literal("Command requires finite deposits (enabled in server config)!"));
                return 0;
            }
            var sl = src.getLevel();
            var depData = sl.getData(RNSContent.LEVEL_DEPOSIT_DATA.get());
            var pos = BlockPosArgument.getLoadedBlockPos(ctx, "dep_block_pos");
            switch (action) {
                case SET -> {
                    long amount;
                    if (infinite) amount = 0;
                    else amount = ctx.getArgument("resource_amount", Long.class);
                    boolean isSet = depData.setDepositBlockDurability(pos, amount);
                    if (isSet) {
                        src.sendSuccess(() -> Component.literal("Success!"), false);
                    } else {
                        src.sendFailure(Component.literal("Failed!"));
                    }
                }
                case SET_VEIN -> {
                    long amount;
                    if (infinite) amount = 0;
                    else amount = ctx.getArgument("resource_amount", Long.class);
                    var vein = DepositBlock.getVein(sl, pos);
                    boolean isSet = true;
                    for (var bp : vein.keySet()) {
                        if (!depData.setDepositBlockDurability(bp, amount)) isSet = false;
                    }
                    if (isSet) {
                        src.sendSuccess(() -> Component.literal("Success!"), false);
                    } else {
                        src.sendFailure(Component.literal("Failed!"));
                    }
                }
                case UNSET -> {
                    depData.removeDepositBlockDurability(pos);
                    src.sendSuccess(() -> Component.literal("Success!"), false);
                }
                case UNSET_VEIN -> {
                    var vein = DepositBlock.getVein(sl, pos);
                    for (var bp : vein.keySet()) {
                        depData.removeDepositBlockDurability(bp);
                    }
                    src.sendSuccess(() -> Component.literal("Success!"), false);
                }
                case COMPUTE_VEIN -> {
                    int initCount = depData.initDepositVeinDurability(pos);
                    src.sendSuccess(() -> Component.literal("Initialized " + initCount + " blocks!"), true);
                }
            }
            return SINGLE_SUCCESS;
        };
    }
}
