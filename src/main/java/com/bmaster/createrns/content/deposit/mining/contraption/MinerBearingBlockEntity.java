package com.bmaster.createrns.content.deposit.mining.contraption;

import com.bmaster.createrns.content.deposit.mining.IHaveAdaptiveGoggleInformation;
import com.bmaster.createrns.util.GoggleTooltipModifiers;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.contraptions.AssemblyException;
import com.simibubi.create.content.contraptions.ControlledContraptionEntity;
import com.simibubi.create.content.contraptions.bearing.BearingBlock;
import com.simibubi.create.content.contraptions.bearing.BearingContraption;
import com.simibubi.create.content.contraptions.bearing.MechanicalBearingBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.function.BiFunction;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class MinerBearingBlockEntity extends MechanicalBearingBlockEntity implements IHaveAdaptiveGoggleInformation {
    public ContraptionMiningBehaviour miningBehaviour;

    public MinerBearingBlockEntity(BlockEntityType<MinerBearingBlockEntity> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }


    @Override
    public void invalidate() {
        super.invalidate();
    }

    public void assembleNextTick() {
        assembleNextTick = true;
    }

    @Override
    public void assemble() {
        assert level != null;
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
    }

    @Override
    public KineticBlockEntity getTargetBlockEntity() {
        return this;
    }

    @Override
    public List<BiFunction<Context, List<Component>, Boolean>> getPrimarySections() {
        return List.of(GoggleTooltipModifiers::addMinerInfoToGoggleTooltip);
    }

    @Override
    public List<BiFunction<Context, List<Component>, Boolean>> getSecondarySections() {
        return List.of(GoggleTooltipModifiers::addRatesToGoggleTooltip, GoggleTooltipModifiers::addUsesToGoggleTooltip);
    }

    @Override
    public List<BiFunction<Context, List<Component>, Boolean>> getMandatoryBottomSections() {
        return List.of(GoggleTooltipModifiers::addKineticsToGoggleTooltip);
    }

    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        return IHaveAdaptiveGoggleInformation.super.addToGoggleTooltip(tooltip, isPlayerSneaking);
    }
}


