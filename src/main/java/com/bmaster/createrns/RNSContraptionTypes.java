package com.bmaster.createrns;

import com.bmaster.createrns.content.deposit.mining.contraption.MinerContraption;
import com.simibubi.create.api.contraption.ContraptionType;
import com.simibubi.create.api.registry.CreateBuiltInRegistries;
import net.minecraft.core.Holder.Reference;
import net.minecraft.core.Registry;
import net.neoforged.neoforge.registries.RegisterEvent;

public class RNSContraptionTypes {
    public static final Reference<ContraptionType> MINER_BEARING = Registry.registerForHolder(
            CreateBuiltInRegistries.CONTRAPTION_TYPE,
            CreateRNS.asResource("miner_bearing"),
            new ContraptionType(MinerContraption::new)
    );

    public static void register(RegisterEvent event) {
    }
}
