package com.bmaster.createrns.compat.ponder;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.RNSBlocks;
import com.tterrag.registrate.util.entry.ItemProviderEntry;
import com.tterrag.registrate.util.entry.RegistryEntry;
import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.registration.PonderTagRegistrationHelper;
import net.createmod.ponder.foundation.PonderIndex;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class RNSPonderPlugin implements PonderPlugin {
    public static void register() {
        PonderIndex.addPlugin(new RNSPonderPlugin());
    }

    @Override
    public String getModId() {
        return CreateRNS.ID;
    }

    @Override
    public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        PonderSceneRegistrationHelper<ItemProviderEntry<?>> helper1 = helper.withKeyFunction(RegistryEntry::getId);

        helper1.forComponents(RNSBlocks.MINER_BEARING_BLOCK, RNSBlocks.DRILL_HEAD_BLOCK)
                .addStoryBoard("mining", RNSPonderScenes::mining)
                .addStoryBoard("extracting", RNSPonderScenes::extracting);
    }

    @Override
    public void registerTags(PonderTagRegistrationHelper<ResourceLocation> helper) {
        PonderPlugin.super.registerTags(helper);
    }
}
