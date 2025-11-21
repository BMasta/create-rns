package com.bmaster.createrns.content.deposit.mining;

import com.simibubi.create.foundation.sound.SoundScapes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

public class MiningEffectsGenerator {
    public final Level level;
    public BlockPos sourcePosition;
    protected List<BlockParticleOption> particleOptions = null;

    public MiningEffectsGenerator(Level level) {
        this.level = level;
    }

    public MiningEffectsGenerator(Level level, BlockPos sourcePosition) {
        this.level = level;
        this.sourcePosition = sourcePosition;
    }

    public void setSourcePosition(@Nullable BlockPos sourcePosition) {
        this.sourcePosition = sourcePosition;
    }

    public void setParticles(Set<BlockPos> minedBlocks) {
        if (!level.isClientSide) return;
        particleOptions = minedBlocks.stream()
                .map(bp -> new BlockParticleOption(ParticleTypes.BLOCK, level.getBlockState(bp)))
                .toList();
    }

    public void tickSoundScape(float speed) {
        if (sourcePosition == null) return;
        float pitch = Mth.clamp((Math.abs(speed) / 256f) + .45f, .85f, 1f);
        SoundScapes.play(SoundScapes.AmbienceGroup.CRUSHING, sourcePosition, pitch);
    }

    public void spawnParticles() {
        if (level == null || !level.isClientSide || sourcePosition == null || particleOptions == null) return;

        var r = level.random;
        ParticleOptions selectedParticle = particleOptions.get(r.nextInt(0, particleOptions.size()));

        for (int i = 0; i < 2; i++)
            level.addParticle(selectedParticle,
                    sourcePosition.getX() + r.nextFloat(),
                    sourcePosition.getY() - 0.5 + r.nextFloat(),
                    sourcePosition.getZ() + r.nextFloat(),
                    0, 0, 0);
    }
}
