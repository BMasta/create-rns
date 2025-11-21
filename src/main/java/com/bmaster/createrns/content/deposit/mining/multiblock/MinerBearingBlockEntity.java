package com.bmaster.createrns.content.deposit.mining.multiblock;

import com.bmaster.createrns.content.deposit.mining.IHaveMiningGoggleInformation;
import com.bmaster.createrns.content.deposit.mining.MiningEffectsGenerator;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.contraptions.AssemblyException;
import com.simibubi.create.content.contraptions.ControlledContraptionEntity;
import com.simibubi.create.content.contraptions.bearing.BearingBlock;
import com.simibubi.create.content.contraptions.bearing.BearingContraption;
import com.simibubi.create.content.contraptions.bearing.MechanicalBearingBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class MinerBearingBlockEntity extends MechanicalBearingBlockEntity implements IHaveMiningGoggleInformation {
    protected ContraptionMiningBehaviour miningBehaviour;
    protected MiningEffectsGenerator effects;

    public MinerBearingBlockEntity(BlockEntityType<MinerBearingBlockEntity> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        effects = new MiningEffectsGenerator(level);
    }

    @Override
    public void tick() {
        super.tick();
        if (level.isClientSide && miningBehaviour.isMining()) effects.spawnParticles();
    }

    @Override
    public void tickAudio() {
        super.tickAudio();
        if (level.isClientSide && miningBehaviour.isMining()) effects.tickSoundScape(getSpeed());
    }

    public void assembleNextTick() {
        assembleNextTick = true;
    }

    @Override
    public void assemble() {
        if (!(level.getBlockState(worldPosition).getBlock() instanceof BearingBlock))
            return;

        Direction direction = getBlockState().getValue(BearingBlock.FACING);
        BearingContraption contraption = new MinerContraption(false, direction);

        try {
            if (!contraption.assemble(level, worldPosition)) return;
            lastException = null;
        } catch (AssemblyException e) {
            lastException = e;
            sendData();
            return;
        }

        contraption.removeBlocksFromWorld(level, BlockPos.ZERO);
        movedContraption = ControlledContraptionEntity.create(level, this, contraption);
        BlockPos anchor = worldPosition.relative(direction);
        movedContraption.setPos(anchor.getX(), anchor.getY(), anchor.getZ());
        movedContraption.setRotationAxis(direction.getAxis());
        level.addFreshEntity(movedContraption);

        AllSoundEvents.CONTRAPTION_ASSEMBLE.playOnServer(level, worldPosition);

        running = true;
        angle = 0;
        sendData();
        updateGeneratedRotation();

        if (running) miningBehaviour.refresh();
    }

    @Override
    public void disassemble() {
        super.disassemble();
        if (!running) miningBehaviour.refresh();
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        miningBehaviour = new ContraptionMiningBehaviour(this);
        behaviours.add(miningBehaviour);
    }

    @Override
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(compound, registries, clientPacket);
        if (clientPacket && effects != null) {
            effects.setSourcePosition(miningBehaviour.getDrillHeadAbsPos());
            effects.setParticles(miningBehaviour.getClaimedDepositBlocks());
        }
    }

    @Override
    public KineticBlockEntity getTargetBlockEntity() {
        return this;
    }

    @Override
    public String getLangIdentifier() {
        return "megadrill";
    }

    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        return IHaveMiningGoggleInformation.super.addToGoggleTooltip(tooltip, isPlayerSneaking);
    }

    @Override
    public boolean addInventoryToGoggleTooltip(List<Component> tooltip, boolean isMainSection) {
        return false;
    }
}


