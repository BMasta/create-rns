package com.bmaster.createrns.infrastructure.command;

import com.bmaster.createrns.RNSContent;
import com.bmaster.createrns.data.gen.depositworldgen.DepositSetConfigBuilder;
import com.bmaster.createrns.deposit.DepositBlock;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceKeyArgument;
import net.minecraft.commands.arguments.ResourceOrTagKeyArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.List;
import java.util.Objects;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class ScannerCommand {
    private static final LiteralArgumentBuilder<CommandSourceStack> LOCATE = Commands.literal("locate")
            .then(Commands.literal("generated_undiscovered")
                    .then(Commands.argument("structure", ResourceOrTagKeyArgument.resourceOrTagKey(Registries.STRUCTURE))
                            .executes(execLocate(true, true))))
            .then(Commands.literal("generated")
                    .then(Commands.argument("structure", ResourceOrTagKeyArgument.resourceOrTagKey(Registries.STRUCTURE))
                            .executes(execLocate(true, false))))
            .then(Commands.literal("any_undiscovered")
                    .then(Commands.argument("structure", ResourceOrTagKeyArgument.resourceOrTagKey(Registries.STRUCTURE))
                            .executes(execLocate(false, true))))
            .then(Commands.literal("any")
                    .then(Commands.argument("structure", ResourceOrTagKeyArgument.resourceOrTagKey(Registries.STRUCTURE))
                            .executes(execLocate(false, false))));

    private static final LiteralArgumentBuilder<CommandSourceStack> ADD_TARGET = Commands.literal("add_target")
            .then(Commands.literal("block")
                    .then(Commands.argument("structure", ResourceKeyArgument.key(Registries.STRUCTURE))
                            .then(Commands.argument("target_position", BlockPosArgument.blockPos())
                                    .executes(ScannerCommand.execAddRmTarget(true, false)))))
            .then(Commands.literal("vein")
                    .then(Commands.argument("structure", ResourceKeyArgument.key(Registries.STRUCTURE))
                            .then(Commands.argument("target_position", BlockPosArgument.blockPos())
                                    .executes(ScannerCommand.execAddRmTarget(true, true)))));

    private static final LiteralArgumentBuilder<CommandSourceStack> REMOVE_TARGET = Commands.literal("remove_target")
            .then(Commands.argument("target_position", BlockPosArgument.blockPos())
                    .executes(ScannerCommand.execAddRmTarget(false, false)));

    private static final LiteralArgumentBuilder<CommandSourceStack> FOUND = Commands.literal("found")
            .then(Commands.argument("target_position", BlockPosArgument.blockPos())
                    .executes(execIsFound())
                    .then(Commands.argument("new_value", BoolArgumentType.bool())
                            .executes(execMarkFound())));

    public static final LiteralArgumentBuilder<CommandSourceStack> CMD = Commands.literal("scanner")
            .requires(css -> css.hasPermission(2))
            .then(LOCATE)
            .then(ADD_TARGET)
            .then(REMOVE_TARGET)
            .then(FOUND);

    private static Command<CommandSourceStack> execAddRmTarget(boolean isAdd, boolean isVein) {
        return ctx -> {
            var src = ctx.getSource();
            var sl = src.getLevel();
            var depData = sl.getData(RNSContent.LEVEL_DEPOSIT_DATA.get());
            if (isVein) {
                var start = BlockPosArgument.getLoadedBlockPos(ctx, "target_position");
                var vein = DepositBlock.getVein(sl, start);
                if (vein.isEmpty()) {
                    src.sendFailure(Component.literal("Vein does not exist"));
                    return 0;
                }
                var center = BoundingBox.encapsulatingPositions(vein.keySet()).orElseThrow().getCenter();
                if (isAdd) {
                    var key = ResourceKeyArgument.getStructure(ctx, "structure").key();
                    depData.addDeposit(key, center);
                    src.sendSuccess(() -> Component.literal("The %d-block vein of type %s with center at ".formatted(vein.size(), key.location()))
                            .append(bracketedCoords(center))
                            .append(" is now scannable"), true);
                } else throw new RuntimeException("Not Implemented");
            } else {
                var pos = BlockPosArgument.getLoadedBlockPos(ctx, "target_position");
                if (isAdd) {
                    var key = ResourceKeyArgument.getStructure(ctx, "structure").key();
                    depData.addDeposit(key, pos);
                    src.sendSuccess(() -> Component.literal("Target of type " + key.location() + " at ")
                            .append(bracketedCoords(pos))
                            .append(" is now scannable"), true);
                } else {
                    boolean isRemoved = depData.removeDeposit(pos);
                    if (!isRemoved) {
                        src.sendFailure(Component.literal("Target does not exist"));
                        return 0;
                    }
                    src.sendSuccess(() -> Component.literal("Target at ")
                            .append(bracketedCoords(pos))
                            .append(" is no longer scannable"), true);
                }
            }
            return SINGLE_SUCCESS;
        };
    }

    private static Command<CommandSourceStack> execIsFound() {
        return ctx -> {
            var src = ctx.getSource();
            var sl = src.getLevel();
            var depData = sl.getData(RNSContent.LEVEL_DEPOSIT_DATA.get());
            BlockPos pos = BlockPosArgument.getLoadedBlockPos(ctx, "target_position");
            if (depData.getType(pos) == null) {
                src.sendFailure(Component.literal("Target does not exist"));
                return 0;
            }
            src.sendSuccess(() -> Component.literal(depData.isFound(pos) ? "True" : "False"), false);
            return SINGLE_SUCCESS;
        };
    }

    private static Command<CommandSourceStack> execMarkFound() {
        return ctx -> {
            var src = ctx.getSource();
            var sl = src.getLevel();
            var depData = sl.getData(RNSContent.LEVEL_DEPOSIT_DATA.get());

            BlockPos pos = BlockPosArgument.getLoadedBlockPos(ctx, "target_position");
            boolean val = ctx.getArgument("new_value", Boolean.class);
            var type = depData.getType(pos);
            if (type == null) {
                src.sendFailure(Component.literal("Target does not exist"));
                return 0;
            }
            var isSet = depData.setFound(type, pos, val);
            if (!isSet) {
                src.sendFailure(Component.literal("Failed to mark target at ")
                        .append(bracketedCoords(pos))
                        .append(" as %s".formatted(val ? "found" : "not found")));
                return 0;
            }

            src.sendSuccess(() -> Component.literal("Marked target at ")
                    .append(bracketedCoords(pos))
                    .append(" as %s".formatted(val ? "found" : "not found")), true);
            return SINGLE_SUCCESS;
        };
    }

    private static Command<CommandSourceStack> execLocate(boolean generated, boolean undiscovered) {
        return ctx -> {
            var src = ctx.getSource();
            var sl = src.getLevel();
            var entity = src.getEntity();
            if (entity == null) {
                src.sendFailure(Component.literal("Command must be called by an entity"));
                return 0;
            }
            var pos = entity.blockPosition();
            var keys = parseResourceOrTag(ctx, "structure", Registries.STRUCTURE);
            var depData = sl.getData(RNSContent.LEVEL_DEPOSIT_DATA.get());
            for (var k : keys) {
                var nearest = depData.getNearest(k, sl, pos, DepositSetConfigBuilder.DEFAULT_SPACING * 4,
                        !undiscovered, generated);
                if (nearest == null) {
                    src.sendFailure(Component.literal("Could not find target of type " + k.location()));
                } else {
                    src.sendSuccess(() -> Component.literal("Found " + k.location() + " at ")
                            .append(bracketedCoords(nearest)), false);
                }
            }
            return SINGLE_SUCCESS;
        };
    }

    private static <T> List<ResourceKey<T>> parseResourceOrTag(
            CommandContext<CommandSourceStack> ctx, String arg, ResourceKey<Registry<T>> reg) throws CommandSyntaxException {
        var access = ctx.getSource().getLevel().registryAccess();
        var ex = new DynamicCommandExceptionType(id -> Component.literal("Unknown structure or tag: " + id));
        var resOrTag = ResourceOrTagKeyArgument.getResourceOrTagKey(ctx, arg, reg, ex).unwrap();

        if (resOrTag.left().isPresent()) return List.of(resOrTag.left().get());

        @SuppressWarnings("OptionalGetWithoutIsPresent")
        var set = access.lookupOrThrow(reg).get(resOrTag.right().get()).orElse(null);
        if (set == null) return List.of();
        return set.stream()
                .map(h -> h.unwrapKey().orElse(null))
                .filter(Objects::nonNull)
                .toList();
    }

    private static Component bracketedCoords(BlockPos pos) {
        String copied = "%d %d %d".formatted(pos.getX(), pos.getY(), pos.getZ());
        MutableComponent raw = Component.literal("[%d, %d, %d]".formatted(pos.getX(), pos.getY(), pos.getZ()));
        return raw.setStyle(Style.EMPTY
                .withColor(ChatFormatting.GREEN)
                .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, copied))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        Component.literal("Click to copy"))));
    }
}
