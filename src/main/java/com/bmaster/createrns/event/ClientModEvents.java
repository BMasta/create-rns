package com.bmaster.createrns.event;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.RNSParticleTypes;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CreateRNS.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {
    @SubscribeEvent
    public static void onRegisterParticleProviders(RegisterParticleProvidersEvent event) {
        RNSParticleTypes.RESONANCE.register(RNSParticleTypes.RESONANCE_TYPE.get(), event);
        RNSParticleTypes.STABILIZING_RESONANCE.register(RNSParticleTypes.STABILIZING_RESONANCE_TYPE.get(), event);
        RNSParticleTypes.SHATTERING_RESONANCE.register(RNSParticleTypes.SHATTERING_RESONANCE_TYPE.get(), event);
    }
}
