package com.bmaster.createrns.item.DepositScanner;

import com.bmaster.createrns.CreateRNS;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.function.Supplier;

public record DepositScannerC2SPacket(Item item, boolean recompute) {
    public static void send(Item itemToScan, boolean recompute) {
        DepositScannerChannel.CHANNEL.sendToServer(new DepositScannerC2SPacket(itemToScan, recompute));
    }

    public static void encode(DepositScannerC2SPacket p, FriendlyByteBuf buf) {
        ResourceLocation pId = ForgeRegistries.ITEMS.getKey(p.item);
        if (pId == null) pId = ForgeRegistries.ITEMS.getKey(Items.AIR);
        if (pId != null) buf.writeResourceLocation(pId);
        buf.writeBoolean(p.recompute);
    }

    public static DepositScannerC2SPacket decode(FriendlyByteBuf buf) {
        Item pItem = ForgeRegistries.ITEMS.getValue(buf.readResourceLocation());
        if (pItem == null) pItem = Items.AIR;
        return new DepositScannerC2SPacket(pItem, buf.readBoolean());
    }

    public static void handle(DepositScannerC2SPacket p, Supplier<NetworkEvent.Context> ctxSup) {
        var ctx = ctxSup.get();
        ctx.enqueueWork(() -> {
            ServerPlayer sp = ctx.getSender();
            if (sp == null) return;
            DepositScannerServerHandler.processScanRequest(sp, p.item, p.recompute);
        });
        ctx.setPacketHandled(true);
    }
}
