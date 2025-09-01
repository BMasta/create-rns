package com.bmaster.createrns.mining.miner;

import com.bmaster.createrns.RNSContent;
import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.mining.*;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.foundation.sound.SoundScapes;
import com.simibubi.create.foundation.utility.CreateLang;
import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

public abstract class MinerBlockEntity extends MiningBlockEntity {
    private List<BlockState> particleOptions = null;

    public MinerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public int getCurrentProgressIncrement() {
        return (int) Math.abs(getSpeed());
    }

    @Override
    public boolean isMining() {
        if (level == null || process == null) return false;
        return !reservedDepositBlocks.isEmpty() && isSpeedRequirementFulfilled();
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null) return;

        if (isMining() && level.isClientSide) {
                spawnParticles();
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
    protected void addStressImpactStats(List<Component> tooltip, float stressAtBase) {
        super.addStressImpactStats(tooltip, stressAtBase);
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        boolean added = false;

        if (process != null && isMining()) {
            // Try adding desired section
            if (!isPlayerSneaking) added = addInventoryToGoggleTooltip(tooltip, true);
            else added = addRatesToGoggleTooltip(tooltip, true);

            // If unsuccessful, try adding the less desired
            if (!added) {
                if (!isPlayerSneaking) added = addRatesToGoggleTooltip(tooltip, true);
                else added = addInventoryToGoggleTooltip(tooltip, true);
            }
        }

        added = addKineticsToGoggleTooltip(tooltip, !added);

        return added;
    }

    protected void spawnParticles() {
        if (level == null) return;
        if (particleOptions == null) {
            particleOptions = reservedDepositBlocks.stream()
                    .map(bp -> level.getBlockState(bp))
                    .toList();
        }

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

    @SuppressWarnings("SameParameterValue")
    private boolean addInventoryToGoggleTooltip(List<Component> tooltip, boolean isMainSection) {
        if (inventory.isEmpty()) return false;

        if (isMainSection) {
            new LangBuilder(CreateRNS.MOD_ID).translate("miner.contents").forGoggles(tooltip);
        } else {
            // Newline between sections
            new LangBuilder(CreateRNS.MOD_ID).space().forGoggles(tooltip);
        }

        for (int slot = 0; slot < inventory.getSlots(); ++slot) {
            var is = inventory.getStackInSlot(slot);
            if (is.equals(ItemStack.EMPTY)) continue;
            new LangBuilder(CreateRNS.MOD_ID)
                    .add(is.getHoverName().copy().withStyle(ChatFormatting.GRAY))
                    .add(Component.literal(" x" + is.getCount()).withStyle(ChatFormatting.GREEN))
                    .forGoggles(tooltip, 1);
        }

        return true;
    }

    @SuppressWarnings("SameParameterValue")
    private boolean addRatesToGoggleTooltip(List<Component> tooltip, boolean isMainSection) {
        if (reservedDepositBlocks.isEmpty()) return false;

        if (isMainSection) {
            new LangBuilder(CreateRNS.MOD_ID).translate("miner.production_rates").forGoggles(tooltip);
        } else {
            // Newline between sections
            new LangBuilder(CreateRNS.MOD_ID).space().forGoggles(tooltip);
        }

        var stpListSorted = process.innerProcesses.stream()
                .sorted(Comparator.comparingInt(p -> p.maxProgress))
                .toList();

        for (var p : stpListSorted) {
            var progressPerHour = 60 * SharedConstants.TICKS_PER_MINUTE * getCurrentProgressIncrement();
            var itemsPerHour = (float) ((long) progressPerHour * 10 / p.maxProgress) / 10;
            new LangBuilder(CreateRNS.MOD_ID)
                    .add(p.yield.getDescription().copy()
                            .append(": ")
                            .withStyle(ChatFormatting.GRAY))
                    .add(Component.literal(Float.toString(itemsPerHour))
                            .append(Component.translatable("%s.miner.per_hour".formatted(CreateRNS.MOD_ID)))
                            .withStyle(ChatFormatting.GREEN))
                    .forGoggles(tooltip, 1);
        }
        return true;
    }

    @SuppressWarnings("UnusedReturnValue")
    private boolean addKineticsToGoggleTooltip(List<Component> tooltip, boolean isMainSection) {
        float stressAtBase = 0f;
        if (IRotate.StressImpact.isEnabled()) stressAtBase = calculateStressApplied();
        if (Mth.equal(stressAtBase, 0)) return false;

        if (isMainSection) {
            CreateLang.translate("gui.goggles.kinetic_stats").forGoggles(tooltip);
        } else {
            // Newline between sections
            new LangBuilder(CreateRNS.MOD_ID).space().forGoggles(tooltip);
        }
        addStressImpactStats(tooltip, calculateStressApplied());
        return true;
    }
}
