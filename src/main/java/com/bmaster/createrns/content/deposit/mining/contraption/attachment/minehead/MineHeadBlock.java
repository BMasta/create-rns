package com.bmaster.createrns.content.deposit.mining.contraption.attachment.minehead;

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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@SuppressWarnings("deprecation")
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class MineHeadBlock extends FaceAttachedMinerComponentBlock implements IBE<MineHeadBlockEntity> {
    public static final VoxelShaper SHAPE = new AllShapes.Builder(Block.box(0, 0, 0, 16, 8, 16)).forDirectional();
    public static final EnumProperty<MineHeadSize> SIZE = EnumProperty.create("size", MineHeadSize.class);

    public MineHeadBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(SIZE, MineHeadSize.SMALL));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(SIZE);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level.isClientSide || state.is(oldState.getBlock()) || movedByPiston) return;
        if (state.getValue(SIZE) != MineHeadSize.SMALL) return;
        MineHeadMultiblock.formMultiblock(level, pos, state);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (state.getValue(SIZE) != MineHeadSize.SMALL) return Shapes.block();
        return SHAPE.get(getConnectedDirection(state));
    }

    @SuppressWarnings("deprecation")
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        int mineHeadCount = switch (state.getValue(SIZE)) {
            case SMALL -> 1;
            case LARGE -> 0;
        };
        if (mineHeadCount == 0) return List.of();
        return List.of(new ItemStack(RNSBlocks.MINE_HEAD.get(), mineHeadCount));
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide && !movedByPiston && !state.is(newState.getBlock()) &&
                !newState.is(Blocks.IRON_BLOCK) && !newState.is(RNSBlocks.MINE_HEAD_PART.get())) {
            // Trigger multiblock removal
            MineHeadMultiblock.breakMultiblock(level, pos, state, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }

    @Override
    public Class<MineHeadBlockEntity> getBlockEntityClass() {
        return MineHeadBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends MineHeadBlockEntity> getBlockEntityType() {
        return RNSBlockEntities.MINE_HEAD.get();
    }
}
