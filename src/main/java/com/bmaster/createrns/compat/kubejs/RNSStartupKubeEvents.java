package com.bmaster.createrns.compat.kubejs;

import dev.latvian.mods.kubejs.bindings.event.StartupEvents;
import dev.latvian.mods.kubejs.event.EventHandler;
import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class RNSStartupKubeEvents {
    public static final EventHandler RNS_CATALYSTS =
            StartupEvents.GROUP.startup("rnsCatalysts", () -> CatalystsKubeEvent.class);
    public static final EventHandler RNS_DEPOSIT_STRUCTURES =
            StartupEvents.GROUP.startup("rnsDepositStructures", () -> DepositStructuresKubeEvent.class);
    public static final EventHandler RNS_ENABLE_DEPOSITS =
            StartupEvents.GROUP.startup("rnsEnableDeposits", () -> EnableDepositsKubeEvent.class);

    public static void init() {
    }

    private RNSStartupKubeEvents() {
    }
}
