package com.bmaster.createrns.infrastructure.command;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.RNSBlocks;
import com.bmaster.createrns.RNSDeposits;
import com.bmaster.createrns.content.deposit.mining.contraption.MinerBearingBlock;
import com.bmaster.createrns.content.deposit.mining.contraption.attachment.minehead.MineHeadBlock;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public final class TemplateCommands {
    public static final LiteralArgumentBuilder<CommandSourceStack> PLACE_CMD;
    public static final LiteralArgumentBuilder<CommandSourceStack> REMOVE_CMD;

    private static final String TEMPLATE_ARGUMENT = "template";
    private static final String START_ARGUMENT = "start";
    private static final String COUNT_ARGUMENT = "count";
    private static final int MINER_FOOTPRINT_SIZE = 3;
    private static final int TEMPLATE_GAP = 1;
    private static final int MOTOR_RPM = 256;
    private static final Direction MINER_DIRECTION = Direction.DOWN;
    private static final Map<String, PlacementTemplate> TEMPLATES;

    static {
        TEMPLATES = createTemplates();
        PLACE_CMD = createCommand("place", TemplateAction.PLACE);
        REMOVE_CMD = createCommand("remove", TemplateAction.REMOVE);
    }

    private TemplateCommands() {}

    private static Map<String, PlacementTemplate> createTemplates() {
        var minerSetup = PlacementTemplate.withGap(
                "miner_setup",
                MINER_FOOTPRINT_SIZE,
                MINER_FOOTPRINT_SIZE,
                TEMPLATE_GAP,
                List.of(
                        TemplatePart.fill(
                                BlockPos.ZERO,
                                new BlockPos(2, 0, 2),
                                RNSDeposits.IRON_DEPOSIT::getDefaultState),
                        TemplatePart.block(
                                new BlockPos(1, 3, 1),
                                () -> RNSBlocks.MINER_BEARING.getDefaultState()
                                        .setValue(MinerBearingBlock.FACING, MINER_DIRECTION)),
                        TemplatePart.block(
                                new BlockPos(1, 2, 1),
                                Blocks.BARREL::defaultBlockState),
                        TemplatePart.block(
                                new BlockPos(1, 1, 1),
                                () -> MineHeadBlock.withConnectedDirection(
                                        RNSBlocks.MINE_HEAD.getDefaultState(), MINER_DIRECTION))));

        var motor = PlacementTemplate.withGap(
                "motor",
                MINER_FOOTPRINT_SIZE,
                MINER_FOOTPRINT_SIZE,
                TEMPLATE_GAP,
                List.of(TemplatePart.block(
                        new BlockPos(1, 4, 1),
                        () -> AllBlocks.CREATIVE_MOTOR.getDefaultState()
                                .setValue(CreativeMotorBlock.FACING, MINER_DIRECTION),
                        TemplateCommands::validateMotor,
                        TemplateCommands::configureMotor)));

        return Map.of(minerSetup.name(), minerSetup, motor.name(), motor);
    }

    private static LiteralArgumentBuilder<CommandSourceStack> createCommand(String name, TemplateAction action) {
        return Commands.literal(name)
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument(TEMPLATE_ARGUMENT, StringArgumentType.word())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(TEMPLATES.keySet(), builder))
                        .then(Commands.argument(START_ARGUMENT, BlockPosArgument.blockPos())
                                .then(Commands.argument(COUNT_ARGUMENT, IntegerArgumentType.integer(1))
                                        .executes(context -> execute(context, action)))));
    }

    private static int execute(CommandContext<CommandSourceStack> context, TemplateAction action) {
        var source = context.getSource();
        var templateName = StringArgumentType.getString(context, TEMPLATE_ARGUMENT);
        var template = TEMPLATES.get(templateName);
        if (template == null) {
            source.sendFailure(CreateRNS.translatable("command.debug.template.unknown", templateName));
            return 0;
        }

        var level = source.getLevel();
        var start = BlockPosArgument.getBlockPos(context, START_ARGUMENT);
        int count = IntegerArgumentType.getInteger(context, COUNT_ARGUMENT);
        var failure = template.visitBlocks(start, count, (part, pos) -> {
            if (!level.isLoaded(pos) || level.isOutsideBuildHeight(pos)) {
                return CreateRNS.translatable("command.debug.template.invalid_area");
            }
            if (action == TemplateAction.PLACE && part.validator() != null) {
                return part.validator().validate(level, pos);
            }
            return null;
        });
        if (failure != null) {
            source.sendFailure(failure);
            return 0;
        }

        template.visitBlocks(start, count, (part, pos) -> {
            if (action == TemplateAction.REMOVE) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                return null;
            }

            level.setBlock(pos, part.state().get(), Block.UPDATE_ALL);
            if (part.initializer() != null) part.initializer().accept(level, pos.immutable());
            return null;
        });

        var templateNameComponent = CreateRNS.translatable("command.debug.template." + template.name());
        source.sendSuccess(() -> CreateRNS.translatable(
                action.successKey,
                count,
                templateNameComponent,
                start.getX(),
                start.getY(),
                start.getZ()), true);
        return count;
    }

    private static @Nullable Component validateMotor(ServerLevel level, BlockPos motorPos) {
        var bearingPos = motorPos.below();
        if (level.getBlockState(bearingPos).is(RNSBlocks.MINER_BEARING.get())) return null;
        return CreateRNS.translatable(
                "command.debug.template.motor.missing_bearing",
                bearingPos.getX(), bearingPos.getY(), bearingPos.getZ());
    }

    private static void configureMotor(ServerLevel level, BlockPos motorPos) {
        if (!(level.getBlockEntity(motorPos) instanceof CreativeMotorBlockEntity motor)) {
            throw new IllegalStateException("Expected creative motor block entity at " + motorPos);
        }

        // Facing down reverses the generated rotation direction.
        motor.generatedSpeed.setValue(-MOTOR_RPM);
    }

    private enum TemplateAction {
        PLACE("command.debug.template.place.success"),
        REMOVE("command.debug.template.remove.success");

        private final String successKey;

        TemplateAction(String successKey) {
            this.successKey = successKey;
        }
    }

    @FunctionalInterface
    private interface PlacementValidator {
        @Nullable Component validate(ServerLevel level, BlockPos pos);
    }

    @FunctionalInterface
    private interface TemplateBlockVisitor {
        @Nullable Component visit(TemplatePart part, BlockPos pos);
    }

    private record PlacementTemplate(
            String name,
            int footprintWidth,
            int footprintDepth,
            int xStride,
            int zStride,
            List<TemplatePart> parts
    ) {
        private PlacementTemplate {
            if (footprintWidth < 1 || footprintDepth < 1) {
                throw new IllegalArgumentException("Template footprint must have positive dimensions");
            }
            if (xStride < footprintWidth || zStride < footprintDepth) {
                throw new IllegalArgumentException("Template stride cannot be smaller than its footprint");
            }
            if (parts.isEmpty()) throw new IllegalArgumentException("Template must contain at least one part");

            parts = List.copyOf(parts);
            for (var part : parts) {
                if (part.from().getX() < 0 || part.to().getX() >= footprintWidth ||
                        part.from().getZ() < 0 || part.to().getZ() >= footprintDepth) {
                    throw new IllegalArgumentException("Template part lies outside the declared footprint");
                }
            }
        }

        private static PlacementTemplate withGap(
                String name,
                int footprintWidth,
                int footprintDepth,
                int gap,
                List<TemplatePart> parts
        ) {
            if (gap < 0) throw new IllegalArgumentException("Template gap cannot be negative");
            return new PlacementTemplate(
                    name,
                    footprintWidth,
                    footprintDepth,
                    Math.addExact(footprintWidth, gap),
                    Math.addExact(footprintDepth, gap),
                    parts);
        }

        private @Nullable Component visitBlocks(BlockPos start, int count, TemplateBlockVisitor visitor) {
            int columns = Math.clamp((int) Math.ceil(Math.sqrt((double) count * zStride / xStride)), 1, count);
            var cursor = new BlockPos.MutableBlockPos();

            for (int index = 0; index < count; index++) {
                long originX = (long) start.getX() + (long) (index % columns) * xStride;
                long originZ = (long) start.getZ() + (long) (index / columns) * zStride;

                for (var part : parts) {
                    for (int x = part.from().getX(); x <= part.to().getX(); x++) {
                        for (int y = part.from().getY(); y <= part.to().getY(); y++) {
                            for (int z = part.from().getZ(); z <= part.to().getZ(); z++) {
                                long blockX = originX + x;
                                long blockY = (long) start.getY() + y;
                                long blockZ = originZ + z;
                                if (blockX < Integer.MIN_VALUE || blockX > Integer.MAX_VALUE ||
                                        blockY < Integer.MIN_VALUE || blockY > Integer.MAX_VALUE ||
                                        blockZ < Integer.MIN_VALUE || blockZ > Integer.MAX_VALUE) {
                                    return CreateRNS.translatable("command.debug.template.invalid_area");
                                }

                                cursor.set((int) blockX, (int) blockY, (int) blockZ);
                                var failure = visitor.visit(part, cursor);
                                if (failure != null) return failure;
                            }
                        }
                    }
                }
            }
            return null;
        }
    }

    private record TemplatePart(
            BlockPos from,
            BlockPos to,
            Supplier<BlockState> state,
            @Nullable PlacementValidator validator,
            @Nullable BiConsumer<ServerLevel, BlockPos> initializer
    ) {
        private TemplatePart {
            if (from.getX() > to.getX() || from.getY() > to.getY() || from.getZ() > to.getZ()) {
                throw new IllegalArgumentException("Template part bounds are reversed");
            }
        }

        private static TemplatePart block(BlockPos offset, Supplier<BlockState> state) {
            return new TemplatePart(offset, offset, state, null, null);
        }

        private static TemplatePart block(
                BlockPos offset,
                Supplier<BlockState> state,
                PlacementValidator validator,
                BiConsumer<ServerLevel, BlockPos> initializer
        ) {
            return new TemplatePart(offset, offset, state, validator, initializer);
        }

        private static TemplatePart fill(BlockPos from, BlockPos to, Supplier<BlockState> state) {
            return new TemplatePart(from, to, state, null, null);
        }
    }
}
