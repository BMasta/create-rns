package com.bmaster.createrns.content.deposit.info.sync;

import com.bmaster.createrns.content.deposit.scanning.DepositScannerChannel;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Supplier;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public record FoundDepositsSnapshotC2SPacket() {
    public static final FoundDepositsSnapshotC2SPacket INSTANCE = new FoundDepositsSnapshotC2SPacket();

    public static void send() {
        DepositScannerChannel.CHANNEL.sendToServer(INSTANCE);
    }

    public static void encode(FoundDepositsSnapshotC2SPacket packet, FriendlyByteBuf buffer) {
    }

    public static FoundDepositsSnapshotC2SPacket decode(FriendlyByteBuf buffer) {
        return INSTANCE;
    }

    public static void handle(FoundDepositsSnapshotC2SPacket packet, Supplier<NetworkEvent.Context> ctxSup) {
        var ctx = ctxSup.get();
        ctx.enqueueWork(() -> {
            ServerPlayer sp = ctx.getSender();
            if (sp == null) return;
            var server = sp.getServer();
            if (server == null) return;
            FoundDepositsSnapshotS2CPacket.send(sp, server);
        });
        ctx.setPacketHandled(true);
    }
}
