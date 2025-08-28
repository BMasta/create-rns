package com.bmaster.createrns.event;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.RNSDatapacks;
import com.bmaster.createrns.capability.depositindex.DepositSpec;
import com.bmaster.createrns.capability.depositindex.IDepositIndex;
import com.bmaster.createrns.util.GeneratedDataPackResources;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DataPackRegistryEvent;

@Mod.EventBusSubscriber(modid = CreateRNS.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEvents {
    @SubscribeEvent
    public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.register(IDepositIndex.class);
    }

    @SubscribeEvent
    public static void onNewRegistry(DataPackRegistryEvent.NewRegistry e) {
        CreateRNS.LOGGER.info("Adding DepositSpec registry");
        e.dataPackRegistry(DepositSpec.REGISTRY_KEY, DepositSpec.CODEC, DepositSpec.CODEC);
    }

    @SubscribeEvent
    public static void onAddPackFinders(net.minecraftforge.event.AddPackFindersEvent e) {
        if (e.getPackType() != PackType.SERVER_DATA) return;
        e.addRepositorySource(consumer -> consumer.accept(RNSDatapacks.DEPOSITS_DATAPACK));
    }
}
