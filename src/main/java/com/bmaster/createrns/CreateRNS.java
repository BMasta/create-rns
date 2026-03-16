package com.bmaster.createrns;

import com.bmaster.createrns.compat.ponder.RNSPonderPlugin;
import com.bmaster.createrns.content.deposit.scanning.DepositScannerChannel;
import com.bmaster.createrns.infrastructure.ServerConfig;
import com.mojang.logging.LogUtils;
import com.simibubi.create.foundation.data.CreateRegistrate;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(CreateRNS.MOD_ID)
public class CreateRNS {
    public static final String MOD_ID = "create_rns";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final CreateRegistrate REGISTRATE = CreateRegistrate.create(CreateRNS.MOD_ID);

    public CreateRNS(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();
        REGISTRATE.registerEventListeners(modEventBus);
        RNSMisc.register();
        RNSPartialModels.register();
        RNSParticleTypes.register(modEventBus);
        RNSTags.register();
        RNSBlocks.register();
        RNSItems.register();
        RNSBlockEntities.register();
        RNSRecipeTypes.register();
        RNSRecipes.register();
        RNSPacks.register();
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> RNSPonderPlugin::register);

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(RNSSoundEvents::register);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);

        // Register our mod's ForgeConfigSpec so that Forge can create and load the config file for us
        context.registerConfig(ModConfig.Type.SERVER, ServerConfig.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(DepositScannerChannel::init);
    }
}
