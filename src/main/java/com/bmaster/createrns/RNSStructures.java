package com.bmaster.createrns;

import com.bmaster.createrns.content.deposit.worldgen.DepositStructure;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Supplier;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class RNSStructures {
    private static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_TYPE, CreateRNS.ID);

    public static final Supplier<StructureType<DepositStructure>> DEPOSIT = STRUCTURE_TYPES.register(
            "deposit", () -> () -> DepositStructure.CODEC);

    public static void register(IEventBus modBus) {
        STRUCTURE_TYPES.register(modBus);
    }
}
