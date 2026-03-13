package com.bmaster.createrns;

import com.bmaster.createrns.content.deposit.mining.contraption.MinerBearingBlockEntity;
import com.bmaster.createrns.content.deposit.mining.contraption.attachment.drillhead.DrillHeadBlockEntity;
import com.bmaster.createrns.content.deposit.mining.contraption.attachment.drillhead.DrillHeadRenderer;
import com.bmaster.createrns.content.deposit.mining.contraption.attachment.resonance.buffer.ResonanceBufferBlockEntity;
import com.bmaster.createrns.content.deposit.mining.contraption.attachment.resonance.buffer.ResonanceBufferRenderer;
import com.bmaster.createrns.content.deposit.mining.contraption.attachment.resonance.resonator.ResonatorBlockEntity;
import com.bmaster.createrns.content.deposit.mining.contraption.attachment.resonance.resonator.ResonatorRenderer;
import com.simibubi.create.content.contraptions.bearing.BearingRenderer;
import com.simibubi.create.content.contraptions.bearing.BearingVisual;
import com.tterrag.registrate.util.entry.BlockEntityEntry;
import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class RNSBlockEntities {
    public static final BlockEntityEntry<MinerBearingBlockEntity> MINER_BEARING_BE = CreateRNS.REGISTRATE
            .blockEntity("miner_bearing", MinerBearingBlockEntity::new)
            .visual(() -> BearingVisual::new)
            .validBlocks(RNSBlocks.MINER_BEARING)
            .renderer(() -> BearingRenderer::new)
            .register();

    public static final BlockEntityEntry<DrillHeadBlockEntity> DRILL_HEAD_BE = CreateRNS.REGISTRATE
            .blockEntity("drill_head", DrillHeadBlockEntity::new)
            .validBlocks(RNSBlocks.DRILL_HEAD)
            .renderer(() -> DrillHeadRenderer::new)
            .register();

    public static final BlockEntityEntry<ResonatorBlockEntity> RESONATOR_BE = CreateRNS.REGISTRATE
            .blockEntity("resonator", ResonatorBlockEntity::new)
            .validBlocks(RNSBlocks.RESONATOR)
            .validBlocks(RNSBlocks.SHATTERING_RESONATOR)
            .validBlocks(RNSBlocks.STABILIZING_RESONATOR)
            .renderer(() -> ResonatorRenderer::new)
            .register();

    public static final BlockEntityEntry<ResonanceBufferBlockEntity> RESONANCE_BUFFER_BE = CreateRNS.REGISTRATE
            .blockEntity("resonance_buffer", ResonanceBufferBlockEntity::new)
            .validBlocks(RNSBlocks.RESONANCE_BUFFER)
            .renderer(() -> ResonanceBufferRenderer::new)
            .register();

    public static void register() {
    }
}
