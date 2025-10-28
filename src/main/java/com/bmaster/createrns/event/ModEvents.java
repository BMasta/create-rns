package com.bmaster.createrns.event;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.RNSContent;
import com.bmaster.createrns.RNSRecipes;
import com.bmaster.createrns.data.gen.depositworldgen.DepositWorldgenProvider;
import com.bmaster.createrns.deposit.spec.DepositSpec;
import com.bmaster.createrns.deposit.capability.IDepositIndex;
import com.bmaster.createrns.mining.miner.MinerSpec;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.event.AddPackFindersEvent;
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
        e.dataPackRegistry(DepositSpec.REGISTRY_KEY, DepositSpec.CODEC, DepositSpec.CODEC);
        e.dataPackRegistry(MinerSpec.REGISTRY_KEY, MinerSpec.CODEC, MinerSpec.CODEC);
    }

    @SubscribeEvent
    public static void onAddPackFinders(AddPackFindersEvent e) {
        // TODO: Could be useful in the future for compat with other mods
//        if (e.getPackType() != PackType.SERVER_DATA) return;
//        DynamicDatapack.addDepositBiomeTag();
//        DynamicDatapack.addDepositSetAndTag();
//        e.addRepositorySource(consumer -> consumer.accept(DynamicDatapack.finish()));
    }

    @SubscribeEvent
    public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(RNSContent.MINER_MK1_DRILL.modelLocation());
        event.register(RNSContent.MINER_MK2_DRILL.modelLocation());
    }

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        generator.addProvider(event.includeServer(), new RNSRecipes.Washing(output));
        generator.addProvider(event.includeServer(), new DepositWorldgenProvider(output));
    }
}
