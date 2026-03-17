package com.bmaster.createrns.event;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.RNSMisc;
import com.bmaster.createrns.content.deposit.info.DepositIndexProvider;
import com.bmaster.createrns.content.deposit.info.StructureDepositLocation;
import com.bmaster.createrns.data.pack.DynamicDatapackContent;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
@Mod.EventBusSubscriber(modid = CreateRNS.ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CommonForgeEvents {
    @SubscribeEvent
    public static void onAttachCaps(AttachCapabilitiesEvent<Level> event) {
        if (!(event.getObject() instanceof ServerLevel sl)) return;

        event.addCapability(
                ResourceLocation.fromNamespaceAndPath(CreateRNS.ID, "deposit_index"),
                new DepositIndexProvider(sl)
        );
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        var d = event.getDispatcher();
        d.register(RNSMisc.RNS_COMMAND);
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        // Resolve the final spacing value of the deposit structure set after all datapacks have been loaded
        var setKey = ResourceKey.create(Registries.STRUCTURE_SET, CreateRNS.asResource(
                DynamicDatapackContent.DEPOSIT_STRUCTURE_SET_NAME));
        var setHolder = event.getServer().registryAccess()
                .lookupOrThrow(Registries.STRUCTURE_SET)
                .get(setKey)
                .orElse(null);
        if (setHolder == null) {
            CreateRNS.LOGGER.error("Could not resolve structure set {}. Keeping deposit spacing at {}",
                    setKey.location(), StructureDepositLocation.getSpacing());
            return;
        }
        StructureSet set = setHolder.value();
        StructureDepositLocation.setSpacing(((RandomSpreadStructurePlacement) set.placement()).spacing());
        CreateRNS.LOGGER.info("Resolved deposit structure spacing from {}: {}",
                setKey.location(), StructureDepositLocation.getSpacing());
    }
}
