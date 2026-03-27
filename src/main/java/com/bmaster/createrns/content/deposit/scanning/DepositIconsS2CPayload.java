package com.bmaster.createrns.content.deposit.scanning;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.content.deposit.spec.DepositSpecLookup;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Map;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public record DepositIconsS2CPayload(Map<ResourceKey<Level>, ArrayList<Item>> icons) implements CustomPacketPayload {
    public static final Type<DepositIconsS2CPayload> TYPE =
            new Type<>(CreateRNS.asResource("deposit_icons_s2c"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DepositIconsS2CPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.map(
                    Object2ObjectOpenHashMap::new,
                    ResourceKey.streamCodec(Registries.DIMENSION),
                    ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.registry(Registries.ITEM))
            ), p -> p.icons,
            DepositIconsS2CPayload::new
    );

    public static void handle(DepositIconsS2CPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> DepositSpecLookup.setScannerIcons(payload.icons));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
