package com.bmaster.createrns.content.deposit.scanning;

import com.bmaster.createrns.content.deposit.spec.DepositSpecLookup;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public record DepositIconsS2CPacket(Map<ResourceKey<Level>, List<Item>> icons) {
    public static void send(ServerPlayer receiver, MinecraftServer server) {
        DepositScannerChannel.CHANNEL.send(PacketDistributor.PLAYER.with(() -> receiver),
                new DepositIconsS2CPacket(DepositSpecLookup.getScannerIcons(server)));
    }

    public static void encode(DepositIconsS2CPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.icons.size());
        for (var entry : packet.icons.entrySet()) {
            buffer.writeResourceLocation(entry.getKey().location());
            buffer.writeVarInt(entry.getValue().size());
            for (var item : entry.getValue()) {
                if (item == Items.AIR) continue;
                var itemId = ForgeRegistries.ITEMS.getKey(item);
                if (itemId == null) continue;
                buffer.writeResourceLocation(itemId);
            }
        }
    }

    public static DepositIconsS2CPacket decode(FriendlyByteBuf buffer) {
        int dimensions = buffer.readVarInt();
        var icons = new Object2ObjectOpenHashMap<ResourceKey<Level>, List<Item>>(dimensions);
        for (int i = 0; i < dimensions; ++i) {
            var dimension = ResourceKey.create(Registries.DIMENSION, buffer.readResourceLocation());
            int itemCount = buffer.readVarInt();
            var items = new ArrayList<Item>(itemCount);
            for (int j = 0; j < itemCount; ++j) {
                var item = ForgeRegistries.ITEMS.getValue(buffer.readResourceLocation());
                if  (item != null && item != Items.AIR) items.add(item);
            }
            icons.put(dimension, items);
        }
        return new DepositIconsS2CPacket(icons);
    }

    public static void handle(DepositIconsS2CPacket packet, Supplier<NetworkEvent.Context> ctxSup) {
        var ctx = ctxSup.get();
        ctx.enqueueWork(() -> DepositSpecLookup.setScannerIcons(packet.icons));
        ctx.setPacketHandled(true);
    }
}
