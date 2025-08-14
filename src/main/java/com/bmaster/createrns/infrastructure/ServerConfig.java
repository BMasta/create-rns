package com.bmaster.createrns.infrastructure;

import com.bmaster.createrns.CreateRNS;
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

    // a list of strings that are treated as resource locations for items
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> OVERWORLD_ORES = BUILDER
            .comment("Ore chunks for these items will spawn in the overworld")
            .defineListAllowEmpty("items", List.of(), ServerConfig::validateItemName);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    public static Set<Item> items;

    private static boolean validateItemName(final Object obj) {
        return obj instanceof final String itemName && ForgeRegistries.ITEMS.containsKey(
                ResourceLocation.parse(itemName));
    }

    @SubscribeEvent
    static void onLoadReload(final ModConfigEvent event) {
        if (event instanceof ModConfigEvent.Unloading) return;

        // convert the list of strings into a set of items
        items = OVERWORLD_ORES.get().stream()
                .map(itemName -> ForgeRegistries.ITEMS.getValue(ResourceLocation.parse(itemName)))
                .collect(Collectors.toSet());
    }
}
