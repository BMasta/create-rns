package com.bmaster.createrns.serialization;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.content.deposit.info.sync.*;
import com.bmaster.createrns.content.deposit.scanning.DepositScannerC2SPayload;
import com.bmaster.createrns.content.deposit.scanning.DepositScannerClientHandler.AntennaStatus;
import com.bmaster.createrns.content.deposit.scanning.DepositScannerClientHandler.HeightStatus;
import com.bmaster.createrns.content.deposit.scanning.DepositScannerS2CPayload;
import com.bmaster.createrns.content.deposit.scanning.DepositScannerServerHandler.RequestType;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.network.connection.ConnectionType;

import java.util.List;

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

        var restored = roundTrip(FoundDepositSyncEntry.STREAM_CODEC, payload, level.registryAccess());

        helper.assertValueEqual(restored, payload, "restored found deposit sync entry");
        helper.succeed();
    }

    @GameTest(template = "empty16x16")
    public void foundDepositPayloadsRoundTrip(GameTestHelper helper) {
        var level = helper.getLevel();
        var entry = new FoundDepositSyncEntry(level.dimension(), STRUCTURE_KEY, new ChunkPos(1, 2),
                helper.absolutePos(new net.minecraft.core.BlockPos(7, 8, 9)));
        var delta = new FoundDepositDeltaS2CPayload(FoundDepositDeltaS2CPayload.Operation.ADD, entry);
        var clear = new FoundDepositsClearS2CPayload(level.dimension());
        var snapshot = new FoundDepositsSnapshotS2CPayload(List.of(entry));

        helper.assertValueEqual(roundTrip(FoundDepositDeltaS2CPayload.STREAM_CODEC, delta, level.registryAccess()), delta,
                "restored found deposit delta payload");
        helper.assertValueEqual(roundTrip(FoundDepositsClearS2CPayload.STREAM_CODEC, clear, level.registryAccess()), clear,
                "restored found deposit clear payload");
        helper.assertValueEqual(
                roundTrip(FoundDepositsSnapshotS2CPayload.STREAM_CODEC, snapshot, level.registryAccess()),
                snapshot,
                "restored found deposit snapshot payload"
        );
        helper.assertTrue(
                roundTrip(FoundDepositsSnapshotC2SPayload.STREAM_CODEC, FoundDepositsSnapshotC2SPayload.INSTANCE,
                        level.registryAccess()) == FoundDepositsSnapshotC2SPayload.INSTANCE,
                "Snapshot request payload should round-trip to its singleton instance"
        );
        helper.succeed();
    }

    @GameTest(template = "empty16x16")
    public void scannerPayloadsRoundTrip(GameTestHelper helper) {
        var level = helper.getLevel();
        var c2s = new DepositScannerC2SPayload(new ItemStack(Items.COMPASS), RequestType.TRACK);
        var s2c = new DepositScannerS2CPayload(
                AntennaStatus.BOTH_ACTIVE,
                HeightStatus.ABOVE,
                42,
                true,
                RequestType.TRACK
        );

        var restoredC2s = roundTrip(DepositScannerC2SPayload.STREAM_CODEC, c2s, level.registryAccess());
        helper.assertTrue(restoredC2s.item().is(Items.COMPASS), "Scanner request payload should preserve the item");
        helper.assertValueEqual(restoredC2s.item().getCount(), 1, "scanner request item count");
        helper.assertValueEqual(restoredC2s.rt(), RequestType.TRACK, "scanner request type");
        helper.assertValueEqual(roundTrip(DepositScannerS2CPayload.STREAM_CODEC, s2c, level.registryAccess()), s2c,
                "restored scanner reply payload");
        helper.succeed();
    }

    private static <T> T roundTrip(StreamCodec<RegistryFriendlyByteBuf, T> codec, T value, RegistryAccess access) {
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), access, ConnectionType.NEOFORGE);
        codec.encode(buffer, value);
        return codec.decode(buffer);
    }
}
