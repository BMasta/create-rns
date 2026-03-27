package com.bmaster.createrns.content.deposit.scanning;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Supplier;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public record DepositIconsC2SPacket() {
    public static final DepositIconsC2SPacket INSTANCE = new DepositIconsC2SPacket();

    public static void send() {
        DepositScannerChannel.CHANNEL.sendToServer(INSTANCE);
    }

    public static void encode(DepositIconsC2SPacket packet, FriendlyByteBuf buffer) {
    }

    public static DepositIconsC2SPacket decode(FriendlyByteBuf buffer) {
        return INSTANCE;
    }

    public static void handle(DepositIconsC2SPacket packet, Supplier<NetworkEvent.Context> ctxSup) {
        var ctx = ctxSup.get();
        ctx.enqueueWork(() -> {
            ServerPlayer sp = ctx.getSender();
            if (sp == null) return;
            var server = sp.getServer();
            if (server == null) return;
            DepositIconsS2CPacket.send(sp, server);
        });
        ctx.setPacketHandled(true);
    }
}
