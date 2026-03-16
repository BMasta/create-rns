package com.bmaster.createrns;

import com.bmaster.createrns.content.deposit.mining.block.MinerBlockEntity;
import com.bmaster.createrns.content.deposit.mining.block.MinerRenderer;
import com.bmaster.createrns.content.deposit.mining.block.MinerVisual;
import com.bmaster.createrns.content.deposit.mining.multiblock.MinerBearingBlockEntity;
import com.bmaster.createrns.content.deposit.mining.multiblock.attachment.resonator.ResonatorBlockEntity;
import com.bmaster.createrns.content.deposit.mining.multiblock.attachment.resonator.ResonatorRenderer;
import com.simibubi.create.content.contraptions.bearing.BearingRenderer;
import com.simibubi.create.content.contraptions.bearing.BearingVisual;
import com.tterrag.registrate.util.entry.BlockEntityEntry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class RNSBlockEntities {
    public static final BlockEntityEntry<MinerBlockEntity> MINER_BE = CreateRNS.REGISTRATE
            .blockEntity("miner", (BlockEntityType<MinerBlockEntity> t, BlockPos p, BlockState s) ->
                    new MinerBlockEntity(p, s))
            .visual(() -> MinerVisual::new)
            .validBlock(RNSBlocks.MINER_MK1_BLOCK)
            .validBlock(RNSBlocks.MINER_MK2_BLOCK)
            .renderer(() -> MinerRenderer::new)
            .register();

    public static final BlockEntityEntry<MinerBearingBlockEntity> MINER_BEARING_BE = CreateRNS.REGISTRATE
            .blockEntity("miner_bearing", MinerBearingBlockEntity::new)
            .visual(() -> BearingVisual::new)
            .validBlocks(RNSBlocks.MINER_BEARING_BLOCK)
            .renderer(() -> BearingRenderer::new)
            .register();

    public static final BlockEntityEntry<ResonatorBlockEntity> RESONATOR_BE = CreateRNS.REGISTRATE
            .blockEntity("resonator", ResonatorBlockEntity::new)
            .validBlocks(RNSBlocks.RESONATOR_BLOCK)
            .validBlocks(RNSBlocks.SHATTERING_RESONATOR_BLOCK)
            .validBlocks(RNSBlocks.STABILIZING_RESONATOR_BLOCK)
            .renderer(() -> ResonatorRenderer::new)
            .register();

    public static void register() {
    }
}
