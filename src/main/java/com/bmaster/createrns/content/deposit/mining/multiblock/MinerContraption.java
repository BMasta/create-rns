package com.bmaster.createrns.content.deposit.mining.multiblock;

import com.bmaster.createrns.RNSContent;
import com.bmaster.createrns.util.Utils;
import com.simibubi.create.content.contraptions.AssemblyException;
import com.simibubi.create.content.contraptions.bearing.BearingContraption;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.Queue;
import java.util.Set;

public class MinerContraption extends BearingContraption {
    public BlockPos drillHeadPos;

    public MinerContraption(boolean isWindmill, Direction facing) {
        super(isWindmill, facing);
    }

    @Override
    public boolean isActorTypeDisabled(ItemStack filter) {
        return super.isActorTypeDisabled(filter);
    }

    @Override
    public boolean searchMovedStructure(Level world, BlockPos pos, @Nullable Direction forcedDirection) throws AssemblyException {
        drillHeadPos = null;
        if (world.getBlockState(pos).is(RNSContent.DRILL_HEAD_BLOCK.get())) {
            anchor = pos;
            if (bounds == null) bounds = new AABB(BlockPos.ZERO);
            addBlock(world, pos, capture(world, pos));
            return true;
        }
        boolean result = super.searchMovedStructure(world, pos, forcedDirection);
        if (drillHeadPos == null) throw new RNSAssemblyException("not_one_drill_head");

        // Local position of the drill head must not differ from origin on any axis other than the one the contraption is facing
        if (Utils.dot(Utils.normalVecFlip(facing, true), toLocalPos(drillHeadPos)) != 0) {
            throw new RNSAssemblyException("drill_head_not_aligned_with_bearing");
        }

        return result;
    }

    @Override
    protected boolean moveBlock(Level world, @Nullable Direction forcedDirection, Queue<BlockPos> frontier, Set<BlockPos> visited) throws AssemblyException {
        var pos = frontier.peek();
        if (pos != null) {
            var bs = world.getBlockState(pos);
            if (bs.is(RNSContent.DRILL_HEAD_BLOCK.get())) {
                if (drillHeadPos != null) throw new RNSAssemblyException("not_one_drill_head");
                if (bs.getValue(DrillHeadBlock.FACING) != facing)
                    throw new RNSAssemblyException("wrong_drill_head_direction");
                drillHeadPos = pos;
            }
        }
        return super.moveBlock(world, forcedDirection, frontier, visited);
    }
}
