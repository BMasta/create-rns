package com.bmaster.createrns.compat.kubejs;

import com.bmaster.createrns.CreateRNS;
import dev.latvian.mods.kubejs.event.KubeStartupEvent;
import dev.latvian.mods.kubejs.typings.Info;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;
import java.util.function.Supplier;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class DepositStructureSetKubeEvent implements KubeStartupEvent {
    private final Supplier<Map<ResourceLocation, AvailableStructure>> availableStructures;
    private @Nullable DepositStructureSetKubeBuilder overworld;
    private @Nullable DepositStructureSetKubeBuilder nether;

    public DepositStructureSetKubeEvent(Map<ResourceLocation, AvailableStructure> availableStructures) {
        this(() -> Map.copyOf(availableStructures));
    }

    DepositStructureSetKubeEvent(Supplier<Map<ResourceLocation, AvailableStructure>> availableStructures) {
        this.availableStructures = availableStructures;
    }

    @Info("Returns the overworld deposit structure set builder.")
    public DepositStructureSetKubeBuilder overworld() {
        if (overworld == null) {
            overworld = new DepositStructureSetKubeBuilder(CreateRNS.asResource("deposits"), availableStructures);
        }
        return overworld;
    }

    @Info("Returns the nether deposit structure set builder.")
    public DepositStructureSetKubeBuilder nether() {
        if (nether == null) {
            nether = new DepositStructureSetKubeBuilder(CreateRNS.asResource("nether_deposits"), availableStructures);
        }
        return nether;
    }

    @Nullable
    DepositStructureSetKubeBuilder configuredOverworld() {
        return overworld;
    }

    @Nullable
    DepositStructureSetKubeBuilder configuredNether() {
        return nether;
    }

    boolean hasConfiguredDimensions() {
        return overworld != null || nether != null;
    }

    public record AvailableStructure(ResourceLocation id, @Nullable Integer weight, boolean builtIn) {
    }
}
