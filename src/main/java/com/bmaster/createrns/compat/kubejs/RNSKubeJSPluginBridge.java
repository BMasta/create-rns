package com.bmaster.createrns.compat.kubejs;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/// Used for safely interacting with the plugin regardless of KubeJS availability
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class RNSKubeJSPluginBridge {
    private static BooleanSupplier managingDeposits = () -> false;
    private static Supplier<Set<ResourceLocation>> enabledDepositBlocks = Set::of;
    private static Supplier<Set<ResourceLocation>> selectedStructureIds = Set::of;

    public static boolean isManagingDeposits() {
        return managingDeposits.getAsBoolean();
    }

    public static Set<ResourceLocation> getEnabledDepositBlocks() {
        return enabledDepositBlocks.get();
    }

    public static Set<ResourceLocation> getSelectedStructureIds() {
        return selectedStructureIds.get();
    }

    public static void install(
            BooleanSupplier managingDeposits,
            Supplier<Set<ResourceLocation>> enabledDepositBlocks,
            Supplier<Set<ResourceLocation>> selectedStructureIds
    ) {
        RNSKubeJSPluginBridge.managingDeposits = managingDeposits;
        RNSKubeJSPluginBridge.enabledDepositBlocks = enabledDepositBlocks;
        RNSKubeJSPluginBridge.selectedStructureIds = selectedStructureIds;
    }

    private RNSKubeJSPluginBridge() {
    }
}
