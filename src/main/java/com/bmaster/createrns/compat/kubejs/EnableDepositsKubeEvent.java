package com.bmaster.createrns.compat.kubejs;

import com.bmaster.createrns.CreateRNS;
import dev.latvian.mods.kubejs.event.StartupEventJS;
import dev.latvian.mods.kubejs.typings.Info;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;
import java.util.function.Supplier;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class EnableDepositsKubeEvent extends StartupEventJS {
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
    public EnableDepositsKubeBuilder overworld() {
        if (overworld == null) {
            overworld = new EnableDepositsKubeBuilder(CreateRNS.asResource("deposits"), availableStructures);
        }
        return overworld;
    }

    @Info("Returns the nether deposit structure set builder.")
    public EnableDepositsKubeBuilder nether() {
        if (nether == null) {
            nether = new EnableDepositsKubeBuilder(CreateRNS.asResource("nether_deposits"), availableStructures);
        }
        return nether;
    }

    @Nullable
    EnableDepositsKubeBuilder configuredOverworld() {
        return overworld;
    }

    @Nullable
    EnableDepositsKubeBuilder configuredNether() {
        return nether;
    }

    boolean hasConfiguredDimensions() {
        return overworld != null || nether != null;
    }

    public record AvailableStructure(ResourceLocation id, @Nullable Integer weight, boolean builtIn) {
    }
}
