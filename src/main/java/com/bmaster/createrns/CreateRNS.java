package com.bmaster.createrns;

import com.bmaster.createrns.compat.ponder.RNSPonderPlugin;
import com.bmaster.createrns.content.deposit.mining.contraption.RNSMovementChecks;
import com.bmaster.createrns.content.deposit.scanning.DepositScannerChannel;
import com.bmaster.createrns.infrastructure.ServerConfig;
import com.mojang.logging.LogUtils;
import com.simibubi.create.foundation.data.CreateRegistrate;
import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

import javax.annotation.ParametersAreNonnullByDefault;

@Mod(CreateRNS.ID)
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class CreateRNS {
    public static final String ID = "create_rns";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final CreateRegistrate REGISTRATE = CreateRegistrate.create(CreateRNS.ID);

    public static ResourceLocation asResource(String path) {
        return ResourceLocation.fromNamespaceAndPath(ID, path);
    }

    public static LangBuilder lang() {
        return new LangBuilder(CreateRNS.ID);
    }

    public static MutableComponent translatable(String langSuffix) {
        return Component.translatable(asLangEntry(langSuffix));
    }

    public static MutableComponent translatable(String langSuffix, Object... args) {
        return Component.translatable(asLangEntry(langSuffix), args);
    }

    public static MutableComponent vanillaTranslatable(String langPrefix, String langSuffix) {
        return Component.translatable(asLangEntry(langPrefix, langSuffix));
    }

    public static MutableComponent vanillaTranslatable(String langPrefix, String langSuffix, Object... args) {
        return Component.translatable(asLangEntry(langPrefix, langSuffix), args);
    }

    public static String asLangEntry(String langSuffix) {
        return ID + "." + langSuffix;
    }

    public static String asLangEntry(String prefix, String path) {
        return prefix + "." + ID + "." + path;
    }

    public CreateRNS(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();
        REGISTRATE.registerEventListeners(modEventBus);
        RNSMisc.register();
        RNSPartialModels.register();
        RNSParticleTypes.register(modEventBus);
        RNSTags.register();
        RNSMovementChecks.register();
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
