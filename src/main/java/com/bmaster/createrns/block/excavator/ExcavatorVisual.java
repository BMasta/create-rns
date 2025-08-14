package com.bmaster.createrns.block.excavator;

import com.simibubi.create.content.kinetics.base.KineticBlockEntityVisual;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class ExcavatorVisual extends KineticBlockEntityVisual<ExcavatorBlockEntity> {
//    private final RotatingInstance instance = instancerProvider().instancer(AllInstanceTypes.ROTATING,
//                    Models.partial(AllPartialModels.SHAFT_HALF)).createInstance()

    public ExcavatorVisual(VisualizationContext context, ExcavatorBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
    }

    @Override
    protected Direction.Axis rotationAxis() {
        return ExcavatorBlock.getRotationAxis();
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
