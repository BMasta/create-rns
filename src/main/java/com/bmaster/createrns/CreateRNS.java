package com.bmaster.createrns;

import com.mojang.logging.LogUtils;
import com.simibubi.create.foundation.data.CreateRegistrate;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(CreateRNS.MOD_ID)
public class CreateRNS {
    public static final String MOD_ID = "create_rns";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final CreateRegistrate REGISTRATE = CreateRegistrate.create(CreateRNS.MOD_ID);

    public CreateRNS(ModContainer container, IEventBus modBus)  {
        REGISTRATE.registerEventListeners(modBus);

        RNSContent.register();
        RNSRecipeTypes.register();
    }
}
