package com.bmaster.createrns;

import com.bmaster.createrns.content.deposit.mining.contraption.attachment.resonance.ResonanceParticle;
import com.simibubi.create.content.equipment.bell.BasicParticleData;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import javax.annotation.ParametersAreNonnullByDefault;

@SuppressWarnings("Convert2MethodRef")
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class RNSParticleTypes {
    protected static final DeferredRegister<ParticleType<?>> REGISTER =
            DeferredRegister.create(Registries.PARTICLE_TYPE, CreateRNS.ID);

    public static final BasicParticleData<ResonanceParticle> RESONANCE = new ResonanceParticle.Data(
            () -> RNSParticleTypes.RESONANCE_TYPE.get());

    public static final DeferredHolder<ParticleType<?>, ParticleType<BasicParticleData<ResonanceParticle>>> RESONANCE_TYPE =
            REGISTER.register("resonance", RESONANCE::createType);

    public static final BasicParticleData<ResonanceParticle> SHATTERING_RESONANCE = new ResonanceParticle.Data(
            () -> RNSParticleTypes.SHATTERING_RESONANCE_TYPE.get());

    public static DeferredHolder<ParticleType<?>, ParticleType<BasicParticleData<ResonanceParticle>>> SHATTERING_RESONANCE_TYPE =
            REGISTER.register("shattering_resonance", SHATTERING_RESONANCE::createType);

    public static final BasicParticleData<ResonanceParticle> STABILIZING_RESONANCE = new ResonanceParticle.Data(
            () -> RNSParticleTypes.STABILIZING_RESONANCE_TYPE.get());

    public static DeferredHolder<ParticleType<?>, ParticleType<BasicParticleData<ResonanceParticle>>> STABILIZING_RESONANCE_TYPE =
            REGISTER.register("stabilizing_resonance", STABILIZING_RESONANCE::createType);


    public static void register(IEventBus bus) {
        REGISTER.register(bus);
    }
}
