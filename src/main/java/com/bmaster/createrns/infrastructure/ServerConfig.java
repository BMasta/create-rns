package com.bmaster.createrns.infrastructure;

import com.bmaster.createrns.CreateRNS;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Mod.EventBusSubscriber(modid = CreateRNS.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ServerConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    // ------------------------------------------------ Config values ----------------------------------------------- //
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> OVERWORLD_ORES_CV = BUILDER
            .comment("Ore chunks for these items will spawn in the overworld")
            .defineListAllowEmpty("overworldOres", List.of(), ServerConfig::isValidRL);

    // -------------------------------------------------------------------------------------------------------------- //

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    // ------------------------------------------------ Baked values ------------------------------------------------ //
    public static List<Item> OVERWORLD_ORES;

    // -------------------------------------------------------------------------------------------------------------- //

    private static boolean isValidRL(final Object obj) {
        return (obj instanceof String s) && ResourceLocation.tryParse(s) != null;
    }

    @SubscribeEvent
    static void onLoadReload(final ModConfigEvent event) {
        if (event instanceof ModConfigEvent.Unloading) return;
        if (event.getConfig().getSpec() != ServerConfig.SPEC) return;

        // Convert item id's into items
        OVERWORLD_ORES = OVERWORLD_ORES_CV.get().stream()
                .map(ResourceLocation::parse)
                .map(ForgeRegistries.ITEMS::getValue)
                .filter(java.util.Objects::nonNull)
                .toList();
        CreateRNS.LOGGER.info("OVERWORLD_ORES is {}", OVERWORLD_ORES);
    }
}
