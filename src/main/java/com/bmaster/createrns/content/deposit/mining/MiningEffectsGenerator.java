package com.bmaster.createrns.content.deposit.mining;

import com.simibubi.create.foundation.sound.SoundScapes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

public class MiningEffectsGenerator {
    public final Level level;
    public final Supplier<BlockPos> srcPositionSup;
    public final Supplier<Direction> srcDirectionSup;
    protected List<BlockParticleOption> particleOptions = null;

    public MiningEffectsGenerator(Level level, Supplier<BlockPos> srcPositionSup, Supplier<Direction> srcDirectionSup) {
        this.level = level;
        this.srcPositionSup = srcPositionSup;
        this.srcDirectionSup = srcDirectionSup;
    }

    public void setParticles(Set<BlockPos> minedBlocks) {
        if (!level.isClientSide) return;
        particleOptions = minedBlocks.stream()
                .map(bp -> new BlockParticleOption(ParticleTypes.BLOCK, level.getBlockState(bp)))
                .toList();
    }

    public void tickSoundScape(float speed) {
        var srcPos = srcPositionSup.get();
        if (srcPos == null) return;
        float pitch = Mth.clamp((Math.abs(speed) / 256f) + .45f, .85f, 1f);
        SoundScapes.play(SoundScapes.AmbienceGroup.CRUSHING, srcPos, pitch);
    }

    public void spawnParticles() {
        if (level == null || !level.isClientSide || particleOptions == null || particleOptions.isEmpty()) return;
        var srcPos = srcPositionSup.get();
        if (srcPos == null) return;
        var srcDir = srcDirectionSup.get();
        if (srcDir == null) return;

        var r = level.random;
        ParticleOptions selectedParticle = particleOptions.get(r.nextInt(0, particleOptions.size()));

        for (int i = 0; i < 2; i++)
            level.addParticle(selectedParticle,
                    srcPos.getX() + 0.5 * srcDir.getStepX() + r.nextFloat(),
                    srcPos.getY() + 0.5 * srcDir.getStepY() + r.nextFloat(),
                    srcPos.getZ() + 0.5 * srcDir.getStepZ() + r.nextFloat(),
                    0, 0, 0);
    }
}
