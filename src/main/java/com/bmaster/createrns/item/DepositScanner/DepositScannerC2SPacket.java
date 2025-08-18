package com.bmaster.createrns.item.DepositScanner;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.capability.orechunkdata.OreChunkClassifier;
import com.bmaster.createrns.util.Utils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;
import com.bmaster.createrns.item.DepositScanner.DepositScannerItemRenderer.AntennaStatus;

import java.util.Optional;
import java.util.function.Supplier;

public record DepositScannerC2SPacket(Item item) {
    private static final int MAX_CHESSBOARD_CHUNK_DISTANCE = 100;
    private static final int MAX_BLOCK_DISTANCE = MAX_CHESSBOARD_CHUNK_DISTANCE * 16;

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

            Optional<ChunkPos> posOpt = OreChunkClassifier.INSTANCE.getNearestOreChunk(sp.chunkPosition(), sl.getSeed(),
                    p.item, MAX_CHESSBOARD_CHUNK_DISTANCE);

            AntennaStatus status = AntennaStatus.INACTIVE;
            int interval = 0;
            boolean playerInChunk = false;

            if (posOpt.isPresent()) {
                var chunkPos = posOpt.get();
                var playerPos = sp.blockPosition();
                playerInChunk = Utils.isPosInChunk(playerPos, chunkPos);

                if (playerInChunk) {
                    status = AntennaStatus.BOTH_ACTIVE;
                } else {
                    var chunkCenterPos = chunkPos.getMiddleBlockPosition(playerPos.getY());
                    var distance = Math.min(MAX_BLOCK_DISTANCE, Math.sqrt(playerPos.distSqr(chunkCenterPos)));

                    float curYaw = sp.getYRot();
                    float targetYaw = getYaw(sp.blockPosition(),  chunkCenterPos);
                    float diff = Mth.wrapDegrees(targetYaw - curYaw);

                    // [5, 60] ticks == [0.25, 3] seconds
                    interval = 5 + (int) (55 * distance / MAX_BLOCK_DISTANCE);

                    if (Math.abs(diff) < 30) {
                        status = AntennaStatus.BOTH_ACTIVE;
                    } else if (diff <= 0) {
                        status = AntennaStatus.LEFT_ACTIVE;
                    } else {
                        status = AntennaStatus.RIGHT_ACTIVE;
                    }
//                    CreateRNS.LOGGER.info("cy={}, ty={} diff={} dist={} int={}", curYaw, targetYaw,
//                            diff, distance, interval);
                }
            }

            DepositScannerChannel.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp),
                    new DepositScannerS2CPacket(status, interval, playerInChunk));
        });
        ctx.setPacketHandled(true);
    }

    private static float getYaw(BlockPos from, BlockPos to) {
        Vec3 a = Vec3.atCenterOf(from);
        Vec3 b = Vec3.atCenterOf(to);
        Vec3 v = b.subtract(a);

        return (float) Mth.wrapDegrees(
                Math.toDegrees(Mth.atan2(-v.x, v.z))
        );
    }
}
