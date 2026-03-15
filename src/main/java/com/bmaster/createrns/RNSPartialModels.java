package com.bmaster.createrns;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class RNSPartialModels {
    public static final PartialModel

            RESONATOR_SHARD = PartialModel.of(
            CreateRNS.asResource("block/resonator/shard")),

    RESONATOR_SHARD_ACTIVE = PartialModel.of(
            CreateRNS.asResource("block/resonator/shard_active")),

    RESONANCE_BUFFER_SHARD = PartialModel.of(
            CreateRNS.asResource("block/resonance_buffer/shard")),

    RESONANCE_BUFFER_SHARD_ACTIVE = PartialModel.of(
            CreateRNS.asResource("block/resonance_buffer/shard_active")),

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
