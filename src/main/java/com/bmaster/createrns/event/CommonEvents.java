package com.bmaster.createrns.event;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.RNSContent;
import com.bmaster.createrns.RNSRecipes;
import com.bmaster.createrns.data.gen.depositworldgen.DepositWorldgenProvider;
import com.bmaster.createrns.data.pack.DynamicDatapack;
import com.bmaster.createrns.deposit.DepositSpec;
import com.bmaster.createrns.deposit.DepositSpecLookup;
import com.bmaster.createrns.item.DepositScanner.DepositScannerC2SPayload;
import com.bmaster.createrns.item.DepositScanner.DepositScannerS2CPayload;
import com.bmaster.createrns.mining.miner.MinerBlockEntity;
import com.bmaster.createrns.mining.miner.MinerSpec;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;

import javax.annotation.Nullable;

@EventBusSubscriber(modid = CreateRNS.MOD_ID)
public class CommonEvents {
    // MOD BUS
    @SubscribeEvent
    public static void onRegisterCapabilities(RegisterCapabilitiesEvent e) {
        // Miners
        e.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                RNSContent.MINER_BE.get(),
                (MinerBlockEntity be, @Nullable Direction side) -> be.getItemHandler(side)
        );
    }

    @SubscribeEvent
    public static void onNewRegistry(DataPackRegistryEvent.NewRegistry e) {
        e.dataPackRegistry(DepositSpec.REGISTRY_KEY, DepositSpec.CODEC, DepositSpec.CODEC);
        e.dataPackRegistry(MinerSpec.REGISTRY_KEY, MinerSpec.CODEC, MinerSpec.CODEC);
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
    public static void onRegisterAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(ModelResourceLocation.standalone(RNSContent.MINER_MK1_DRILL.modelLocation()));
        event.register(ModelResourceLocation.standalone(RNSContent.MINER_MK2_DRILL.modelLocation()));
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
        generator.addProvider(event.includeServer(), new RNSRecipes.Washing(output, provider));
        generator.addProvider(event.includeServer(), new DepositWorldgenProvider(output));
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        var d = event.getDispatcher();
        d.register(RNSContent.RNS_COMMAND);
    }

    // GAME BUS

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load e) {
        if (!(e.getLevel() instanceof ServerLevel sl)) return;

        var depData = sl.getData(RNSContent.LEVEL_DEPOSIT_DATA.get());

        ChunkPos pos = e.getChunk().getPos();
        var sm = sl.structureManager();

        for (var start : sm.startsForStructure(pos, DepositSpecLookup.isDeposit(sl.registryAccess()))) {
            sl.registryAccess()
                    .registryOrThrow(Registries.STRUCTURE)
                    .getResourceKey(start.getStructure())
                    .ifPresent(structKey -> depData.addDeposit(structKey, start));
        }
    }
}
