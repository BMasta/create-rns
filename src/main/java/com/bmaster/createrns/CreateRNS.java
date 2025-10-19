package com.bmaster.createrns;

import com.bmaster.createrns.infrastructure.ServerConfig;
import com.mojang.logging.LogUtils;
import com.simibubi.create.foundation.data.CreateRegistrate;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(CreateRNS.MOD_ID)
public class CreateRNS {
    public static final String MOD_ID = "create_rns";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final CreateRegistrate REGISTRATE = CreateRegistrate.create(CreateRNS.MOD_ID);

    public CreateRNS(IEventBus bus, ModContainer container) {
        REGISTRATE.registerEventListeners(bus);
        RNSTags.register();
        RNSContent.register();
        RNSRecipeTypes.register();
        RNSRecipes.register();

        bus.addListener(this::commonSetup);
        bus.addListener(RNSSoundEvents::register);

        // Register our mod's ForgeConfigSpec so that Forge can create and load the config file for us
        container.registerConfig(ModConfig.Type.SERVER, ServerConfig.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {

    }
}
