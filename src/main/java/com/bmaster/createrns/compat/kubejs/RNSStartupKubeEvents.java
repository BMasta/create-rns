package com.bmaster.createrns.compat.kubejs;

import dev.latvian.mods.kubejs.bindings.event.StartupEvents;
import dev.latvian.mods.kubejs.event.EventHandler;
import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class RNSStartupKubeEvents {
    public static final EventHandler CREATE_RNS_CATALYSTS =
            StartupEvents.GROUP.startup("createRnsCatalysts", () -> CatalystsKubeEvent.class);
    public static final EventHandler CREATE_RNS_DEPOSIT_STRUCTURES =
            StartupEvents.GROUP.startup("createRnsDepositStructures", () -> DepositStructuresKubeEvent.class);
    public static final EventHandler CREATE_RNS_STRUCTURE_SET =
            StartupEvents.GROUP.startup("createRnsStructureSet", () -> DepositStructureSetKubeEvent.class);

    public static void init() {
    }

    private RNSStartupKubeEvents() {
    }
}
