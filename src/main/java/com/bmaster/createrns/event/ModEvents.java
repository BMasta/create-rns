package com.bmaster.createrns.event;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.RNSContent;
import com.bmaster.createrns.datapack.DynamicDatapack;
import com.bmaster.createrns.capability.depositindex.DepositSpec;
import com.bmaster.createrns.capability.depositindex.IDepositIndex;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DataPackRegistryEvent;

import java.util.Set;

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

        var mediumNBT = ResourceLocation.fromNamespaceAndPath(CreateRNS.MOD_ID, "ore_deposit_medium");
        var ironDeposit = new DynamicDatapack.Deposit("iron", RNSContent.IRON_DEPOSIT_BLOCK.get(),
                mediumNBT, 0, 1);
        var copperDeposit = new DynamicDatapack.Deposit("copper", RNSContent.COPPER_DEPOSIT_BLOCK.get(),
                mediumNBT, 0, 1);
        var goldDeposit = new DynamicDatapack.Deposit("gold", RNSContent.GOLD_DEPOSIT_BLOCK.get(),
                mediumNBT, 0, 1);
        var redstoneDeposit = new DynamicDatapack.Deposit("redstone", RNSContent.REDSTONE_DEPOSIT_BLOCK.get(),
                mediumNBT, 0, 1);
        var dSet = new DynamicDatapack.DepositSet(Set.of(
                ironDeposit, copperDeposit, goldDeposit, redstoneDeposit));
        DynamicDatapack.add(dSet);

        e.addRepositorySource(consumer -> consumer.accept(DynamicDatapack.finish()));
    }
}
