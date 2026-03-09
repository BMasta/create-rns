package com.bmaster.createrns.content.deposit.mining.contraption.attachment.drillhead;

import com.bmaster.createrns.RNSBlockEntities;
import com.bmaster.createrns.RNSBlocks;
import com.bmaster.createrns.content.deposit.mining.contraption.attachment.FaceAttachedMinerComponentBlock;
import com.simibubi.create.AllShapes;
import com.simibubi.create.foundation.block.IBE;
import net.createmod.catnip.math.VoxelShaper;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class DrillHeadBlock extends FaceAttachedMinerComponentBlock implements IBE<DrillHeadBlockEntity> {
    public static final VoxelShaper SHAPE = new AllShapes.Builder(Block.box(0, 0, 0, 16, 12, 16)).forDirectional();
    public static final EnumProperty<DrillHeadSize> SIZE = EnumProperty.create("size", DrillHeadSize.class);

    public DrillHeadBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(SIZE, DrillHeadSize.SMALL));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(SIZE);
    }

//    @Override
//    protected ItemInteractionResult useItemOn(
//            ItemStack stack, BlockState state, Level level, BlockPos pos,
//            Player player, InteractionHand hand, BlockHitResult hitResult
//    ) {
//        if (!stack.is(RNSBlocks.DRILL_HEAD.get().asItem()))
//            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
//        if (!player.mayBuild()) return ItemInteractionResult.FAIL;
//
//        if (level.isClientSide) return ItemInteractionResult.SUCCESS;
//        boolean upgraded = DrillHeadMultiblock.tryUpgrade(level, pos, state);
//        if (!upgraded) return ItemInteractionResult.CONSUME;
//
//        if (!player.isCreative()) stack.shrink(1);
//        return ItemInteractionResult.SUCCESS;
//    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE.get(getConnectedDirection(state));
    }

    @SuppressWarnings("deprecation")
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        int drillCount = state.getValue(SIZE).getDrillHeadCost();
        return List.of(new ItemStack(RNSBlocks.DRILL_HEAD.get(), drillCount));
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide && !state.is(newState.getBlock())) {
            DrillHeadMultiblock.removePartBlocks(level, pos, state);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }

    @Override
    public Class<DrillHeadBlockEntity> getBlockEntityClass() {
        return DrillHeadBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends DrillHeadBlockEntity> getBlockEntityType() {
        return RNSBlockEntities.DRILL_HEAD_BE.get();
    }
}
