//package com.bmaster.createrns.item.DepositScanner;
//
//import com.bmaster.createrns.item.DepositScanner.DepositScannerServerHandler.RequestType;
//import net.minecraft.network.FriendlyByteBuf;
//import net.minecraft.resources.ResourceLocation;
//import net.minecraft.server.level.ServerPlayer;
//import net.minecraft.world.item.Item;
//import net.minecraft.world.item.Items;
//import net.minecraftforge.network.NetworkEvent;
//import net.minecraftforge.registries.ForgeRegistries;
//
//import java.util.function.Supplier;
//
//public record DepositScannerC2SPacket(Item item, RequestType rt) {
//    public static void send(Item itemToScan, RequestType rt) {
//        DepositScannerChannel.CHANNEL.sendToServer(new DepositScannerC2SPacket(itemToScan, rt));
//    }
//
//    public static void encode(DepositScannerC2SPacket p, FriendlyByteBuf buf) {
//        ResourceLocation pId = ForgeRegistries.ITEMS.getKey(p.item);
//        if (pId == null) pId = ForgeRegistries.ITEMS.getKey(Items.AIR);
//        if (pId != null) buf.writeResourceLocation(pId);
//        buf.writeEnum(p.rt);
//    }
//
//    public static DepositScannerC2SPacket decode(FriendlyByteBuf buf) {
//        Item pItem = ForgeRegistries.ITEMS.getValue(buf.readResourceLocation());
//        if (pItem == null) pItem = Items.AIR;
//        return new DepositScannerC2SPacket(pItem, buf.readEnum(RequestType.class));
//    }
//
//    public static void handle(DepositScannerC2SPacket p, Supplier<NetworkEvent.Context> ctxSup) {
//        var ctx = ctxSup.get();
//        ctx.enqueueWork(() -> {
//            ServerPlayer sp = ctx.getSender();
//            if (sp == null) return;
//            DepositScannerServerHandler.processScanRequest(sp, p.item, p.rt);
//        });
//        ctx.setPacketHandled(true);
//    }
//}
