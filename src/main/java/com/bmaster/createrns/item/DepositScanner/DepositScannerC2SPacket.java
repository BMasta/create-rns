package com.bmaster.createrns.item.DepositScanner;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.capability.orechunkdata.OreChunkClassifier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Optional;
import java.util.function.Supplier;

public record DepositScannerC2SPacket(Item item) {
    public static void send(Item itemToScan) {
        DepositScannerChannel.CHANNEL.sendToServer(new DepositScannerC2SPacket(itemToScan));
    }

    public static void encode(DepositScannerC2SPacket p, FriendlyByteBuf buf) {
        ResourceLocation pId = ForgeRegistries.ITEMS.getKey(p.item);
        if (pId == null) pId = ForgeRegistries.ITEMS.getKey(Items.AIR);
        if (pId != null) buf.writeResourceLocation(pId);
    }

    public static DepositScannerC2SPacket decode(FriendlyByteBuf buf) {
        Item pItem = ForgeRegistries.ITEMS.getValue(buf.readResourceLocation());
        if (pItem == null) pItem = Items.AIR;
        return new DepositScannerC2SPacket(pItem);
    }

    public static void handle(DepositScannerC2SPacket p, Supplier<NetworkEvent.Context> ctxSup) {
        var ctx = ctxSup.get();
        ctx.enqueueWork(() -> {
            ServerPlayer sp = ctx.getSender();
            if (sp == null) return;
            if(!(sp.level() instanceof ServerLevel sl)) return;

            CreateRNS.LOGGER.info("Server received item to scan -> {}", p.item);
            Optional<ChunkPos> reply = OreChunkClassifier.INSTANCE.getNearestOreChunk(sp.chunkPosition(), sl.getSeed(),
                    p.item, 100);

            DepositScannerChannel.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp),
                    new DepositScannerS2CPacket(reply));
        });
        ctx.setPacketHandled(true);
    }
}
