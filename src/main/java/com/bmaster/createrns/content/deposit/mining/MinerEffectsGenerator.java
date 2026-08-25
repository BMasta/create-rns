package com.bmaster.createrns.content.deposit.mining;

import com.bmaster.createrns.RNSSoundEvents;
import com.bmaster.createrns.content.deposit.mining.contraption.MinerBearingBlock;
import com.bmaster.createrns.content.deposit.mining.contraption.MinerBearingBlockEntity;
import com.bmaster.createrns.content.deposit.mining.contraption.attachment.minehead.MineHeadSize;
import com.bmaster.createrns.content.deposit.operating.sublevel.OperatingSublevelAdapterHolder;
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
        var l = instance.level;
        var p = instance.player;
        if (instance.isPaused() || l == null || p == null || lastPlayed < SOUND_SEQUENCE_INTERVAL) return;
        var adapter = OperatingSublevelAdapterHolder.getAdapter();

        var miners = MINERS.get(p.level().dimension());
        if (miners == null) return;
        miners.stream()
                .filter(bp -> adapter.distManhattan(l, bp, p.blockPosition()) <= MAX_SOUND_DISTANCE)
                .map(bp -> {
                    var be = l.getBlockEntity(bp);
                    if (!(be instanceof MinerBearingBlockEntity miner)) return null;
                    return miner;
                })
                .filter(miner -> {
                    if (miner == null) return false;
                    var process = miner.miningBehaviour.getProcess();
                    if (process == null || miner.miningBehaviour.equipment == null) return false;
                    return miner.miningBehaviour.isMining();
                })
                .min(Comparator.comparing(miner -> adapter.distSqr(
                        l, miner.miningBehaviour.equipment.mineHeadTipPos, p.blockPosition()))
                )
                .ifPresent(miner -> {
                    var mineHeadPos = miner.miningBehaviour.equipment.mineHeadTipPos;
                    var crsList = miner.miningBehaviour.getProcess().getLastSatisfiedCRSes();
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
        if (!registeredSound && be.miningBehaviour.getProcess() != null) {
            refreshSound();
            registeredSound = true;
        }

        if (particleOptions == null) refreshParticles();
        if (particleOptions.isEmpty()) return;

        var r = level.random;
        var mineHeadPos = be.miningBehaviour.equipment.mineHeadTipPos.getCenter();
        var mineHeadSize = be.miningBehaviour.equipment.mineHeadSize;
        var mineHeadFacing = be.getBlockState().getValue(MinerBearingBlock.FACING);
        var selectedParticle = particleOptions.get(r.nextInt(0, particleOptions.size()));

        double forwardOffset = 0.5;
        if (be.miningBehaviour.isOperatingCrossSublevel()) {
            var target = be.miningBehaviour.getOperatingStart();
            if (target != null) {
                double maxOffset = 0.25 * mineHeadSize.modelScale + 0.5;
                forwardOffset = OperatingSublevelAdapterHolder.getAdapter()
                        .getCrossSublevelBlockHitDistance(level, be.miningBehaviour.equipment.mineHeadTipPos,
                                be.miningBehaviour.getOperatingDirection(), target, maxOffset)
                        .orElse(maxOffset);
            }
        }

        var claimedBlocks = be.miningBehaviour.getClaimedDepositBlocks();
        if (claimedBlocks == null) return;
        float mult = Math.min(1f, claimedBlocks.size() / 75f);

        for (int i = 0; i < mineHeadSize.claimBonus * 2 + 1; i++) {
            double angle = r.nextDouble() * Math.PI * 2;
            double uOffset = Math.cos(angle) * 0.24f * mineHeadSize.modelScale;
            double vOffset = Math.sin(angle) * 0.24f * mineHeadSize.modelScale;
            double particleX = mineHeadPos.x + mineHeadFacing.getStepX() * forwardOffset;
            double particleY = mineHeadPos.y + mineHeadFacing.getStepY() * forwardOffset;
            double particleZ = mineHeadPos.z + mineHeadFacing.getStepZ() * forwardOffset;

            switch (mineHeadFacing.getAxis()) {
                case X -> {
                    particleY += uOffset;
                    particleZ += vOffset;
                }
                case Y -> {
                    particleX += uOffset;
                    particleZ += vOffset;
                }
                case Z -> {
                    particleX += uOffset;
                    particleY += vOffset;
                }
            }

            level.addParticle(selectedParticle, particleX, particleY, particleZ, 0, 0, 0);
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
        if (be.miningBehaviour.getProcess() == null || be.miningBehaviour.equipment == null) return;
        var l = be.getLevel();
        if (l == null) return;
        MINERS.computeIfAbsent(l.dimension(), ignored -> new ObjectOpenHashSet<>()).add(be.getBlockPos());
    }

    protected void refreshParticles() {
        var claimedBlocks = be.miningBehaviour.getClaimedDepositBlocks();
        if (claimedBlocks == null) {
            particleOptions = List.of();
            return;
        }
        particleOptions = claimedBlocks.stream()
                .map(bp -> new BlockParticleOption(ParticleTypes.BLOCK, level.getBlockState(bp)))
                .toList();
    }
}
