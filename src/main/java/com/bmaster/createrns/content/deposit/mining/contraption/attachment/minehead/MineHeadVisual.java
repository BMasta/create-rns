package com.bmaster.createrns.content.deposit.mining.contraption.attachment.minehead;

import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.baked.BakedModelBuilder;
import dev.engine_room.flywheel.lib.model.baked.SinglePosVirtualBlockGetter;
import dev.engine_room.flywheel.lib.util.RendererReloadCache;
import dev.engine_room.flywheel.lib.visual.AbstractBlockEntityVisual;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Consumer;

public class MineHeadVisual extends AbstractBlockEntityVisual<MineHeadBlockEntity> {
    private static final RendererReloadCache<BlockState, Model> MODELS =
            new RendererReloadCache<>(MineHeadVisual::createModel);

    protected final TransformedInstance mineHead;

    private static Model createModel(BlockState state) {
        var level = SinglePosVirtualBlockGetter.createFullDark().blockState(state);
        var bakedModel = Minecraft.getInstance().getBlockRenderer().getBlockModel(state);
        return BakedModelBuilder.create(bakedModel).level(level).build();
    }

    public MineHeadVisual(VisualizationContext ctx, MineHeadBlockEntity blockEntity, float partialTick) {
        super(ctx, blockEntity, partialTick);

        mineHead = instancerProvider()
                .instancer(InstanceTypes.TRANSFORMED, MODELS.get(blockState))
                .createInstance();

        var transform = mineHead.setIdentityTransform().translate(getVisualPosition());
        MineHeadRenderer.applyLocalTransforms(transform, blockState).setChanged();
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        consumer.accept(mineHead);
    }

    @Override
    public void updateLight(float partialTick) {
        relight(mineHead);
    }

    @Override
    protected void _delete() {
        mineHead.delete();
    }
}
