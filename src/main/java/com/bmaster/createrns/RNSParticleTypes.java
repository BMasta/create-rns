package com.bmaster.createrns;

import com.bmaster.createrns.content.deposit.mining.contraption.attachment.resonance.ResonanceParticle;
import com.simibubi.create.content.equipment.bell.BasicParticleData;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

@SuppressWarnings({"Convert2MethodRef", "FunctionalExpressionCanBeFolded"})
public class RNSParticleTypes {
    protected static final DeferredRegister<ParticleType<?>> REGISTER =
            DeferredRegister.create(Registries.PARTICLE_TYPE, CreateRNS.ID);

    public static final BasicParticleData<ResonanceParticle> RESONANCE = new ResonanceParticle.Data(
            () -> RNSParticleTypes.RESONANCE_TYPE.get());

    public static final RegistryObject<ParticleType<BasicParticleData<ResonanceParticle>>> RESONANCE_TYPE =
            REGISTER.register("resonance", RESONANCE::createType);

    public static final BasicParticleData<ResonanceParticle> SHATTERING_RESONANCE = new ResonanceParticle.Data(
            () -> RNSParticleTypes.SHATTERING_RESONANCE_TYPE.get());

    public static final RegistryObject<ParticleType<BasicParticleData<ResonanceParticle>>> SHATTERING_RESONANCE_TYPE =
            REGISTER.register("shattering_resonance", SHATTERING_RESONANCE::createType);

    public static final BasicParticleData<ResonanceParticle> STABILIZING_RESONANCE = new ResonanceParticle.Data(
            () -> RNSParticleTypes.STABILIZING_RESONANCE_TYPE.get());

    public static final RegistryObject<ParticleType<BasicParticleData<ResonanceParticle>>> STABILIZING_RESONANCE_TYPE =
            REGISTER.register("stabilizing_resonance", STABILIZING_RESONANCE::createType);


    public static void register(IEventBus bus) {
        REGISTER.register(bus);
    }
}
