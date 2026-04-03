package com.bmaster.createrns.content.deposit.spec;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.util.codec.ItemWithFallbacks;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.structure.Structure;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class DepositSpec {
    public static final Codec<DepositSpec> CODEC = RecordCodecBuilder.create(i -> i.group(
            ItemWithFallbacks.STRICT_CODEC.fieldOf("scanner_icon_item")
                    .forGetter(ds -> ds.scannerIconItemData),
            ItemWithFallbacks.STRICT_CODEC.fieldOf("map_icon_item")
                    .forGetter(ds -> ds.mapIconItemData),
            ResourceLocation.CODEC.fieldOf("structure")
                    .forGetter(ds -> ds.structure)
    ).apply(i, DepositSpec::new));

    public static final ResourceKey<Registry<DepositSpec>> REGISTRY_KEY =
            ResourceKey.createRegistryKey(CreateRNS.asResource("deposit_spec"));

    public final ResourceLocation structure;
    protected final ItemWithFallbacks scannerIconItemData;
    protected final ItemWithFallbacks mapIconItemData;
    protected ItemStack mapIcon;

    public DepositSpec(
            ItemWithFallbacks scannerIconItemData, ItemWithFallbacks mapIconItemData, ResourceLocation structure
    ) {
        this.structure = structure;
        this.scannerIconItemData = scannerIconItemData;
        this.mapIconItemData = mapIconItemData;
    }

    public ResourceKey<Structure> structureKey() {
        return ResourceKey.create(Registries.STRUCTURE, structure);
    }

    public boolean initialize(RegistryAccess access) {
        if (!scannerIconItemData.resolve(access, false)) return false;
        if (!mapIconItemData.resolve(access, false)) return false;
        this.mapIcon = new ItemStack(mapIconItemData.item);
        return scannerIconItemData.item != Items.AIR && mapIconItemData.item != Items.AIR;
    }

    public @Nullable Item getScannerIcon() {
        return (scannerIconItemData.item != Items.AIR) ? scannerIconItemData.item : null;
    }

    public ItemStack getMapIcon() {
        return mapIcon;
    }
}
