package com.bmaster.createrns.event;

import com.bmaster.createrns.AllContent;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.InputEvent.MouseScrollingEvent;
import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.item.DepositScanner.DepositScannerClientHandler;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CreateRNS.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientForgeEvents {
}
