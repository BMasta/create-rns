package com.bmaster.createrns.compat.kubejs;

import com.bmaster.createrns.CreateRNS;
import dev.latvian.mods.kubejs.event.StartupEventJS;
import dev.latvian.mods.kubejs.typings.Info;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class DepositStructureSetKubeEvent extends StartupEventJS {
    private final DepositStructureSetKubeBuilder overworld;
    private final DepositStructureSetKubeBuilder nether;

    public DepositStructureSetKubeEvent(Map<ResourceLocation, AvailableStructure> availableStructures) {
        overworld = new DepositStructureSetKubeBuilder(CreateRNS.asResource("deposits"), availableStructures);
        nether = new DepositStructureSetKubeBuilder(CreateRNS.asResource("nether_deposits"), availableStructures);
    }

    @Info("Returns the overworld deposit structure set builder.")
    public DepositStructureSetKubeBuilder overworld() {
        return overworld;
    }

    @Info("Returns the nether deposit structure set builder.")
    public DepositStructureSetKubeBuilder nether() {
        return nether;
    }

    public record AvailableStructure(ResourceLocation id, @Nullable Integer weight, boolean builtIn) {
    }
}
