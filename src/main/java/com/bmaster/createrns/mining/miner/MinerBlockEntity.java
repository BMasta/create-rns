package com.bmaster.createrns.mining.miner;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.RNSContent;
import com.bmaster.createrns.infrastructure.ServerConfig;
import com.bmaster.createrns.mining.*;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.inventory.InvManipulationBehaviour;
import com.simibubi.create.foundation.sound.SoundScapes;
import com.simibubi.create.foundation.utility.CreateLang;
import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;

import java.util.*;
import java.util.stream.Collectors;

public class MinerBlockEntity extends MiningBlockEntity {
    private MinerSpec spec = null;
    private List<BlockState> particleOptions = null;
    private int cachedProgress = 0;

    public MinerBlockEntity(BlockPos pos, BlockState state) {
        super(RNSContent.MINER_BE.get(), pos, state);
    }

    @Override
    public int getMiningAreaRadius() {
        if (spec == null) tryInitSpec();
        return Objects.requireNonNull(spec).miningArea().radius();
    }

    @Override
    public int getMiningAreaHeight() {
        if (spec == null) tryInitSpec();
        return Objects.requireNonNull(spec).miningArea().height();
    }

    @Override
    public int getMiningAreaYOffset() {
        if (spec == null) tryInitSpec();
        return Objects.requireNonNull(spec).miningArea().verticalOffset();
    }

    @Override
    public int getTier() {
        if (spec == null) tryInitSpec();
        return Objects.requireNonNull(spec).tier();
    }

    /// Required for visual and renderer who may try to get the tier before the block entity is loaded
    public int getTierSafe() {
        if (spec == null) tryInitSpec();
        return (spec != null) ? spec.tier() : 0;
    }

    @Override
    public int getBaseProgress() {
        // From server config
        if ((getTier() == 1) && (ServerConfig.minerMk1BaseProgress != 0)) return ServerConfig.minerMk1BaseProgress;
        if ((getTier() == 2) && (ServerConfig.minerMk2BaseProgress != 0)) return ServerConfig.minerMk2BaseProgress;

        // Or from miner spec
        if (cachedProgress == 0) {
            if (spec == null) tryInitSpec();
            cachedProgress = 256 * 60 * SharedConstants.TICKS_PER_MINUTE / (int) Objects.requireNonNull(spec).minesPerHour();
        }
        return cachedProgress;
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
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        boolean added = false;

        // Try adding desired section
        if (!isPlayerSneaking) added = addInventoryToGoggleTooltip(tooltip, true);
        else added = addRatesToGoggleTooltip(tooltip, true);

        // If unsuccessful, try adding the less desired
        if (!added) {
            if (!isPlayerSneaking) added = addRatesToGoggleTooltip(tooltip, true);
            else added = addInventoryToGoggleTooltip(tooltip, true);
        }

        // Add kinetics regardless
        added = addKineticsToGoggleTooltip(tooltip, !added);

        return added;
    }

    @Override
    protected void addStressImpactStats(List<Component> tooltip, float stressAtBase) {
        super.addStressImpactStats(tooltip, stressAtBase);
    }

    @SuppressWarnings("SameParameterValue")
    protected boolean addInventoryToGoggleTooltip(List<Component> tooltip, boolean isMainSection) {
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
    protected boolean addRatesToGoggleTooltip(List<Component> tooltip, boolean isMainSection) {
        if (process == null || reservedDepositBlocks.isEmpty()) return false;

        if (isMainSection) {
            new LangBuilder(CreateRNS.MOD_ID).translate("miner.production_rates").forGoggles(tooltip);
        } else {
            // Newline between sections
            new LangBuilder(CreateRNS.MOD_ID).space().forGoggles(tooltip);
        }

        var rates = process.getEstimatedRates(getCurrentProgressIncrement());
        for (var e : rates.object2FloatEntrySet()) {
            new LangBuilder(CreateRNS.MOD_ID)
                    .add(e.getKey().getDescription().copy()
                            .append(": ")
                            .withStyle(ChatFormatting.GRAY))
                    .add(Component.literal(String.format(java.util.Locale.ROOT, "%.1f", e.getFloatValue()))
                            .append(Component.translatable("%s.miner.per_hour".formatted(CreateRNS.MOD_ID)))
                            .withStyle(ChatFormatting.GREEN))
                    .forGoggles(tooltip, 1);
        }
        return true;
    }

    @SuppressWarnings("UnusedReturnValue")
    protected boolean addKineticsToGoggleTooltip(List<Component> tooltip, boolean isMainSection) {
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


    protected void tryInitSpec() {
        if (level == null) return;
        spec = MinerSpecLookup.get(level.registryAccess(), (MinerBlock) getBlockState().getBlock());
    }
}
