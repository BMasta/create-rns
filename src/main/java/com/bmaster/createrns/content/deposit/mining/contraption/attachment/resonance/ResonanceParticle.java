package com.bmaster.createrns.content.deposit.mining.contraption.attachment.resonance;

import com.simibubi.create.content.equipment.bell.BasicParticleData;
import com.simibubi.create.content.equipment.bell.CustomRotationParticle;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Supplier;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ResonanceParticle extends CustomRotationParticle {
    private final SpriteSet animatedSprite;
    protected final float rotation;

    public ResonanceParticle(ClientLevel worldIn, double x, double y, double z, double vx, double vy, double vz, SpriteSet spriteSet) {
        super(worldIn, x, y + (vy < 0 ? -1 : 1), z, spriteSet, 0);
        this.animatedSprite = spriteSet;
        this.quadSize = 0.2f;
        this.loopLength = 10;
        this.lifetime = 10;
//        this.stoppedByCollision = true; // disable movement
        this.rotation = (float) (worldIn.random.nextFloat() * 2 * Math.PI);

        this.setSize(this.quadSize, this.quadSize);
        this.setSpriteFromAge(spriteSet);
    }

    @Override
    public void tick() {
        setSpriteFromAge(animatedSprite);
        if (age++ >= lifetime)
            remove();
    }

    @Override
    public Quaternionf getCustomRotation(Camera camera, float partialTicks) {
        return new Quaternionf().rotateY(-camera.getYRot() * Mth.DEG_TO_RAD)
                .mul(new Quaternionf().rotateZ(rotation));
    }


    public static class Data extends BasicParticleData<ResonanceParticle> {
        protected final Supplier<ParticleType<BasicParticleData<ResonanceParticle>>> typeSupplier;

        public Data(Supplier<ParticleType<BasicParticleData<ResonanceParticle>>> typeSupplier) {
            this.typeSupplier = typeSupplier;
        }

        @Override
        public IBasicParticleFactory<ResonanceParticle> getBasicFactory() {
            return ResonanceParticle::new;
        }

        @Override
        public ParticleType<BasicParticleData<ResonanceParticle>> getType() {
            return typeSupplier.get();
        }
    }
}
