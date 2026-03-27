package com.bmaster.createrns.content.deposit.scanning;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.content.deposit.info.sync.FoundDepositDeltaS2CPacket;
import com.bmaster.createrns.content.deposit.info.sync.FoundDepositsClearS2CPacket;
import com.bmaster.createrns.content.deposit.info.sync.FoundDepositsSnapshotC2SPacket;
import com.bmaster.createrns.content.deposit.info.sync.FoundDepositsSnapshotS2CPacket;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class DepositScannerChannel {
    private static final String PROTOCOL = "4";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(CreateRNS.ID, "deposit_scanner"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    private static int id = 0;

    private static int next() {
        return id++;
    }

    public static void init() {
        CHANNEL.registerMessage(next(), DepositScannerC2SPacket.class,
                DepositScannerC2SPacket::encode, DepositScannerC2SPacket::decode, DepositScannerC2SPacket::handle);

        CHANNEL.registerMessage(next(), DepositScannerS2CPacket.class,
                DepositScannerS2CPacket::encode, DepositScannerS2CPacket::decode, DepositScannerS2CPacket::handle);

        CHANNEL.registerMessage(next(), DepositIconsC2SPacket.class,
                DepositIconsC2SPacket::encode, DepositIconsC2SPacket::decode, DepositIconsC2SPacket::handle);

        CHANNEL.registerMessage(next(), DepositIconsS2CPacket.class,
                DepositIconsS2CPacket::encode, DepositIconsS2CPacket::decode, DepositIconsS2CPacket::handle);

        CHANNEL.registerMessage(next(), FoundDepositsSnapshotC2SPacket.class,
                FoundDepositsSnapshotC2SPacket::encode, FoundDepositsSnapshotC2SPacket::decode,
                FoundDepositsSnapshotC2SPacket::handle);

        CHANNEL.registerMessage(next(), FoundDepositsSnapshotS2CPacket.class,
                FoundDepositsSnapshotS2CPacket::encode, FoundDepositsSnapshotS2CPacket::decode,
                FoundDepositsSnapshotS2CPacket::handle);

        CHANNEL.registerMessage(next(), FoundDepositDeltaS2CPacket.class,
                FoundDepositDeltaS2CPacket::encode, FoundDepositDeltaS2CPacket::decode,
                FoundDepositDeltaS2CPacket::handle);

        CHANNEL.registerMessage(next(), FoundDepositsClearS2CPacket.class,
                FoundDepositsClearS2CPacket::encode, FoundDepositsClearS2CPacket::decode,
                FoundDepositsClearS2CPacket::handle);
    }
}
