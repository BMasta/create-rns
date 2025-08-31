package com.bmaster.createrns.mining.miner;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityVisual;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.content.kinetics.base.SingleAxisRotatingVisual;
import com.simibubi.create.foundation.render.AllInstanceTypes;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class MinerVisual extends KineticBlockEntityVisual<MinerBlockEntity> {
    private final RotatingInstance shaft;
    private final RotatingInstance drill_head;

    public MinerVisual(VisualizationContext context, MinerBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);

        shaft = instancerProvider().instancer(AllInstanceTypes.ROTATING, Models.partial(AllPartialModels.SHAFT_HALF))
                .createInstance()
                .rotateToFace(Direction.SOUTH, Direction.UP)
                .setup(blockEntity)
                .setPosition(getVisualPosition());

        drill_head = instancerProvider().instancer(AllInstanceTypes.ROTATING, Models.partial(AllPartialModels.DRILL_HEAD))
                .createInstance()
                .rotateToFace(Direction.SOUTH, Direction.DOWN)
                .setup(blockEntity)
                .setPosition(getVisualPosition());
    }

    @Override
    protected Direction.Axis rotationAxis() {
        return MinerBlock.getRotationAxis();
    }

    @Override
    public void update(float partialTick) {
        shaft.setup(blockEntity).setChanged();
        drill_head.setup(blockEntity).setChanged();
    }

    @Override
    public void updateLight(float partialTick) {
        relight(pos, shaft);
        relight(pos, drill_head);
    }

    @Override
    public void collectCrumblingInstances(Consumer<@Nullable Instance> consumer) {
        consumer.accept(shaft);
        consumer.accept(drill_head);
    }

    @Override
    protected void _delete() {
        shaft.delete();
        drill_head.delete();
    }
}
