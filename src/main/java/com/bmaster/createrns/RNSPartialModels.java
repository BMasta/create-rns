package com.bmaster.createrns;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.resources.ResourceLocation;

public class RNSPartialModels {
    // Partial models
    public static final PartialModel MINER_MK1_DRILL = PartialModel.of(
            ResourceLocation.fromNamespaceAndPath(CreateRNS.MOD_ID, "block/miner_mk1/drill_head"));
    public static final PartialModel MINER_MK2_DRILL = PartialModel.of(
            ResourceLocation.fromNamespaceAndPath(CreateRNS.MOD_ID, "block/miner_mk2/drill_head"));
    public static final PartialModel RESONATOR_SHARD = PartialModel.of(
            ResourceLocation.fromNamespaceAndPath(CreateRNS.MOD_ID, "block/resonator/shard"));
    public static final PartialModel RESONATOR_SHARD_ACTIVE = PartialModel.of(
            ResourceLocation.fromNamespaceAndPath(CreateRNS.MOD_ID, "block/resonator/shard_active"));
    public static final PartialModel STABILIZING_RESONATOR_SHARD = PartialModel.of(
            ResourceLocation.fromNamespaceAndPath(CreateRNS.MOD_ID, "block/stabilizing_resonator/shard"));
    public static final PartialModel STABILIZING_RESONATOR_SHARD_ACTIVE = PartialModel.of(
            ResourceLocation.fromNamespaceAndPath(CreateRNS.MOD_ID, "block/stabilizing_resonator/shard_active"));
    public static final PartialModel SHATTERING_RESONATOR_SHARD = PartialModel.of(
            ResourceLocation.fromNamespaceAndPath(CreateRNS.MOD_ID, "block/shattering_resonator/shard"));
    public static final PartialModel SHATTERING_RESONATOR_SHARD_ACTIVE = PartialModel.of(
            ResourceLocation.fromNamespaceAndPath(CreateRNS.MOD_ID, "block/shattering_resonator/shard_active"));

    public static void register() {
    }
}
