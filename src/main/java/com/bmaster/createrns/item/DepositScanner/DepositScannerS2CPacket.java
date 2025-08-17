package com.bmaster.createrns.item.DepositScanner;

import com.bmaster.createrns.CreateRNS;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.network.NetworkEvent;

import java.util.Optional;
import java.util.function.Supplier;

public record DepositScannerS2CPacket(Optional<ChunkPos> oreChunkOpt) {
    public static void encode(DepositScannerS2CPacket p, FriendlyByteBuf buf) {
        buf.writeBoolean(p.oreChunkOpt.isPresent());
        p.oreChunkOpt.ifPresent(chunkPos -> buf.writeLong(chunkPos.toLong()));
    }

    public static DepositScannerS2CPacket decode(FriendlyByteBuf buf) {
        boolean present = buf.readBoolean();
        Optional<ChunkPos> cp = present ? Optional.of(new ChunkPos(buf.readLong())) : Optional.empty();
        return new DepositScannerS2CPacket(cp);
    }

    public static void handle(DepositScannerS2CPacket p, Supplier<NetworkEvent.Context> ctxSup) {
        var ctx = ctxSup.get();
        ctx.enqueueWork(() -> {
            var mc = Minecraft.getInstance();
            if (mc.player == null) return;

            if (p.oreChunkOpt.isPresent()) {
                ChunkPos pos = p.oreChunkOpt.get();
                CreateRNS.LOGGER.info("Client received ore chunk pos -> {}, {}", pos.x, pos.z);
            }
        });
        ctx.setPacketHandled(true);
    }
}
