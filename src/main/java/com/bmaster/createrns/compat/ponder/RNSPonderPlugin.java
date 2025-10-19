//package com.bmaster.createrns.compat.ponder;
//
//import com.bmaster.createrns.CreateRNS;
//import com.bmaster.createrns.RNSContent;
//import com.tterrag.registrate.util.entry.ItemProviderEntry;
//import com.tterrag.registrate.util.entry.RegistryEntry;
//import net.createmod.ponder.api.registration.PonderPlugin;
//import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
//import net.createmod.ponder.api.registration.PonderTagRegistrationHelper;
//import net.createmod.ponder.foundation.PonderIndex;
//import net.minecraft.MethodsReturnNonnullByDefault;
//import net.minecraft.resources.ResourceLocation;
//
//import javax.annotation.ParametersAreNonnullByDefault;
//
//@MethodsReturnNonnullByDefault
//@ParametersAreNonnullByDefault
//public class RNSPonderPlugin implements PonderPlugin {
//    public static void register() {
//        PonderIndex.addPlugin(new RNSPonderPlugin());
//    }
//
//    @Override
//    public String getModId() {
//        return CreateRNS.MOD_ID;
//    }
//
//    @Override
//    public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
//        PonderSceneRegistrationHelper<ItemProviderEntry<?>> HELPER = helper.withKeyFunction(RegistryEntry::getId);
//        HELPER.forComponents(RNSContent.MINER_MK1_BLOCK, RNSContent.MINER_MK2_BLOCK)
//                .addStoryBoard("mining", RNSPonderScenes::mining);
//    }
//
//    @Override
//    public void registerTags(PonderTagRegistrationHelper<ResourceLocation> helper) {
//        PonderPlugin.super.registerTags(helper);
//    }
//}
