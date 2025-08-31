package com.bmaster.createrns.mining.miner;

import com.bmaster.createrns.CreateRNS;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class MinerInfoChannel {
    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(CreateRNS.MOD_ID, "miner"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    private static int id = 0;

    private static int next() {
        return id++;
    }

    public static void init() {
        CHANNEL.registerMessage(next(), MinerInfoC2SPacket.class,
                MinerInfoC2SPacket::encode, MinerInfoC2SPacket::decode, MinerInfoC2SPacket::handle);

        CHANNEL.registerMessage(next(), MinerInfoS2CPacket.class,
                MinerInfoS2CPacket::encode, MinerInfoS2CPacket::decode, MinerInfoS2CPacket::handle);
    }

}
