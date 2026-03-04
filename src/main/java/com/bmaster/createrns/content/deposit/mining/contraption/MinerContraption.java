package com.bmaster.createrns.content.deposit.mining.contraption;

import com.bmaster.createrns.RNSBlocks;
import com.bmaster.createrns.content.deposit.mining.contraption.attachment.MiningEquipmentBlock;
import com.bmaster.createrns.content.deposit.mining.contraption.attachment.drillhead.DrillHeadBlock;
import com.bmaster.createrns.util.Utils;
import com.simibubi.create.content.contraptions.AssemblyException;
import com.simibubi.create.content.contraptions.bearing.BearingContraption;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
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
        if (world.getBlockState(pos).is(RNSBlocks.DRILL_HEAD_BLOCK.get())) {
            anchor = pos;
            if (bounds == null) bounds = new AABB(BlockPos.ZERO);
            addBlock(world, pos, capture(world, pos));
            return true;
        }
        boolean result = super.searchMovedStructure(world, pos, forcedDirection);
        // No drill heads found
        if (drillHeadPos == null) throw new RNSAssemblyException("not_one_drill");
        return result;
    }

    @Override
    protected boolean moveBlock(
            Level world, @Nullable Direction forcedDirection, Queue<BlockPos> frontier, Set<BlockPos> visited
    ) throws AssemblyException {
        var pos = frontier.peek();
        if (pos != null) {
            var bs = world.getBlockState(pos);
            if (bs.getBlock() instanceof DrillHeadBlock && MiningEquipmentBlock.getConnectedDirection(bs) != facing) {
                // Drill is not facing forward
                throw new RNSAssemblyException("wrong_drill_direction");
            }

            if (bs.is(RNSBlocks.DRILL_HEAD_BLOCK.get())) {
                // Multiple drill heads found
                if (drillHeadPos != null) throw new RNSAssemblyException("not_one_drill");
                // Local position of the drill differs from origin on an axis the contraption is not facing
                if (Utils.dot(Utils.normalVecFlip(facing, true), toLocalPos(pos)) != 0) {
                    throw new RNSAssemblyException("drill_not_aligned");
                }
                drillHeadPos = pos;
            }
        }
        return super.moveBlock(world, forcedDirection, frontier, visited);
    }
}
