package com.bmaster.createrns.compat.kubejs;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import javax.annotation.ParametersAreNonnullByDefault;

/// Used for safely interacting with the plugin regardless of KubeJS availability
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class RNSKubeJSPluginBridge {
    private static Supplier<Map<ResourceKey<Level>, Set<ResourceLocation>>> selection = Map::of;

    public static Map<ResourceKey<Level>, Set<ResourceLocation>> getSelectedDepositBlocks() {
        return selection.get();
    }

    static void install(Supplier<Map<ResourceKey<Level>, Set<ResourceLocation>>> selection) {
        RNSKubeJSPluginBridge.selection = selection;
    }

    private RNSKubeJSPluginBridge() {
    }
}
