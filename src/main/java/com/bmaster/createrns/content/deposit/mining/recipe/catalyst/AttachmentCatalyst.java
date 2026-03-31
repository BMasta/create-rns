package com.bmaster.createrns.content.deposit.mining.recipe.catalyst;

import com.bmaster.createrns.RNSTags.RNSBlockTags;
import com.simibubi.create.content.contraptions.bearing.BearingContraption;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.world.level.block.Block;

import java.util.List;

public class AttachmentCatalyst extends Catalyst {
    public static List<AttachmentCatalyst> fromContraption(BearingContraption contraption) {
        var attachments = new Object2IntOpenHashMap<Block>();
        for (var info : contraption.getBlocks().values()) {
            if (info.state().is(RNSBlockTags.MINER_ATTACHMENTS)) {
                var b = info.state().getBlock();
                attachments.addTo(b, 1);
            }
        }
        return attachments.object2IntEntrySet().stream()
                .map(e -> new AttachmentCatalyst(e.getKey(), e.getIntValue()))
                .toList();
    }

    public final Block attachmentBlock;
    public final int count;

    public AttachmentCatalyst(Block attachmentBlock, int count) {
        this.attachmentBlock = attachmentBlock;
        this.count = count;
    }
}
