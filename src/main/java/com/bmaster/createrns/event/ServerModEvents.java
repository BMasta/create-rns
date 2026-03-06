package com.bmaster.createrns.event;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.RNSRecipes;
import com.bmaster.createrns.content.deposit.info.DepositSpec;
import com.bmaster.createrns.content.deposit.info.IDepositIndex;
import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.CatalystRequirementSet;
import com.bmaster.createrns.data.gen.depositworldgen.DepositWorldgenProvider;
import com.bmaster.createrns.data.pack.DynamicDatapack;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.server.packs.PackType;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DataPackRegistryEvent;

@Mod.EventBusSubscriber(modid = CreateRNS.ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ServerModEvents {
    @SubscribeEvent
    public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.register(IDepositIndex.class);
    }

    @SubscribeEvent
    public static void onNewRegistry(DataPackRegistryEvent.NewRegistry e) {
        e.dataPackRegistry(DepositSpec.REGISTRY_KEY, DepositSpec.CODEC, DepositSpec.CODEC);
        e.dataPackRegistry(CatalystRequirementSet.REGISTRY_KEY, CatalystRequirementSet.CODEC, CatalystRequirementSet.CODEC);
    }

    @SubscribeEvent
    public static void onAddPackFinders(AddPackFindersEvent e) {
        if (e.getPackType() == PackType.SERVER_DATA) {
            for (var pack : DynamicDatapack.DATAPACKS) {
                e.addRepositorySource(consumer -> consumer.accept(pack));
            }
        }
        if (e.getPackType() == PackType.CLIENT_RESOURCES) {
            for (var pack : DynamicDatapack.RESOURCE_PACKS) {
                e.addRepositorySource(consumer -> consumer.accept(pack));
            }
        }
    }

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        generator.addProvider(event.includeServer(), new RNSRecipes.MechanicalCrafting(output));
        generator.addProvider(event.includeServer(), new DepositWorldgenProvider(output));
    }
}
