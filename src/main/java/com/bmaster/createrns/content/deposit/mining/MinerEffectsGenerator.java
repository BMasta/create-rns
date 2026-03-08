package com.bmaster.createrns.content.deposit.mining;

import com.bmaster.createrns.RNSSoundEvents;
import com.bmaster.createrns.content.deposit.mining.contraption.MinerBearingBlock;
import com.bmaster.createrns.content.deposit.mining.contraption.MinerBearingBlockEntity;
import com.bmaster.createrns.util.Utils;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class MinerEffectsGenerator {
    public enum SoundModifier {
        RESONANCE
    }

    protected static final int SOUND_SEQUENCE_INTERVAL = 4;
    protected static final Object2ObjectOpenHashMap<MinerBearingBlockEntity, EnumSet<SoundModifier>> miners = new Object2ObjectOpenHashMap<>();
    protected static int lastPlayed = 0;

    @SuppressWarnings("DataFlowIssue")
    public static void globalTick() {
        lastPlayed++;

        var instance = Minecraft.getInstance();
        var p = instance.player;
        if (instance.isPaused() || p == null || lastPlayed < SOUND_SEQUENCE_INTERVAL) return;

        miners.object2ObjectEntrySet().stream()
                .filter(e -> {
                    var miner = e.getKey();
                    if (miner.miningBehaviour.equipment == null) return false;
                    return miner.miningBehaviour.isMining();
                })
                .min(Comparator.comparing(e ->
                        e.getKey().miningBehaviour.equipment.drillHeadPos.distSqr(p.blockPosition())))
                .ifPresent(e -> {
                    var miner = e.getKey();
                    var drillPos = miner.miningBehaviour.equipment.drillHeadPos;
                    var modifiers = e.getValue();
                    float pitch = 0.5f + Math.min(1, Math.abs(miner.getTheoreticalSpeed()) / 256f) / 2;
                    RNSSoundEvents.MINING.playClient(p.level(), drillPos, 1, pitch, false);
                    if (modifiers.contains(SoundModifier.RESONANCE)) {
                        RNSSoundEvents.MINING_RESONANCE_ACCENT.playClient(p.level(), drillPos, 1, pitch, false);
                    }
                });
        lastPlayed = 0;
    }

    public static void clearState() {
        miners.clear();
    }

    protected final Level level;
    protected final MinerBearingBlockEntity be;

    protected boolean registeredSound = false;
    protected List<BlockParticleOption> particleOptions = null;

    public MinerEffectsGenerator(MinerBearingBlockEntity be) {
        this.level = be.getLevel();
        assert level != null && level.isClientSide;
        this.be = be;
    }

    public void refresh() {
        refreshSound();
        refreshParticles();
    }

    public void tick() {
        if (be.miningBehaviour.equipment == null) return;
        // Add miner to sound producers
        if (!registeredSound && be.miningBehaviour.process != null) {
            refreshSound();
            registeredSound = true;
        }

        if (particleOptions == null) refreshParticles();
        if (particleOptions.isEmpty()) return;

        var r = level.random;
        var drillPos = be.miningBehaviour.equipment.drillHeadPos.getCenter();
        var drillFacing = be.getBlockState().getValue(MinerBearingBlock.FACING);
        var selectedParticle = particleOptions.get(r.nextInt(0, particleOptions.size()));

        assert be.miningBehaviour.claimedDepositBlocks != null;
        float mult = Math.min(1f, be.miningBehaviour.claimedDepositBlocks.size() / 75f);

        var drillFacingFlipped = Utils.normalVecFlip(drillFacing, true);

        for (int i = 0; i < Math.round(1 + mult * 5); i++) {
            level.addParticle(selectedParticle,
                    drillPos.x + drillFacing.getStepX() * 0.5 * (1 - r.nextFloat() * mult),
                    drillPos.y + drillFacing.getStepY() * 0.5 * (1 - r.nextFloat() * mult),
                    drillPos.z + drillFacing.getStepZ() * 0.5 * (1 - r.nextFloat() * mult),
                    0, 0, 0);
        }
    }

    public void uninitialize() {
        miners.remove(be);
    }

    protected void refreshSound() {
        if (be.miningBehaviour.process == null || be.miningBehaviour.equipment == null) return;
        var modifiers = EnumSet.noneOf(SoundModifier.class);

        if (be.miningBehaviour.process.isResonanceActive()) modifiers.add(SoundModifier.RESONANCE);
        miners.put(be, modifiers);
    }

    protected void refreshParticles() {
        particleOptions = be.miningBehaviour.getClaimedDepositBlocks().stream()
                .map(bp -> new BlockParticleOption(ParticleTypes.BLOCK, level.getBlockState(bp)))
                .toList();
    }
}
