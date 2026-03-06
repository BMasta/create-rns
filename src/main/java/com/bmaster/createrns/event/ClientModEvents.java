package com.bmaster.createrns.event;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.RNSParticleTypes;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = CreateRNS.ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {
    @SubscribeEvent
    public static void onRegisterParticleProviders(RegisterParticleProvidersEvent event) {
        RNSParticleTypes.RESONANCE.register(RNSParticleTypes.RESONANCE_TYPE.get(), event);
        RNSParticleTypes.STABILIZING_RESONANCE.register(RNSParticleTypes.STABILIZING_RESONANCE_TYPE.get(), event);
        RNSParticleTypes.SHATTERING_RESONANCE.register(RNSParticleTypes.SHATTERING_RESONANCE_TYPE.get(), event);
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        var stack = event.getItemStack();
        var tooltip = event.getToolTip();
        var itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
        assert itemId != null;
        var alwaysTooltipKey = "item." + itemId.getNamespace() + "." + itemId.getPath() + ".tooltip.always";
        var descriptionBasedKey = stack.getDescriptionId() + ".tooltip.always";
        for (int i = 1;;++i) {
            if (I18n.exists(alwaysTooltipKey + i)) {
                tooltip.add(Component.translatable(alwaysTooltipKey + i));
            } else if (I18n.exists(descriptionBasedKey + i)) {
                tooltip.add(Component.translatable(descriptionBasedKey + i));
            } else {
                break;
            }
        }
    }
}
