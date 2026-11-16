package com.bmaster.createrns.util.codec;

import com.mojang.serialization.Codec;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ItemWithFallbacks extends ResourceWithFallbacks<Item> {
    public static final Codec<ItemWithFallbacks> LENIENT_CODEC = createCodec(ItemWithFallbacks::new, true);
    public static final Codec<ItemWithFallbacks> STRICT_CODEC = createCodec(ItemWithFallbacks::new, false);
    public static final Codec<ItemWithFallbacks> STRICT_RESOLVABLE_CODEC =
            STRICT_CODEC.validate(ResourceWithFallbacks::validateResolvable);
    public static final StreamCodec<RegistryFriendlyByteBuf, ItemWithFallbacks> STREAM_CODEC =
            createStreamCodec(Registries.ITEM, ItemWithFallbacks::new);

    public static final ItemWithFallbacks EMPTY = new ItemWithFallbacks(List.of(), true);

    protected ItemWithFallbacks(List<ResourceOrTagLocation> originalEntries, boolean encodedAsList) {
        this(Items.AIR, originalEntries, encodedAsList);
    }

    protected ItemWithFallbacks(
            Item value, List<ResourceOrTagLocation> originalEntries, boolean encodedAsList
    ) {
        super(value, originalEntries, encodedAsList);
    }

    @Override
    protected Registry<Item> registry() {
        return BuiltInRegistries.ITEM;
    }

    @Override
    protected Item emptyFallbackValue() {
        return Items.AIR;
    }

    @Override
    protected String resourceLogIdentifier() {
        return "item";
    }

    @Override
    protected Item resolveFromResourceStack(Item resource) {
        return new ItemStack(resource).getItem();
    }
}
