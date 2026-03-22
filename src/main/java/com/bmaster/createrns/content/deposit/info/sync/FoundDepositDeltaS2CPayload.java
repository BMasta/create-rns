package com.bmaster.createrns.content.deposit.info.sync;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.content.deposit.info.FoundDepositClientCache;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public record FoundDepositDeltaS2CPayload(
        Operation operation, FoundDepositSyncEntry entry
) implements CustomPacketPayload {

    public enum Operation {
        ADD, REMOVE
    }

    public static final Type<FoundDepositDeltaS2CPayload> TYPE =
            new Type<>(CreateRNS.asResource("found_deposit_delta_s2c"));

    public static final StreamCodec<RegistryFriendlyByteBuf, FoundDepositDeltaS2CPayload> STREAM_CODEC =
            StreamCodec.composite(
                    NeoForgeStreamCodecs.enumCodec(Operation.class), FoundDepositDeltaS2CPayload::operation,
                    FoundDepositSyncEntry.STREAM_CODEC, FoundDepositDeltaS2CPayload::entry,
                    FoundDepositDeltaS2CPayload::new
            );

    public static void handle(FoundDepositDeltaS2CPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            var location = payload.entry.toClientDepositLocation();
            switch (payload.operation) {
                case ADD -> FoundDepositClientCache.add(payload.entry.dimension(), location);
                case REMOVE -> FoundDepositClientCache.remove(payload.entry.dimension(), location);
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
