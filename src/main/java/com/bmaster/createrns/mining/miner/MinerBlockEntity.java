package com.bmaster.createrns.mining.miner;

import com.bmaster.createrns.RNSContent;
import com.bmaster.createrns.mining.MiningBehaviour;
import com.bmaster.createrns.mining.MiningBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.inventory.InvManipulationBehaviour;
import com.simibubi.create.foundation.sound.SoundScapes;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class MinerBlockEntity extends MiningBlockEntity {
    private List<BlockState> particleOptions = null;

    public MinerBlockEntity(BlockPos pos, BlockState state) {
        super(RNSContent.MINER_BE.get(), pos, state);
    }

    @Override
    protected String getLangIdentifier() {
        return "miner";
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
    public void read(CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        if (level == null) return;
        // Clients get their claimed blocks from server updates
        if (clientPacket) {
            particleOptions = getBehaviour(MiningBehaviour.TYPE).getClaimedDepositBlocks().stream()
                    .map(bp -> level.getBlockState(bp))
                    .toList();
        }
    }

    protected void tryEjectUp() {
        if (level == null) return;

        BlockEntity be = level.getBlockEntity(worldPosition.above());
        InvManipulationBehaviour inserter =
                be == null ? null : BlockEntityBehaviour.get(level, be.getBlockPos(), InvManipulationBehaviour.TYPE);
        @SuppressWarnings("DataFlowIssue")
        var targetInv = be == null ? null
                : be.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.DOWN)
                .orElse(inserter == null ? null : inserter.getInventory());

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
