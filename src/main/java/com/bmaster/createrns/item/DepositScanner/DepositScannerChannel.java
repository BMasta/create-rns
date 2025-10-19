//package com.bmaster.createrns.item.DepositScanner;
//
//import com.bmaster.createrns.CreateRNS;
//import net.minecraft.resources.ResourceLocation;
//import net.minecraftforge.network.NetworkRegistry;
//import net.minecraftforge.network.simple.SimpleChannel;
//
//public class DepositScannerChannel {
//    private static final String PROTOCOL = "1";
//    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
//            ResourceLocation.fromNamespaceAndPath(CreateRNS.MOD_ID, "deposit_scanner"),
//            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);
//
//    private static int id = 0;
//
//    private static int next() {
//        return id++;
//    }
//
//    public static void init() {
//        CHANNEL.registerMessage(next(), DepositScannerC2SPacket.class,
//                DepositScannerC2SPacket::encode, DepositScannerC2SPacket::decode, DepositScannerC2SPacket::handle);
//
//        CHANNEL.registerMessage(next(), DepositScannerS2CPacket.class,
//                DepositScannerS2CPacket::encode, DepositScannerS2CPacket::decode, DepositScannerS2CPacket::handle);
//    }
//}
