package com.bmaster.createrns.mining.miner;

import com.bmaster.createrns.item.DepositScanner.*;
import com.simibubi.create.content.equipment.goggles.GoggleOverlayRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class MinerInfoC2SPacket {
    private static final int COOLDOWN = 5;
    private static int sendIn = 0;

    public BlockPos minerPos;

    public static void send(BlockPos minerPos) {
        if (sendIn <= 0) {
            sendIn = COOLDOWN;
            forceSend(minerPos);
        } else {
            sendIn--;
        }
    }

    public static void forceSend(BlockPos minerPos) {
        sendIn = COOLDOWN;
        MinerInfoChannel.CHANNEL.sendToServer(new MinerInfoC2SPacket(minerPos));
    }

    public MinerInfoC2SPacket(BlockPos minerPos) {
        this.minerPos = minerPos;
    }

    public static void encode(MinerInfoC2SPacket p, FriendlyByteBuf buf) {
        buf.writeBlockPos(p.minerPos);
    }

    public static MinerInfoC2SPacket decode(FriendlyByteBuf buf) {
        return new MinerInfoC2SPacket(buf.readBlockPos());
    }

    public static void handle(MinerInfoC2SPacket p, Supplier<NetworkEvent.Context> ctxSup) {
        var ctx = ctxSup.get();
        ctx.enqueueWork(() -> {
            ServerPlayer sp = ctx.getSender();
            if (sp == null) return;
            var be = sp.level().getBlockEntity(p.minerPos);
            if (!(be instanceof MinerBlockEntity minerBE)) return;
        });
        ctx.setPacketHandled(true);
    }
}
