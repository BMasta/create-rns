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
            CreateRNS.asResource("block/shattering_resonator/shard_active")),

    SCANNER_UNPOWERED = PartialModel.of(CreateRNS.asResource("item/deposit_scanner/unpowered")),

    SCANNER_POWERED_1 = PartialModel.of(CreateRNS.asResource("item/deposit_scanner/powered_1")),

    SCANNER_POWERED_2 = PartialModel.of(CreateRNS.asResource("item/deposit_scanner/powered_2")),

    SCANNER_POWERED_3 = PartialModel.of(CreateRNS.asResource("item/deposit_scanner/powered_3")),

    SCANNER_POWERED_4 = PartialModel.of(CreateRNS.asResource("item/deposit_scanner/powered_4")),

    SCANNER_POWERED_5 = PartialModel.of(CreateRNS.asResource("item/deposit_scanner/powered_5")),

    SCANNER_POWERED = PartialModel.of(CreateRNS.asResource("item/deposit_scanner/powered")),

    ANTENNA_UNPOWERED = PartialModel.of(CreateRNS.asResource("item/deposit_scanner/antenna_unpowered")),

    ANTENNA_POWERED = PartialModel.of(CreateRNS.asResource("item/deposit_scanner/antenna_powered")),

    WHEEL = PartialModel.of(CreateRNS.asResource("item/deposit_scanner/wheel"));

    public static void register() {
    }
}
