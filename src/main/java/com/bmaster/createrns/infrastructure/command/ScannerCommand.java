package com.bmaster.createrns.infrastructure.command;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.RNSTags.RNSStructureTags;
import com.bmaster.createrns.content.deposit.DepositBlock;
import com.bmaster.createrns.content.deposit.info.IDepositIndex;
import com.bmaster.createrns.content.deposit.info.CustomServerDepositLocation;
import com.bmaster.createrns.content.deposit.info.ServerDepositLocation;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Function3;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceKeyArgument;
import net.minecraft.commands.arguments.ResourceOrTagKeyArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.RegistryLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class ScannerCommand {
    public static final SuggestionProvider<CommandSourceStack> SUGGEST_DEPOSIT_STRUCTURES =
            (ctx, b) -> SharedSuggestionProvider.suggestResource(
                    ctx.getSource().getLevel().registryAccess().lookupOrThrow(Registries.STRUCTURE).listElements()
                            .filter(h -> h.is(RNSStructureTags.DEPOSITS))
                            .map(h -> h.key().location()),
                    b
            );

    public static final SuggestionProvider<CommandSourceStack> SUGGEST_DEPOSIT_STRUCTURES_OR_TAGS =
            (ctx, b) -> {
                var lookup = ctx.getSource().getLevel().registryAccess().lookupOrThrow(Registries.STRUCTURE);
                var structures = lookup.listElements()
                        .filter(h -> h.is(RNSStructureTags.DEPOSITS))
                        .map(h -> h.key().location())
                        .toList();
                var tags = lookup.listTags()
                        .filter(named -> named.stream().allMatch(h ->
                                h.is(RNSStructureTags.DEPOSITS)))
                        .map(named -> named.key().location())
                        .toList();
                var remaining = b.getRemaining().toLowerCase(Locale.ROOT);
                var structureSuggestions = new LinkedHashSet<String>();
                var tagSuggestions = new LinkedHashSet<String>();

                SharedSuggestionProvider.filterResources(
                        structures,
                        remaining,
                        rl -> rl,
                        rl -> structureSuggestions.add(rl.toString())
                );

                if (remaining.startsWith("#")) {
                    SharedSuggestionProvider.filterResources(
                            tags,
                            remaining,
                            "#",
                            rl -> rl,
                            rl -> tagSuggestions.add("#" + rl)
                    );
                } else {
                    SharedSuggestionProvider.filterResources(
                            tags,
                            remaining,
                            rl -> rl,
                            rl -> tagSuggestions.add("#" + rl)
                    );
                }

                var range = StringRange.between(b.getStart(), b.getInput().length());
                var ordered = new ArrayList<Suggestion>(structureSuggestions.size() + tagSuggestions.size());
                for (var s : structureSuggestions) ordered.add(new Suggestion(range, s));
                for (var s : tagSuggestions) ordered.add(new Suggestion(range, s));
                return CompletableFuture.completedFuture(new Suggestions(range, ordered));
            };

    private static final LiteralArgumentBuilder<CommandSourceStack> LOCATE = Commands.literal("locate")
            .then(Commands.literal("any")
                    .then(Commands.argument("structure", ResourceOrTagKeyArgument.resourceOrTagKey(Registries.STRUCTURE))
                            .suggests(SUGGEST_DEPOSIT_STRUCTURES_OR_TAGS)
                            .executes(execLocate(false))))
            .then(Commands.literal("undiscovered")
                    .then(Commands.argument("structure", ResourceOrTagKeyArgument.resourceOrTagKey(Registries.STRUCTURE))
                            .suggests(SUGGEST_DEPOSIT_STRUCTURES_OR_TAGS)
                            .executes(execLocate(true))))
            .then(Commands.literal("all")
                    .executes(execLocateAll(false))
                    .then(Commands.argument("source_position", BlockPosArgument.blockPos())
                            .executes(execLocateAll(true))));

    private static final LiteralArgumentBuilder<CommandSourceStack> ADD_TARGET = Commands.literal("add_target")
            .then(Commands.literal("block")
                    .then(Commands.argument("structure", ResourceKeyArgument.key(Registries.STRUCTURE))
                            .suggests(SUGGEST_DEPOSIT_STRUCTURES)
                            .then(Commands.argument("target_position", BlockPosArgument.blockPos())
                                    .executes(ScannerCommand.execAddRmTarget(true, false)))))
            .then(Commands.literal("vein")
                    .then(Commands.argument("structure", ResourceKeyArgument.key(Registries.STRUCTURE))
                            .suggests(SUGGEST_DEPOSIT_STRUCTURES)
                            .then(Commands.argument("target_position", BlockPosArgument.blockPos())
                                    .executes(ScannerCommand.execAddRmTarget(true, true)))));

    private static final LiteralArgumentBuilder<CommandSourceStack> REMOVE_TARGET = Commands.literal("remove_target")
            .then(Commands.argument("target_position", BlockPosArgument.blockPos())
                    .executes(ScannerCommand.execAddRmTarget(false, false)));

    private static final LiteralArgumentBuilder<CommandSourceStack> FOUND = Commands.literal("found")
            .then(Commands.argument("target_position", BlockPosArgument.blockPos())
                    .executes(execIsFound())
                    .then(Commands.argument("new_value", BoolArgumentType.bool())
                            .executes(execMarkFound(false))))
            .then(Commands.literal("forget_all")
                    .executes(execMarkFound(true)));

    public static final LiteralArgumentBuilder<CommandSourceStack> CMD = Commands.literal("scanner")

            .requires(css -> css.hasPermission(2))
            .then(LOCATE)
            .then(ADD_TARGET)
            .then(REMOVE_TARGET)
            .then(FOUND);

    public static final String CMD_F = "command.scanner.error";
    public static final String CMD_S = "command.scanner.success";

    public static final Function<Object, Component> F_NOT_FOUND = t ->
            CreateRNS.translatable(CMD_F + ".not_found", t.toString());

    public static final Function<Object, Component> F_NOT_FOUND_IN_CHUNK = t ->
            CreateRNS.translatable(CMD_F + ".not_found_in_chunk", t.toString());

    public static final BiFunction<Object, Integer, Component> F_NOT_FOUND_IN_X_CHUNKS = (t, n) ->
            CreateRNS.translatable(CMD_F + ".not_found_in_x_chunks", t.toString(), n);

    public static final Function<Object, Component> F_NOT_REGISTERED = t ->
            CreateRNS.translatable(CMD_F + ".not_registered", t.toString());

    public static final Supplier<Component> F_OCCUPIED = () ->
            CreateRNS.translatable(CMD_F + ".occupied");

    public static final Function3<Object, Component, Boolean, Component> F_ALREADY_FOUND = (t, c, v) ->
            CreateRNS.translatable(CMD_F + ".already_state", t.toString(), c, foundState(v));

    public static final Supplier<Component> F_CALLER_NOT_ENTITY = () ->
            CreateRNS.translatable(CMD_F + ".caller_not_entity");

    public static final Function<Object, Component> F_STRUCTURE_UNKNOWN = t ->
            CreateRNS.translatable(CMD_F + ".structure_unknown", t.toString());

    public static final Function<Object, Component> F_STRUCTURE_NOT_DEPOSIT = t ->
            CreateRNS.translatable(CMD_F + ".structure_not_deposit", t.toString());

    public static final Function3<Object, Component, Boolean, Component> S_ADD_RM = (t, c, v) ->
            CreateRNS.translatable(CMD_S + ".target_state", t.toString(), c, scannableState(v));

    public static final Function3<Object, Component, Boolean, Component> S_IS_FOUND = (t, c, v) ->
            CreateRNS.translatable(CMD_S + ".target_state", t.toString(), c, foundState(v));

    public static final Function3<Object, Component, Boolean, Component> S_NOW_FOUND = (t, c, v) ->
            CreateRNS.translatable(CMD_S + ".marked_state", t.toString(), c, foundState(v));

    public static final Function3<Object, Component, Integer, Component> S_FOUND_AT = (t, c, n) ->
            CreateRNS.translatable(CMD_S + ".found_at", t.toString(), c, Component.literal(n + "").withStyle(ChatFormatting.GOLD));

    public static final Function<Integer, Component> S_AVG_DIST = (n) ->
            CreateRNS.translatable(CMD_S + ".avg_dist", Component.literal(n + "").withStyle(ChatFormatting.GOLD));

    public static final Supplier<Component> S_FORGOT_FOUND = () ->
            CreateRNS.translatable(CMD_S + ".forgot_all_found");

    private static final int SEARCH_RADIUS_CHUNKS = 600; // 9600 blocks

    private static Component foundState(boolean isFound) {
        return CreateRNS.translatable(isFound
                ? "command.scanner.state.found"
                : "command.scanner.state.not_found");
    }

    private static Component scannableState(boolean isScannable) {
        return CreateRNS.translatable(isScannable
                ? "command.scanner.state.scannable"
                : "command.scanner.state.not_scannable");
    }

    private static Command<CommandSourceStack> execAddRmTarget(boolean isAdd, boolean isVein) {
        return ctx -> {
            var src = ctx.getSource();
            var sl = src.getLevel();
            var depIndex = IDepositIndex.get(sl);
            if (isVein) {
                var start = BlockPosArgument.getLoadedBlockPos(ctx, "target_position");
                var vein = DepositBlock.getVein(sl, start);
                if (vein.isEmpty()) {
                    src.sendFailure(F_NOT_FOUND.apply("Vein"));
                    return 0;
                }
                var center = BoundingBox.encapsulatingPositions(vein.keySet()).orElseThrow().getCenter();
                if (isAdd) {
                    var depKey = getDepositResourceKeyFromArg(ctx, "structure");
                    var isAdded = depIndex.addCustomDeposit(new CustomServerDepositLocation(depKey, center));
                    if (!isAdded) {
                        src.sendFailure(F_OCCUPIED.get());
                        return 0;
                    }
                    var targetStr = "The %d-block vein of type %s with center at ".formatted(vein.size(), depKey.location());
                    src.sendSuccess(() -> S_ADD_RM.apply(targetStr, bracketedCoords(center, false), true), true);
                } else throw new RuntimeException("Not Implemented");
            } else {
                var pos = BlockPosArgument.getLoadedBlockPos(ctx, "target_position");
                if (isAdd) {
                    var depKey = getDepositResourceKeyFromArg(ctx, "structure");
                    var isAdded = depIndex.addCustomDeposit(new CustomServerDepositLocation(depKey, pos));
                    if (!isAdded) {
                        src.sendFailure(F_OCCUPIED.get());
                        return 0;
                    }
                    src.sendSuccess(() -> S_ADD_RM.apply(
                            depKey.location(), bracketedCoords(pos, false), true), true);
                } else {
                    var closestCustom = CustomServerDepositLocation.getNearestCustom(sl, pos, true, 1);
                    if (closestCustom == null) {
                        src.sendFailure(F_NOT_FOUND_IN_CHUNK.apply("Target"));
                        return 0;
                    }
                    boolean isRemoved = depIndex.removeCustomDeposit(closestCustom);
                    if (!isRemoved) {
                        src.sendFailure(F_NOT_REGISTERED.apply(closestCustom.getKey().location()));
                        return 0;
                    }
                    src.sendSuccess(() -> S_ADD_RM.apply(
                            closestCustom.getKey().location(), bracketedCoords(pos, false), false), true);
                }
            }
            return SINGLE_SUCCESS;
        };
    }

    private static Command<CommandSourceStack> execIsFound() {
        return ctx -> {
            var src = ctx.getSource();
            var sl = src.getLevel();
            var pos = BlockPosArgument.getLoadedBlockPos(ctx, "target_position");
            var dep = ServerDepositLocation.getNearest(sl, RNSStructureTags.DEPOSITS, pos, true, 1);
            if (dep == null) {
                src.sendFailure(F_NOT_FOUND_IN_CHUNK.apply(RNSStructureTags.DEPOSITS.location()));
                return 0;
            }
            var precise = dep.computePreciseLocation();
            var depPos = dep.getLocation();
            var isFound = dep.isFound(sl);
            src.sendSuccess(() -> S_IS_FOUND.apply(
                    dep.getKey().location(), bracketedCoords(depPos, !precise), isFound), false);
            return SINGLE_SUCCESS;
        };
    }

    private static Command<CommandSourceStack> execMarkFound(boolean forgetAll) {
        return ctx -> {
            var src = ctx.getSource();
            var sl = src.getLevel();
            if (forgetAll) {
                IDepositIndex.get(sl).forgetFoundDeposits();
                src.sendSuccess(S_FORGOT_FOUND, true);
                return SINGLE_SUCCESS;
            }

            var pos = BlockPosArgument.getLoadedBlockPos(ctx, "target_position");
            var dep = ServerDepositLocation.getNearest(sl, RNSStructureTags.DEPOSITS, pos, true, 1);
            if (dep == null) {
                src.sendFailure(F_NOT_FOUND_IN_CHUNK.apply(RNSStructureTags.DEPOSITS.location()));
                return 0;
            }

            boolean val = BoolArgumentType.getBool(ctx, "new_value");
            var isSet = dep.setFound(sl, val);
            var precise = dep.computePreciseLocation();
            var depPos = dep.getLocation();
            if (!isSet) {
                src.sendFailure(F_ALREADY_FOUND.apply(dep.getKey().location(), bracketedCoords(depPos, !precise), val));
                return 0;
            }

            src.sendSuccess(() -> S_NOW_FOUND.apply(
                    dep.getKey().location(), bracketedCoords(depPos, !precise), val), true);
            return SINGLE_SUCCESS;
        };
    }

    private static Command<CommandSourceStack> execLocate(boolean undiscovered) {
        return ctx -> {
            var src = ctx.getSource();
            var sl = src.getLevel();
            var entity = src.getEntity();
            if (entity == null) {
                src.sendFailure(F_CALLER_NOT_ENTITY.get());
                return 0;
            }
            int searchRadiusChunks = SEARCH_RADIUS_CHUNKS;
            var pos = entity.blockPosition();
            var resOrTag = getDepositKeyFromArg(ctx, "structure");

            var nearest = ServerDepositLocation.getNearest(sl, resOrTag, pos, !undiscovered, searchRadiusChunks);
            if (nearest == null) {
                // Use structure ID provided by the argument
                src.sendFailure(F_NOT_FOUND_IN_X_CHUNKS.apply(getIdFromResOrTag(resOrTag), searchRadiusChunks));
            } else {
                // Use structure ID resolved by getNearest()
                var dist = (int) Math.sqrt(nearest.getLocation().distSqr(pos));
                src.sendSuccess(() -> S_FOUND_AT.apply(nearest.getKey().location(), bracketedCoords(nearest.getLocation(),
                        !nearest.computePreciseLocation()), dist), false);
            }

            return SINGLE_SUCCESS;
        };
    }

    private static Command<CommandSourceStack> execLocateAll(boolean positionSpecified) {
        return ctx -> {
            var src = ctx.getSource();
            var sl = src.getLevel();
            BlockPos pos;
            if (positionSpecified) {
                pos = BlockPosArgument.getBlockPos(ctx, "source_position");
            } else {
                pos = BlockPos.containing(src.getPosition());
            }
            int searchRadiusChunks = SEARCH_RADIUS_CHUNKS;
            var lookup = sl.registryAccess().lookupOrThrow(Registries.STRUCTURE);
            var deposits = lookup.get(RNSStructureTags.DEPOSITS).orElse(null);

            if (deposits == null) {
                src.sendFailure(F_NOT_FOUND.apply(RNSStructureTags.DEPOSITS.location()));
                return 0;
            }

            var nearestAny = ServerDepositLocation.getNearest(sl, RNSStructureTags.DEPOSITS, pos, true, searchRadiusChunks);
            if (nearestAny == null) {
                src.sendFailure(F_NOT_FOUND_IN_X_CHUNKS.apply(RNSStructureTags.DEPOSITS.location(), searchRadiusChunks));
            } else {
                var dist = (int) Math.sqrt(nearestAny.getLocation().distSqr(pos));
                src.sendSuccess(() -> S_FOUND_AT.apply(RNSStructureTags.DEPOSITS.location(), bracketedCoords(
                        nearestAny.getLocation(), !nearestAny.computePreciseLocation()), dist), false);
            }

            var depKeys = deposits.stream()
                    .map(h -> h.unwrapKey().orElse(null))
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparing(k -> k.location().toString()))
                    .toList();

            int distSum = 0;
            for (var depKey : depKeys) {
                var nearest = ServerDepositLocation.getNearest(sl, depKey, pos, true, searchRadiusChunks);
                if (nearest == null) {
                    src.sendFailure(F_NOT_FOUND_IN_X_CHUNKS.apply(depKey.location(), searchRadiusChunks));
                } else {
                    var dist = (int) Math.sqrt(nearest.getLocation().distSqr(pos));
                    distSum += dist;
                    src.sendSuccess(() -> S_FOUND_AT.apply(nearest.getKey().location(), bracketedCoords(
                            nearest.getLocation(), !nearest.computePreciseLocation()), dist), false);
                }
            }

            if (!depKeys.isEmpty() && distSum > 0) {
                var avgDist = distSum / depKeys.size();
                src.sendSuccess(() -> S_AVG_DIST.apply(avgDist), false);
            }

            return SINGLE_SUCCESS;
        };
    }

    private static Either<ResourceKey<Structure>, TagKey<Structure>> getDepositKeyFromArg(
            CommandContext<CommandSourceStack> ctx, String arg
    ) throws CommandSyntaxException {
        var ex = new DynamicCommandExceptionType(F_STRUCTURE_UNKNOWN::apply);
        var resOrTag = ResourceOrTagKeyArgument.getResourceOrTagKey(ctx, arg, Registries.STRUCTURE, ex).unwrap();
        var lookup = ctx.getSource().getLevel().registryAccess().lookupOrThrow(Registries.STRUCTURE);

        // Argument is one structure
        var depKey = resOrTag.left().orElse(null);
        if (depKey != null) {
            ensureIsDepositStructure(lookup, depKey);
            return Either.left(depKey);
        }

        // Argument is a structure tag
        var depTag = resOrTag.right().orElse(null);
        assert depTag != null;
        var depHS = lookup.get(depTag).orElse(null);
        if (depHS == null)
            throw new DynamicCommandExceptionType(F_STRUCTURE_UNKNOWN::apply).create(depTag.location());

        var depKeys = depHS.stream()
                .map(h -> h.unwrapKey().orElse(null))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(k -> k.location().toString()))
                .toList();
        for (var k : depKeys) {
            ensureIsDepositStructure(lookup, k);
        }
        return Either.right(depTag);
    }

    private static ResourceKey<Structure> getDepositResourceKeyFromArg(
            CommandContext<CommandSourceStack> ctx, String arg
    ) throws CommandSyntaxException {
        var depKey = ResourceKeyArgument.getStructure(ctx, arg).unwrapKey().orElseThrow();
        var lookup = ctx.getSource().getLevel().registryAccess().lookupOrThrow(Registries.STRUCTURE);
        ensureIsDepositStructure(lookup, depKey);
        return depKey;
    }

    private static void ensureIsDepositStructure
            (RegistryLookup<Structure> reg, ResourceKey<Structure> key) throws CommandSyntaxException {
        var holder = reg.get(key).orElse(null);
        if (holder == null || !holder.is(RNSStructureTags.DEPOSITS)) {
            throw new DynamicCommandExceptionType(F_STRUCTURE_NOT_DEPOSIT::apply).create(key.location());
        }
    }

    private static <T> ResourceLocation getIdFromResOrTag(Either<ResourceKey<T>, TagKey<T>> resOrTag) {
        var left = resOrTag.left().orElse(null);
        var right = resOrTag.right().orElse(null);
        assert left != null || right != null;
        return (left != null) ? left.location() : right.location();
    }

    private static Component bracketedCoords(BlockPos pos, boolean hideY) {
        String copied = "%d %d %d".formatted(pos.getX(), pos.getY(), pos.getZ());
        MutableComponent raw = Component.literal("[%d, %s, %d]".formatted(pos.getX(), (hideY) ? "~" : pos.getY(), pos.getZ()));
        return raw.setStyle(Style.EMPTY
                .withColor(ChatFormatting.GREEN)
                .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, copied))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        Component.literal("Click to copy"))));
    }
}
