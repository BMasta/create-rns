package com.bmaster.createrns;

import com.bmaster.createrns.content.deposit.mining.contraption.RNSMovementChecks;
import com.bmaster.createrns.infrastructure.ServerConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;
import com.simibubi.create.foundation.data.CreateRegistrate;
import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

import javax.annotation.ParametersAreNonnullByDefault;

@Mod(CreateRNS.ID)
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class CreateRNS {
    public static final String ID = "create_rns";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final CreateRegistrate REGISTRATE = CreateRegistrate.create(CreateRNS.ID);
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

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

    public CreateRNS(ModContainer container, IEventBus modBus) {
        REGISTRATE.registerEventListeners(modBus);
        RNSMisc.register(modBus);
        RNSPartialModels.register();
        RNSParticleTypes.register(modBus);
        RNSTags.register();
        RNSMovementChecks.register();
        RNSDeposits.register();
        RNSBlocks.register();
        RNSItems.register();
        RNSBlockEntities.register();
        RNSRecipeTypes.register();
        RNSRecipes.register();
        RNSPacks.register();
        modBus.addListener(RNSSoundEvents::register);
        container.registerConfig(ModConfig.Type.SERVER, ServerConfig.SPEC);
    }
}
