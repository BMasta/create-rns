package com.bmaster.createrns.serialization;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.content.deposit.info.sync.*;
import com.bmaster.createrns.content.deposit.scanning.DepositScannerC2SPacket;
import com.bmaster.createrns.content.deposit.scanning.DepositScannerClientHandler.AntennaStatus;
import com.bmaster.createrns.content.deposit.scanning.DepositScannerClientHandler.HeightStatus;
import com.bmaster.createrns.content.deposit.scanning.DepositScannerS2CPacket;
import com.bmaster.createrns.content.deposit.scanning.DepositScannerServerHandler.RequestType;
import com.bmaster.createrns.util.CodecHelper;
import io.netty.buffer.Unpooled;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.function.Function;

@GameTestHolder(CreateRNS.ID)
@PrefixGameTestTemplate(false)
public class NetworkPayloadSerialization {
    private static final ResourceKey<Structure> STRUCTURE_KEY =
            ResourceKey.create(Registries.STRUCTURE, CreateRNS.asResource("deposit_iron"));

    @GameTest(template = "empty16x16")
    public void foundDepositSyncEntryRoundTrips(GameTestHelper helper) {
        var level = helper.getLevel();
        var payload = new FoundDepositSyncEntry(level.dimension(), STRUCTURE_KEY, new ChunkPos(3, -2),
                helper.absolutePos(new net.minecraft.core.BlockPos(4, 5, 6)));

        var restored = roundTrip(payload, FoundDepositSyncEntry::encode, FoundDepositSyncEntry::decode);

        CodecHelper.assertValueEqual(helper, restored, payload, "restored found deposit sync entry");
        helper.succeed();
    }

    @GameTest(template = "empty16x16")
    public void foundDepositPayloadsRoundTrip(GameTestHelper helper) {
        var level = helper.getLevel();
        var entry = new FoundDepositSyncEntry(level.dimension(), STRUCTURE_KEY, new ChunkPos(1, 2),
                helper.absolutePos(new net.minecraft.core.BlockPos(7, 8, 9)));
        var delta = new FoundDepositDeltaS2CPacket(FoundDepositDeltaS2CPacket.Operation.ADD, entry);
        var clear = new FoundDepositsClearS2CPacket(level.dimension());
        var snapshot = new FoundDepositsSnapshotS2CPacket(List.of(entry));

        CodecHelper.assertValueEqual(helper,
                roundTrip(delta, FoundDepositDeltaS2CPacket::encode, FoundDepositDeltaS2CPacket::decode), delta,
                "restored found deposit delta packet");
        CodecHelper.assertValueEqual(helper,
                roundTrip(clear, FoundDepositsClearS2CPacket::encode, FoundDepositsClearS2CPacket::decode), clear,
                "restored found deposit clear packet");
        CodecHelper.assertValueEqual(helper,
                roundTrip(snapshot, FoundDepositsSnapshotS2CPacket::encode, FoundDepositsSnapshotS2CPacket::decode),
                snapshot, "restored found deposit snapshot packet");
        helper.assertTrue(
                roundTrip(FoundDepositsSnapshotC2SPacket.INSTANCE, FoundDepositsSnapshotC2SPacket::encode,
                        FoundDepositsSnapshotC2SPacket::decode) == FoundDepositsSnapshotC2SPacket.INSTANCE,
                "Snapshot request packet should round-trip to its singleton instance");
        helper.succeed();
    }

    @GameTest(template = "empty16x16")
    public void scannerPayloadsRoundTrip(GameTestHelper helper) {
        var c2s = new DepositScannerC2SPacket(Items.COMPASS, RequestType.TRACK);
        var s2c = new DepositScannerS2CPacket(
                AntennaStatus.BOTH_ACTIVE,
                HeightStatus.ABOVE,
                42,
                true,
                RequestType.TRACK
        );

        var restoredC2s = roundTrip(c2s, DepositScannerC2SPacket::encode, DepositScannerC2SPacket::decode);
        helper.assertTrue(restoredC2s.item() == Items.COMPASS, "Scanner request packet should preserve the item");
        CodecHelper.assertValueEqual(helper, restoredC2s.rt(), RequestType.TRACK, "scanner request type");
        CodecHelper.assertValueEqual(helper,
                roundTrip(s2c, DepositScannerS2CPacket::encode, DepositScannerS2CPacket::decode), s2c,
                "restored scanner reply packet");
        helper.succeed();
    }

    private static <T> T roundTrip(T value, PacketEncoder<T> encoder, Function<FriendlyByteBuf, T> decoder) {
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        encoder.encode(value, buffer);
        return decoder.apply(buffer);
    }

    @FunctionalInterface
    private interface PacketEncoder<T> {
        void encode(T value, FriendlyByteBuf buffer);
    }
}
