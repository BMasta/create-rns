package com.bmaster.createrns.event;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.RNSMisc;
import com.bmaster.createrns.RNSRecipes;
import com.bmaster.createrns.content.deposit.info.IDepositIndex;
import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.CatalystRequirementSet;
import com.bmaster.createrns.content.deposit.spec.DepositSpec;
import com.bmaster.createrns.data.gen.depositworldgen.DepositWorldgenProvider;
import com.bmaster.createrns.data.pack.DynamicDatapack;
import com.bmaster.createrns.data.pack.MiningRecipeBuilder;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DataPackRegistryEvent;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mod.EventBusSubscriber(modid = CreateRNS.ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class CommonModEvents {
    @SubscribeEvent
    public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.register(IDepositIndex.class);
    }

    @SubscribeEvent
    public static void onNewRegistry(DataPackRegistryEvent.NewRegistry e) {
        e.dataPackRegistry(DepositSpec.REGISTRY_KEY, DepositSpec.CODEC, DepositSpec.CODEC);
        e.dataPackRegistry(CatalystRequirementSet.REGISTRY_KEY, CatalystRequirementSet.CODEC,
                CatalystRequirementSet.STREAM_CODEC);
    }

    @SubscribeEvent
    public static void onAddPackFinders(AddPackFindersEvent e) {
        if (e.getPackType() == PackType.SERVER_DATA) {
            for (var ddp : DynamicDatapack.DATAPACKS) {
                e.addRepositorySource(consumer -> consumer.accept(ddp.build()));
            }
        }
        if (e.getPackType() == PackType.CLIENT_RESOURCES) {
            for (var drp : DynamicDatapack.RESOURCE_PACKS) {
                e.addRepositorySource(consumer -> consumer.accept(drp.build()));
            }
        }
    }

    @SubscribeEvent
    public static void onBuildCreativeModeTabContents(BuildCreativeModeTabContentsEvent e) {
        if (e.getTabKey() != RNSMisc.MAIN_TAB.getKey()) return;

        for (var r : MiningRecipeBuilder.getRecipes()) {
            var depItem = ForgeRegistries.ITEMS.getValue(r.recipe().depositBlockId());
            if (depItem != null && !r.isEnabled().get()) {
                e.getEntries().remove(new ItemStack(depItem));
            }
        }
    }

    @SubscribeEvent
    public static void onGatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        generator.addProvider(event.includeServer(), new RNSRecipes.MechanicalCrafting(output));
        generator.addProvider(event.includeServer(), new RNSRecipes.Polishing(output));
        generator.addProvider(event.includeServer(), new DepositWorldgenProvider(output));
    }
}
