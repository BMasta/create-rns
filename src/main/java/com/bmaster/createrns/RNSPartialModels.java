package com.bmaster.createrns;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;

public class RNSPartialModels {
    public static final PartialModel

            RESONATOR_SHARD = PartialModel.of(
            CreateRNS.asResource("block/resonator/shard")),

    RESONATOR_SHARD_ACTIVE = PartialModel.of(
            CreateRNS.asResource("block/resonator/shard_active")),

    PROPAGATOR_SHARD = PartialModel.of(
            CreateRNS.asResource("block/resonance_propagator/shard")),

    PROPAGATOR_SHARD_ACTIVE = PartialModel.of(
            CreateRNS.asResource("block/resonance_propagator/shard_active")),

    STABILIZING_RESONATOR_SHARD = PartialModel.of(
            CreateRNS.asResource("block/stabilizing_resonator/shard")),

    STABILIZING_RESONATOR_SHARD_ACTIVE = PartialModel.of(
            CreateRNS.asResource("block/stabilizing_resonator/shard_active")),

    SHATTERING_RESONATOR_SHARD = PartialModel.of(
            CreateRNS.asResource("block/shattering_resonator/shard")),

    SHATTERING_RESONATOR_SHARD_ACTIVE = PartialModel.of(
            CreateRNS.asResource("block/shattering_resonator/shard_active"));

    public static void register() {
    }
}
