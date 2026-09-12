package com.bmaster.createrns;

import com.bmaster.createrns.content.deposit.mining.display.CompactMinerStatusDisplaySource;
import com.bmaster.createrns.content.deposit.mining.display.EstimatedMinerProductionRatesDisplaySource;
import com.bmaster.createrns.content.deposit.mining.display.FullMinerStatusDisplaySource;
import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.tterrag.registrate.util.entry.RegistryEntry;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.registries.Registries;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class RNSDisplaySources {
    public static final RegistryEntry<CompactMinerStatusDisplaySource> COMPACT_MINER_STATUS =
            CreateRNS.REGISTRATE.displaySource("compact_miner_status", CompactMinerStatusDisplaySource::new)
                    .onRegisterAfter(Registries.BLOCK_ENTITY_TYPE, source ->
                            DisplaySource.BY_BLOCK_ENTITY.add(RNSBlockEntities.MINER_BEARING.get(), source))
                    .register();
    public static final RegistryEntry<FullMinerStatusDisplaySource> FULL_MINER_STATUS =
            CreateRNS.REGISTRATE.displaySource("full_miner_status", FullMinerStatusDisplaySource::new)
                    .onRegisterAfter(Registries.BLOCK_ENTITY_TYPE, source ->
                            DisplaySource.BY_BLOCK_ENTITY.add(RNSBlockEntities.MINER_BEARING.get(), source))
                    .register();
    public static final RegistryEntry<EstimatedMinerProductionRatesDisplaySource>
            ESTIMATED_MINER_PRODUCTION_RATES = CreateRNS.REGISTRATE
            .displaySource("estimated_miner_production_rates", EstimatedMinerProductionRatesDisplaySource::new)
            .onRegisterAfter(Registries.BLOCK_ENTITY_TYPE, source ->
                    DisplaySource.BY_BLOCK_ENTITY.add(RNSBlockEntities.MINER_BEARING.get(), source))
            .register();

    public static void register() {
    }
}
