package com.bmaster.createrns.content.deposit.scanning;

import com.bmaster.createrns.CreateRNS;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class DepositScannerChannel {
    private static final String PROTOCOL = "1";
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
    }
}
