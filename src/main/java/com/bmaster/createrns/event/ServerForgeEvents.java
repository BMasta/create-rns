package com.bmaster.createrns.event;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.RNSMisc;
import com.bmaster.createrns.content.deposit.info.DepositIndexProvider;
import com.bmaster.createrns.content.deposit.info.DepositSpecLookup;
import com.bmaster.createrns.content.deposit.info.IDepositIndex;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CreateRNS.ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ServerForgeEvents {
    @SubscribeEvent
    public static void onAttachCaps(AttachCapabilitiesEvent<Level> event) {
        if (!(event.getObject() instanceof ServerLevel sl)) return;

        event.addCapability(
                ResourceLocation.fromNamespaceAndPath(CreateRNS.ID, "deposit_index"),
                new DepositIndexProvider()
        );
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        var d = event.getDispatcher();
        d.register(RNSMisc.RNS_COMMAND);
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load e) {
        if (!(e.getLevel() instanceof ServerLevel sl)) return;

        var depIdx = IDepositIndex.fromLevel(sl);
        if (depIdx == null) return;

        ChunkPos pos = e.getChunk().getPos();
        var sm = sl.structureManager();

        for (var start : sm.startsForStructure(pos, DepositSpecLookup.isDeposit(sl.registryAccess()))) {
            sl.registryAccess()
                    .registryOrThrow(Registries.STRUCTURE)
                    .getResourceKey(start.getStructure())
                    .ifPresent(structKey -> depIdx.addDeposit(structKey, start));
        }
    }
}
