package com.bmaster.createrns.event;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.capability.orechunkdata.OreChunkDataProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CreateRNS.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ForgeEvents {
    @SubscribeEvent
    public static void onAttachCaps(AttachCapabilitiesEvent<LevelChunk> event) {
        LevelChunk chunk = event.getObject();
        if (chunk.getLevel().isClientSide()) return;

        event.addCapability(
                ResourceLocation.fromNamespaceAndPath(CreateRNS.MOD_ID, "ore_chunk_data"),
                new OreChunkDataProvider(event.getObject(), true)
        );
    }
}
