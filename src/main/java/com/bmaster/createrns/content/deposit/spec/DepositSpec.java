package com.bmaster.createrns.content.deposit.spec;

import com.bmaster.createrns.CreateRNS;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.structure.Structure;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Optional;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public record DepositSpec(Item scannerIconItem, ItemStack mapIconItem, ResourceLocation structure) {
    public static final Codec<DepositSpec> CODEC = RecordCodecBuilder.create(i -> i.group(
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("scanner_icon_item").forGetter(DepositSpec::scannerIconItem),
            BuiltInRegistries.ITEM.byNameCodec().optionalFieldOf("map_icon_item")
                    .forGetter(spec -> spec.mapIconItem.getItem() == spec.scannerIconItem
                            ? Optional.empty()
                            : Optional.of(spec.mapIconItem.getItem())),
            ResourceLocation.CODEC.fieldOf("structure").forGetter(DepositSpec::structure)
    ).apply(i, (scannerIconItem, mapIconItem, structure) ->
            new DepositSpec(scannerIconItem, new ItemStack(mapIconItem.orElse(scannerIconItem)), structure)));

    public static final ResourceKey<Registry<DepositSpec>> REGISTRY_KEY =
            ResourceKey.createRegistryKey(CreateRNS.asResource("deposit_spec"));

    public ResourceKey<Structure> structureKey() {
        return ResourceKey.create(Registries.STRUCTURE, structure);
    }
}
