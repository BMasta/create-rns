package com.bmaster.createrns.mining.miner;

import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

public record MinerInfoS2CPacket(BlockPos minerPos, Object2FloatOpenHashMap<Item> progressInfo) {
    public static void send(ServerPlayer receiver, BlockPos minerPos, Object2FloatOpenHashMap<Item> progressInfo) {
        MinerInfoChannel.CHANNEL.send(PacketDistributor.PLAYER.with(() -> receiver),
                new MinerInfoS2CPacket(minerPos, progressInfo));
    }

    public static void encode(MinerInfoS2CPacket p, FriendlyByteBuf buf) {
        buf.writeBlockPos(p.minerPos);
        buf.writeMap(p.progressInfo, (b, i) -> b.writeItem(new ItemStack(i)),
                FriendlyByteBuf::writeFloat);
    }

    public static MinerInfoS2CPacket decode(FriendlyByteBuf buf) {
        var minerPos = buf.readBlockPos();
        var progressInfo = buf.readMap(Object2FloatOpenHashMap::new,
                (b) -> b.readItem().getItem(), FriendlyByteBuf::readFloat);
        return new MinerInfoS2CPacket(minerPos, progressInfo);
    }

    public static void handle(MinerInfoS2CPacket p, Supplier<NetworkEvent.Context> ctxSup) {
        var ctx = ctxSup.get();
        ctx.enqueueWork(() -> {
            var player = Minecraft.getInstance().player;
            if (player == null) return;
            var l = player.level();
            var be = l.getBlockEntity(p.minerPos);
            if (!(be instanceof MinerBlockEntity minerBE)) return;
        });
        ctx.setPacketHandled(true);
    }
}
