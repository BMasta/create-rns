package com.bmaster.createrns.content.deposit.scanning;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.content.deposit.scanning.DepositScannerServerHandler.RequestType;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public record DepositScannerC2SPayload(ItemStack item, RequestType rt) implements CustomPacketPayload {
    public static final Type<DepositScannerC2SPayload> TYPE =
            new Type<>(CreateRNS.asResource("deposit_scanner_c2s"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DepositScannerC2SPayload> STREAM_CODEC =
            StreamCodec.composite(ItemStack.STREAM_CODEC, DepositScannerC2SPayload::item,
                    NeoForgeStreamCodecs.enumCodec(RequestType.class), DepositScannerC2SPayload::rt,
                    DepositScannerC2SPayload::new
            );

    public static void handle(DepositScannerC2SPayload p, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            var player = ctx.player();
            if (!(player instanceof ServerPlayer sp)) return;
            DepositScannerServerHandler.processScanRequest(sp, p.item.getItem(), p.rt);
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
