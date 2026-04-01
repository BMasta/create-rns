package com.bmaster.createrns.content.deposit.mining.recipe;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.function.Function;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class MiningRecipeHelper {
    public static MiningRecipe fromJson(MapCodec<SerializedRecipe> codec, ResourceLocation id, JsonObject json) {
        return parseJson(codec, id, json).toRecipe(id);
    }

    public static MiningRecipe fromNetwork(MapCodec<SerializedRecipe> codec, ResourceLocation id, FriendlyByteBuf buf) {
        var tag = buf.readNbt();
        if (tag == null)
            throw new IllegalStateException("Failed to decode mining recipe " + id + " from network: missing payload");

        return parseNbt(codec, id, tag).toRecipe(id);
    }

    public static void toNetwork(MapCodec<SerializedRecipe> codec, FriendlyByteBuf buf, MiningRecipe recipe) {
        var payload = SerializedRecipe.fromRecipe(recipe);
        buf.writeNbt(encodeNbt(codec, recipe.getId(), payload));
    }

    private static SerializedRecipe parseJson(MapCodec<SerializedRecipe> codec, ResourceLocation id, JsonObject json) {
        return getOrThrow(
                codec.codec().parse(JsonOps.INSTANCE, json),
                error -> new JsonSyntaxException("Failed to parse mining recipe '" + id + "': " + error)
        );
    }

    private static SerializedRecipe parseNbt(MapCodec<SerializedRecipe> codec, ResourceLocation id, CompoundTag tag) {
        return getOrThrow(
                codec.codec().parse(NbtOps.INSTANCE, tag),
                error -> new IllegalStateException("Failed to decode mining recipe '" + id + "' from network: " + error)
        );
    }

    private static CompoundTag encodeNbt(MapCodec<SerializedRecipe> codec, ResourceLocation id, SerializedRecipe payload) {
        Tag encoded = getOrThrow(
                codec.codec().encodeStart(NbtOps.INSTANCE, payload),
                error -> new IllegalStateException("Failed to encode mining recipe '" + id + "' for network: " + error)
        );

        if (encoded instanceof CompoundTag compoundTag) {
            return compoundTag;
        }
        throw new IllegalStateException(
                "Failed to encode mining recipe '" + id + "' for network: codec did not produce a CompoundTag");
    }

    private static <T> T getOrThrow(DataResult<T> result, Function<String, RuntimeException> exceptionFactory) {
        var error = result.error().orElse(null);
        if (error != null) throw exceptionFactory.apply(error.message());

        var parsed = result.result().orElse(null);
        if (parsed != null) return parsed;

        throw exceptionFactory.apply("Unknown codec error");
    }

    public record SerializedRecipe(
            Block depositBlock, Block replacementBlock, DepositDurability dur, List<Yield> yields
    ) {
        public static SerializedRecipe fromRecipe(MiningRecipe recipe) {
            return new SerializedRecipe(
                    recipe.getDepositBlock(),
                    recipe.getReplacementBlock(),
                    recipe.getDurability(),
                    recipe.getYields()
            );
        }

        public MiningRecipe toRecipe(ResourceLocation id) {
            return new MiningRecipe(id, depositBlock, replacementBlock, dur, yields);
        }
    }
}
