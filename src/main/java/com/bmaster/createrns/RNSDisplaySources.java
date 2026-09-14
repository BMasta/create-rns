package com.bmaster.createrns;

import com.bmaster.createrns.content.deposit.mining.display.*;
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
    public static final RegistryEntry<MinerRatesDisplaySource>
            ESTIMATED_MINER_RATES = CreateRNS.REGISTRATE
            .displaySource("miner_rates", MinerRatesDisplaySource::new)
            .onRegisterAfter(Registries.BLOCK_ENTITY_TYPE, source ->
                    DisplaySource.BY_BLOCK_ENTITY.add(RNSBlockEntities.MINER_BEARING.get(), source))
            .register();
    public static final RegistryEntry<RemainingMinerResourcesDisplaySource> REMAINING_MINER_RESOURCES =
            CreateRNS.REGISTRATE.displaySource("remaining_miner_resources", RemainingMinerResourcesDisplaySource::new)
                    .onRegisterAfter(Registries.BLOCK_ENTITY_TYPE, source ->
                            DisplaySource.BY_BLOCK_ENTITY.add(RNSBlockEntities.MINER_BEARING.get(), source))
                    .register();

    public static void register() {
    }
}
