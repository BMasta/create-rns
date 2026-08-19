package com.bmaster.createrns.content.deposit.mining.contraption;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.RNSBlocks;
import com.bmaster.createrns.RNSContraptionTypes;
import com.bmaster.createrns.RNSTags.RNSBlockTags;
import com.bmaster.createrns.content.deposit.mining.contraption.attachment.minehead.MineHeadBlock;
import com.bmaster.createrns.util.Utils;
import com.simibubi.create.api.contraption.ContraptionType;
import com.simibubi.create.content.contraptions.AssemblyException;
import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.content.contraptions.bearing.BearingContraption;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Queue;
import java.util.Set;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class MinerContraption extends BearingContraption {
    public static final int BASE_RESONATOR_LIMIT = 4;
    public static final int BUFFER_LIMIT = 4;

    public BlockPos mineHeadPos;
    public int resonatorCount = 0;
    public int bufferCount = 0;

    public MinerContraption() {
    }

    public MinerContraption(boolean isWindmill, Direction facing) {
        super(isWindmill, facing);
    }

    @Override
    public ContraptionType getType() {
        return RNSContraptionTypes.MINER_BEARING.value();
    }

    @Override
    public boolean isActorTypeDisabled(ItemStack filter) {
        return super.isActorTypeDisabled(filter);
    }

    @Override
    public boolean searchMovedStructure(Level world, BlockPos pos, @Nullable Direction forcedDirection) throws AssemblyException {
        mineHeadPos = null;
        boolean result = super.searchMovedStructure(world, pos, forcedDirection);
        int resonatorLimit = BASE_RESONATOR_LIMIT + bufferCount;
        // No mine heads found
        if (mineHeadPos == null) {
            throw new RNSAssemblyException("not_one_minehead");
        } else if (bufferCount > BUFFER_LIMIT) {
            throw new RNSAssemblyException("too_many_buffers", BUFFER_LIMIT);
        } else if (resonatorCount > resonatorLimit) {
            throw new RNSAssemblyException("too_many_resonators", resonatorLimit);
        }
        setMineHeadBlocksAssembled(true);
        return result;
    }

    @Override
    public void addBlocksToWorld(Level world, StructureTransform transform) {
        setMineHeadBlocksAssembled(false);
        super.addBlocksToWorld(world, transform);
    }

    @Override
    protected boolean moveBlock(
            Level world, @Nullable Direction forcedDirection, Queue<BlockPos> frontier, Set<BlockPos> visited
    ) throws AssemblyException {
        var pos = frontier.peek();
        if (pos != null) {
            var bs = world.getBlockState(pos);
            var b = bs.getBlock();
            if (bs.getBlock() instanceof MineHeadBlock && MineHeadBlock.getConnectedDirection(bs) != facing) {
                // Mine head is not facing forward
                throw new RNSAssemblyException("wrong_minehead_direction");
            }

            if (bs.is(RNSBlocks.MINE_HEAD.get())) {
                // Multiple mine heads found
                if (mineHeadPos != null) throw new RNSAssemblyException("not_one_minehead");
                // Local position of the mine head differs from origin on an axis the contraption is not facing
                if (Utils.dot(Utils.normalVecFlip(facing, true), toLocalPos(pos)) != 0) {
                    throw new RNSAssemblyException("minehead_not_aligned");
                }
                mineHeadPos = pos;
            }
            if (bs.is(RNSBlockTags.RESONATOR_ATTACHMENTS)) {
                resonatorCount++;
            }
            if (bs.is(RNSBlockTags.RES_BUFFER_ATTACHMENTS)) {
                bufferCount++;
            }
        }
        return super.moveBlock(world, forcedDirection, frontier, visited);
    }

    @Override
    public CompoundTag writeNBT(HolderLookup.Provider registries, boolean spawnPacket) {
        var tag = super.writeNBT(registries, spawnPacket);
        if (mineHeadPos != null) {
            var rnsTag =  new CompoundTag();
            rnsTag.putLong("mine_head_pos", mineHeadPos.asLong());
            rnsTag.putInt("resonator_count", resonatorCount);
            rnsTag.putInt("buffer_count", bufferCount);
            tag.put(CreateRNS.ID,  rnsTag);
        }
        return tag;
    }

    @Override
    public void readNBT(Level world, CompoundTag tag, boolean spawnData) {
        super.readNBT(world, tag, spawnData);
        if (!tag.contains(CreateRNS.ID)) return;
        var rnsTag = tag.getCompound(CreateRNS.ID);
        mineHeadPos = BlockPos.of(rnsTag.getLong("mine_head_pos"));
        resonatorCount = rnsTag.getInt("resonator_count");
        bufferCount = rnsTag.getInt("buffer_count");
    }

    private void setMineHeadBlocksAssembled(boolean assembled) {
        blocks.replaceAll((pos, info) -> {
            var state = info.state();
            if (!state.is(RNSBlocks.MINE_HEAD.get()) && !state.is(RNSBlocks.MINE_HEAD_PART.get())) return info;
            return new StructureBlockInfo(pos, state.setValue(MineHeadBlock.ASSEMBLED, assembled), info.nbt());
        });
    }
}
