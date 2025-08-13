package com.github.bmasta.createrns.capability;

import com.github.bmasta.createrns.capability.orechunkdata.IOreChunkData;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

public class ModCapabilities {
    public static final Capability<IOreChunkData> ORE_CHUNK_DATA =
            CapabilityManager.get(new CapabilityToken<IOreChunkData>() {});


}
