package com.bmaster.createrns.content.deposit.mining;

import com.bmaster.createrns.RNSSoundEvents;
import com.bmaster.createrns.content.deposit.mining.contraption.MinerBearingBlock;
import com.bmaster.createrns.content.deposit.mining.contraption.MinerBearingBlockEntity;
import com.bmaster.createrns.content.deposit.mining.contraption.attachment.minehead.MineHeadSize;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@OnlyIn(Dist.CLIENT)
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class MinerEffectsGenerator {
    protected static final int MAX_SOUND_DISTANCE = 128;
    protected static final int SOUND_SEQUENCE_INTERVAL = 4;
    protected static final Object2ObjectOpenHashMap<ResourceKey<Level>, ObjectOpenHashSet<BlockPos>> MINERS =
            new Object2ObjectOpenHashMap<>();
    protected static int lastPlayed = 0;

    @SuppressWarnings("DataFlowIssue")
    public static void globalTick() {
        lastPlayed++;

        var instance = Minecraft.getInstance();
        var p = instance.player;
        if (instance.isPaused() || p == null || lastPlayed < SOUND_SEQUENCE_INTERVAL) return;

        var miners = MINERS.get(p.level().dimension());
        if (miners == null) return;
        miners.stream()
                .filter(bp -> bp.distManhattan(p.blockPosition()) <= MAX_SOUND_DISTANCE)
                .map(bp -> {
                    var be = instance.level.getBlockEntity(bp);
                    if (!(be instanceof MinerBearingBlockEntity miner)) return null;
                    return miner;
                })
                .filter(miner -> {
                    if (miner == null) return false;
                    if (miner.miningBehaviour.equipment == null) return false;
                    if (miner.miningBehaviour.process == null) return false;
                    return miner.miningBehaviour.isMining();
                })
                .min(Comparator.comparing(miner ->
                        miner.miningBehaviour.equipment.mineHeadPos.distSqr(p.blockPosition()))
                )
                .ifPresent(miner -> {
                    var mineHeadPos = miner.miningBehaviour.equipment.mineHeadPos;
                    var crsList = miner.miningBehaviour.process.getLastSatisfiedCRSes();
                    float pitch = 0.5f + Math.min(1, Math.abs(miner.getTheoreticalSpeed()) / 256f) / 2;
                    RNSSoundEvents.MINING.playClient(p.level(), mineHeadPos, 1, pitch, false);
                    if (miner.miningBehaviour.equipment.mineHeadSize == MineHeadSize.HUGE) {
                        RNSSoundEvents.MINING_LARGE_HEAD_ACCENT.playClient(p.level(), mineHeadPos, 1, pitch, false);
                    }
                    crsList.stream()
                            .map(crs -> crs.value().sound)
                            .filter(Objects::nonNull)
                            .distinct()
                            .forEach(s -> miner.getLevel().playLocalSound(
                                    mineHeadPos, s, SoundSource.AMBIENT, 1, pitch, false));
                });
        lastPlayed = 0;
    }

    public static void clearState() {
        MINERS.clear();
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
        var mineHeadPos = be.miningBehaviour.equipment.mineHeadPos.getCenter();
        var MineHeadFacing = be.getBlockState().getValue(MinerBearingBlock.FACING);
        var selectedParticle = particleOptions.get(r.nextInt(0, particleOptions.size()));

        assert be.miningBehaviour.claimedDepositBlocks != null;
        float mult = Math.min(1f, be.miningBehaviour.claimedDepositBlocks.size() / 75f);

        for (int i = 0; i < Math.round(1 + mult * 5); i++) {
            level.addParticle(selectedParticle,
                    mineHeadPos.x + MineHeadFacing.getStepX() * 0.5 * (1 - r.nextFloat() * mult),
                    mineHeadPos.y + MineHeadFacing.getStepY() * 0.5 * (1 - r.nextFloat() * mult),
                    mineHeadPos.z + MineHeadFacing.getStepZ() * 0.5 * (1 - r.nextFloat() * mult),
                    0, 0, 0);
        }
    }

    public void uninitialize() {
        var l = be.getLevel();
        if (l == null) return;
        var dim = l.dimension();
        var dimMiners = MINERS.get(dim);
        if (dimMiners == null) return;
        dimMiners.remove(be.getBlockPos());
        if (dimMiners.isEmpty()) MINERS.remove(dim);
    }

    protected void refreshSound() {
        if (be.miningBehaviour.process == null || be.miningBehaviour.equipment == null) return;
        var l = be.getLevel();
        if (l == null) return;
        MINERS.computeIfAbsent(l.dimension(), ignored -> new ObjectOpenHashSet<>()).add(be.getBlockPos());
    }

    protected void refreshParticles() {
        var claimedBlocks = be.miningBehaviour.getClaimedDepositBlocks();
        if (claimedBlocks == null) return;
        particleOptions = claimedBlocks.stream()
                .map(bp -> new BlockParticleOption(ParticleTypes.BLOCK, level.getBlockState(bp)))
                .toList();
    }
}
