package com.bmaster.createrns.compat.kubejs;

import com.bmaster.createrns.compat.kubejs.deposit.catalysts.CatalystsKubeEvent;
import com.bmaster.createrns.compat.kubejs.deposit.enable.EnableDepositsKubeEvent;
import com.bmaster.createrns.compat.kubejs.deposit.structures.DepositStructuresKubeEvent;
import com.bmaster.createrns.compat.kubejs.util.KubeStartupError;
import dev.latvian.mods.kubejs.event.EventHandler;
import dev.latvian.mods.kubejs.plugin.builtin.event.StartupEvents;
import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class RNSStartupKubeEvents {
    public static final EventHandler RNS_CATALYSTS =
            StartupEvents.GROUP.startup("rnsCatalysts", () -> CatalystsKubeEvent.class)
                    .exceptionHandler(KubeStartupError.eventHandler("rnsCatalysts"));
    public static final EventHandler RNS_DEPOSIT_STRUCTURES =
            StartupEvents.GROUP.startup("rnsDepositStructures", () -> DepositStructuresKubeEvent.class)
                    .exceptionHandler(KubeStartupError.eventHandler("rnsDepositStructures"));
    public static final EventHandler RNS_ENABLE_DEPOSITS =
            StartupEvents.GROUP.startup("rnsEnableDeposits", () -> EnableDepositsKubeEvent.class)
                    .exceptionHandler(KubeStartupError.eventHandler("rnsEnableDeposits"));

    @SuppressWarnings("EmptyMethod")
    public static void init() {
    }

    private RNSStartupKubeEvents() {
    }
}
