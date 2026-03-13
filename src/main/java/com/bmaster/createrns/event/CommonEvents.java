package com.bmaster.createrns.event;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.RNSMisc;
import com.bmaster.createrns.RNSRecipes;
import com.bmaster.createrns.content.deposit.info.StructureDepositLocation;
import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.CatalystRequirementSet;
import com.bmaster.createrns.content.deposit.scanning.DepositScannerC2SPayload;
import com.bmaster.createrns.content.deposit.scanning.DepositScannerS2CPayload;
import com.bmaster.createrns.content.deposit.spec.DepositSpec;
import com.bmaster.createrns.data.gen.depositworldgen.DepositWorldgenProvider;
import com.bmaster.createrns.data.pack.DynamicDatapack;
import com.bmaster.createrns.data.pack.DynamicDatapackContent;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;

import javax.annotation.ParametersAreNonnullByDefault;

@EventBusSubscriber(modid = CreateRNS.ID)
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class CommonEvents {
    // MOD BUS
    @SubscribeEvent
    public static void onNewRegistry(DataPackRegistryEvent.NewRegistry e) {
        e.dataPackRegistry(DepositSpec.REGISTRY_KEY, DepositSpec.CODEC, DepositSpec.CODEC);
        e.dataPackRegistry(CatalystRequirementSet.REGISTRY_KEY, CatalystRequirementSet.CODEC, CatalystRequirementSet.CODEC);
    }

    @SubscribeEvent
    public static void onAddPackFinders(AddPackFindersEvent e) {
        if (e.getPackType() == PackType.SERVER_DATA) {
            for (var pack : DynamicDatapack.DATAPACKS) {
                e.addRepositorySource(consumer -> consumer.accept(pack));
            }
        }
        if (e.getPackType() == PackType.CLIENT_RESOURCES) {
            for (var pack : DynamicDatapack.RESOURCE_PACKS) {
                e.addRepositorySource(consumer -> consumer.accept(pack));
            }
        }
    }

    @SubscribeEvent
    public static void onRegisterPayloadHandlers(final RegisterPayloadHandlersEvent event) {
        // set a network version string (optional but recommended)
        PayloadRegistrar registrar = event.registrar("1");

        // Client->Server
        registrar.playToServer(
                DepositScannerC2SPayload.TYPE,
                DepositScannerC2SPayload.STREAM_CODEC,
                DepositScannerC2SPayload::handle
        );

        registrar.playToClient(
                DepositScannerS2CPayload.TYPE,
                DepositScannerS2CPayload.STREAM_CODEC,
                DepositScannerS2CPayload::handle
        );
    }

    @SubscribeEvent
    public static void onGatherData(GatherDataEvent event) {
        var generator = event.getGenerator();
        var output = generator.getPackOutput();
        var provider = event.getLookupProvider();
        generator.addProvider(event.includeServer(), new RNSRecipes.MechanicalCrafting(output, provider));
        generator.addProvider(event.includeServer(), new RNSRecipes.Polishing(output, provider));
        generator.addProvider(event.includeServer(), new DepositWorldgenProvider(output));
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        var d = event.getDispatcher();
        d.register(RNSMisc.RNS_COMMAND);
    }

    // GAME BUS

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
