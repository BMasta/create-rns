package com.bmaster.createrns.content.deposit.mining.contraption;

import com.bmaster.createrns.RNSBlocks;
import com.bmaster.createrns.content.deposit.mining.contraption.attachment.drillhead.DrillHeadBlock;
import com.bmaster.createrns.content.deposit.mining.contraption.attachment.resonance.buffer.ResonanceBufferBlock;
import com.bmaster.createrns.content.deposit.mining.contraption.attachment.resonance.resonator.AbstractResonatorBlock;
import com.bmaster.createrns.util.Utils;
import com.simibubi.create.content.contraptions.AssemblyException;
import com.simibubi.create.content.contraptions.bearing.BearingContraption;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Queue;
import java.util.Set;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class MinerContraption extends BearingContraption {
    public static final int BASE_RESONATOR_LIMIT = 4;
    public static final int BUFFER_LIMIT = 4;

    public BlockPos drillHeadPos;
    public int resonatorCount = 0;
    public int bufferCount = 0;

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
        boolean result = super.searchMovedStructure(world, pos, forcedDirection);
        int resonatorLimit = BASE_RESONATOR_LIMIT + bufferCount;
        // No drill heads found
        if (drillHeadPos == null) {
            throw new RNSAssemblyException("not_one_drill");
        } else if (bufferCount > BUFFER_LIMIT) {
            throw new RNSAssemblyException("too_many_buffers", BUFFER_LIMIT);
        } else if (resonatorCount > resonatorLimit) {
            throw new RNSAssemblyException("too_many_resonators", resonatorLimit);
        }
        return result;
    }

    @Override
    protected boolean moveBlock(
            Level world, @Nullable Direction forcedDirection, Queue<BlockPos> frontier, Set<BlockPos> visited
    ) throws AssemblyException {
        var pos = frontier.peek();
        if (pos != null) {
            var bs = world.getBlockState(pos);
            var b = bs.getBlock();
            if (bs.getBlock() instanceof DrillHeadBlock && DrillHeadBlock.getConnectedDirection(bs) != facing) {
                // Drill is not facing forward
                throw new RNSAssemblyException("wrong_drill_direction");
            }

            if (bs.is(RNSBlocks.DRILL_HEAD.get())) {
                // Multiple drill heads found
                if (drillHeadPos != null) throw new RNSAssemblyException("not_one_drill");
                // Local position of the drill differs from origin on an axis the contraption is not facing
                if (Utils.dot(Utils.normalVecFlip(facing, true), toLocalPos(pos)) != 0) {
                    throw new RNSAssemblyException("drill_not_aligned");
                }
                drillHeadPos = pos;
            } else if (b instanceof AbstractResonatorBlock) {
                resonatorCount++;
            } else if (b instanceof ResonanceBufferBlock) {
                bufferCount++;
            }
        }
        return super.moveBlock(world, forcedDirection, frontier, visited);
    }
}
