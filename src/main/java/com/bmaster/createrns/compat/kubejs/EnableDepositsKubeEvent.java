package com.bmaster.createrns.compat.kubejs;

import com.bmaster.createrns.CreateRNS;
import dev.latvian.mods.kubejs.event.KubeStartupEvent;
import dev.latvian.mods.kubejs.typings.Info;
import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.util.HideFromJS;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;
import java.util.function.Supplier;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class EnableDepositsKubeEvent implements KubeStartupEvent {
    private final Supplier<Map<ResourceLocation, AvailableStructure>> availableStructures;
    private @Nullable EnableDepositsKubeBuilder overworld;
    private @Nullable EnableDepositsKubeBuilder nether;

    public EnableDepositsKubeEvent(Map<ResourceLocation, AvailableStructure> availableStructures) {
        this(() -> Map.copyOf(availableStructures));
    }

    EnableDepositsKubeEvent(Supplier<Map<ResourceLocation, AvailableStructure>> availableStructures) {
        this.availableStructures = availableStructures;
    }

    @Info("Returns the overworld deposit structure set builder.")
    public EnableDepositsKubeBuilder overworld(Context cx) {
        if (overworld == null) {
            overworld = new EnableDepositsKubeBuilder(CreateRNS.asResource("deposits"), availableStructures,
                    KubeJSStartupError.builderStart(cx, "rnsEnableDeposits", "overworld|nether"));
        }
        return overworld;
    }

    @HideFromJS
    public EnableDepositsKubeBuilder overworld() {
        if (overworld == null) {
            overworld = new EnableDepositsKubeBuilder(CreateRNS.asResource("deposits"), availableStructures);
        }
        return overworld;
    }

    @Info("Returns the nether deposit structure set builder.")
    public EnableDepositsKubeBuilder nether(Context cx) {
        if (nether == null) {
            nether = new EnableDepositsKubeBuilder(CreateRNS.asResource("nether_deposits"), availableStructures,
                    KubeJSStartupError.builderStart(cx, "rnsEnableDeposits", "overworld|nether"));
        }
        return nether;
    }

    @HideFromJS
    public EnableDepositsKubeBuilder nether() {
        if (nether == null) {
            nether = new EnableDepositsKubeBuilder(CreateRNS.asResource("nether_deposits"), availableStructures);
        }
        return nether;
    }

    @Nullable
    EnableDepositsKubeBuilder configuredOverworld() {
        return overworld != null && !overworld.isInvalid() ? overworld : null;
    }

    @Nullable
    EnableDepositsKubeBuilder configuredNether() {
        return nether != null && !nether.isInvalid() ? nether : null;
    }

    boolean hasConfiguredDimensions() {
        return configuredOverworld() != null || configuredNether() != null;
    }

    public record AvailableStructure(
            ResourceLocation id, @Nullable Integer weight, boolean builtIn, boolean valid
    ) {
        public AvailableStructure(ResourceLocation id, @Nullable Integer weight, boolean builtIn) {
            this(id, weight, builtIn, true);
        }
    }
}
