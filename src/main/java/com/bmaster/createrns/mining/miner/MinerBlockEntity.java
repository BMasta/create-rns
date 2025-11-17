package com.bmaster.createrns.mining.miner;

import com.bmaster.createrns.RNSContent;
import com.bmaster.createrns.mining.IHaveMiningGoggleInformation;
import com.bmaster.createrns.mining.MiningBehaviour;
import com.bmaster.createrns.mining.MiningItemHandler;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.inventory.InvManipulationBehaviour;
import com.simibubi.create.foundation.item.ItemHelper;
import com.simibubi.create.foundation.sound.SoundScapes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;

@ParametersAreNonnullByDefault
public class MinerBlockEntity extends KineticBlockEntity implements IHaveMiningGoggleInformation {
    protected final MiningItemHandler inventory = new MiningItemHandler(this);
    private List<BlockState> particleOptions = null;

    public MinerBlockEntity(BlockPos pos, BlockState state) {
        super(RNSContent.MINER_BE.get(), pos, state);
    }

    @Override
    public String getLangIdentifier() {
        return "miner";
    }

    @Override
    public KineticBlockEntity getTargetBlockEntity() {
        return this;
    }

    public MiningItemHandler getItemHandler(@Nullable Direction side) {
        return inventory;
    }

    public boolean isMining() {
        return getBehaviour(MiningBehaviour.TYPE).isMining();
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null) return;

        if (level.isClientSide) {
            if (isMining()) spawnParticles();
        } else {
            tryEjectUp();
        }
    }

    @Override
    public void tickAudio() {
        if (!isMining()) return;
        float speed = Math.abs(getSpeed());

        float pitch = Mth.clamp((speed / 256f) + .45f, .85f, 1f);
        SoundScapes.play(SoundScapes.AmbienceGroup.CRUSHING, worldPosition, pitch);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        behaviours.add(new MiningBehaviour(this));
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider p, boolean clientPacket) {
        super.write(tag, p, clientPacket);
        tag.put("inventory", inventory.serializeNBT(p));
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider p, boolean clientPacket) {
        super.read(tag, p, clientPacket);
        inventory.deserializeNBT(p, tag.getCompound("inventory"));

        // Clients get their claimed blocks from server updates
        if (clientPacket && level != null) {
            particleOptions = getBehaviour(MiningBehaviour.TYPE).getClaimedDepositBlocks().stream()
                    .map(bp -> level.getBlockState(bp))
                    .toList();
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        ItemHelper.dropContents(level, worldPosition, inventory);
    }

    /// Kinetic BE already implements IHaveGoggleInformation, so an explicit override is needed
    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        return IHaveMiningGoggleInformation.super.addToGoggleTooltip(tooltip, isPlayerSneaking);
    }

    protected void tryEjectUp() {
        if (level == null) return;

        var above = worldPosition.above();
        var targetInv = level.getCapability(Capabilities.ItemHandler.BLOCK, above, Direction.DOWN);
        if (targetInv == null) {
            InvManipulationBehaviour inserter = level.getBlockEntity(above) == null ? null
                    : BlockEntityBehaviour.get(level, above, InvManipulationBehaviour.TYPE);
            targetInv = (inserter == null) ? null : inserter.getInventory();
        }
        if (targetInv == null) return;

        var extracted = inventory.extractFirstAvailableItem(true);
        if (extracted.isEmpty()) return;
        for (int i = 0; i < targetInv.getSlots(); ++i) {
            var remaining = targetInv.insertItem(i, extracted, true);
            // We extract a single item, so insertion is always atomic
            if (remaining.isEmpty()) {
                extracted = inventory.extractFirstAvailableItem(false);
                assert !extracted.isEmpty();
                remaining = targetInv.insertItem(i, extracted, false);
                assert remaining.isEmpty();
                return;
            }
        }
    }

    protected void spawnParticles() {
        if (level == null) return;
        if (particleOptions == null) return;

        var r = level.random;
        BlockState selectedParticle = particleOptions.get(r.nextInt(0, particleOptions.size()));
        ParticleOptions particleData = new BlockParticleOption(ParticleTypes.BLOCK, selectedParticle);

        for (int i = 0; i < 2; i++)
            level.addParticle(particleData,
                    worldPosition.getX() + r.nextFloat(),
                    worldPosition.getY() - 0.5 + r.nextFloat(),
                    worldPosition.getZ() + r.nextFloat(),
                    0, 0, 0);
    }
}
