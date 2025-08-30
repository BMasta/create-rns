package com.bmaster.createrns.mining.miner;

import com.simibubi.create.content.kinetics.base.KineticBlockEntityVisual;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class MinerVisual extends KineticBlockEntityVisual<MinerBlockEntity> {
//    private final RotatingInstance instance = instancerProvider().instancer(AllInstanceTypes.ROTATING,
//                    Models.partial(AllPartialModels.SHAFT_HALF)).createInstance()

    public MinerVisual(VisualizationContext context, MinerBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
    }

    @Override
    protected Direction.Axis rotationAxis() {
        return MinerBlock.getRotationAxis();
    }

    @Override
    public void update(float partialTick) {
        super.update(partialTick);
    }

    @Override
    public void updateLight(float partialTick) {
    }

    @Override
    public void collectCrumblingInstances(Consumer<@Nullable Instance> consumer) {
    }

    @Override
    protected void _delete() {
    }
}
