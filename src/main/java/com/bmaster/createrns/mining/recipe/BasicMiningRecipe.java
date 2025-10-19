//package com.bmaster.createrns.mining.recipe;
//
//import com.bmaster.createrns.RNSRecipeTypes;
//import com.google.gson.JsonObject;
//import net.minecraft.network.FriendlyByteBuf;
//import net.minecraft.resources.ResourceLocation;
//import net.minecraft.world.item.Item;
//import net.minecraft.world.item.ItemStack;
//import net.minecraft.world.item.crafting.RecipeSerializer;
//import net.minecraft.world.item.crafting.RecipeType;
//import net.minecraft.world.level.block.Block;
//import net.minecraftforge.registries.ForgeRegistries;
//import org.jetbrains.annotations.NotNull;
//
//import javax.annotation.ParametersAreNonnullByDefault;
//import java.util.Objects;
//
//public class BasicMiningRecipe extends MiningRecipe {
//    public BasicMiningRecipe(ResourceLocation id, Block depositBlock, Item yield) {
//        super(id, depositBlock, yield);
//    }
//
//    @Override
//    public @NotNull RecipeSerializer<?> getSerializer() {
//        return RNSRecipeTypes.BASIC_MINING_SERIALIZER.get();
//    }
//
//    @Override
//    public @NotNull RecipeType<?> getType() {
//        return RNSRecipeTypes.BASIC_MINING_TYPE.get();
//    }
//
//    @SuppressWarnings("SameParameterValue")
//    @ParametersAreNonnullByDefault
//    public static class Serializer extends MiningRecipe.Serializer<BasicMiningRecipe> {
//        @Override
//        public @NotNull BasicMiningRecipe fromJson(ResourceLocation id, JsonObject json) {
//            var depBlock = parseBlockId(json, "deposit_block");
//            var yield = parseItemId(json, "yield");
//            return new BasicMiningRecipe(id, depBlock, yield);
//        }
//
//        @Override
//        public BasicMiningRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
//            var depBlock = Objects.requireNonNull(ForgeRegistries.BLOCKS.getValue(buf.readResourceLocation()));
//            var yield = buf.readItem();
//            return new BasicMiningRecipe(id, depBlock, yield.getItem());
//        }
//
//        @Override
//        public void toNetwork(FriendlyByteBuf buf, BasicMiningRecipe r) {
//            buf.writeResourceLocation(Objects.requireNonNull(ForgeRegistries.BLOCKS.getKey(r.getDepositBlock())));
//            buf.writeItem(new ItemStack(r.getYield()));
//        }
//    }
//}
