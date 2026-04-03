package com.bmaster.createrns.compat.jei;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.RNSBlocks;
import com.bmaster.createrns.RNSRecipeTypes;
import com.bmaster.createrns.content.deposit.mining.recipe.MiningRecipe;
import com.bmaster.createrns.content.deposit.mining.recipe.MiningRecipeLookup;
import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.CatalystRequirementSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Comparator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@JeiPlugin
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class RNSJEI implements IModPlugin {
    public static final ResourceLocation ID = CreateRNS.asResource("jei_plugin");

    private static Object2ObjectOpenHashMap<ResourceKey<Level>, MiningRecipeCategory> categories;

    @Override
    public ResourceLocation getPluginUid() {
        return ID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration reg) {
        var level = Minecraft.getInstance().level;
        if (level == null) {
            categories = new Object2ObjectOpenHashMap<>();
            return;
        }

        categories = MiningRecipeLookup.getAllRelevantDimensions(level).stream().collect(Collectors.toMap(
                d -> d,
                d -> {
                    var dimRL = d.location();
                    var dimSuffix = (d == Level.OVERWORLD ? "" : "_" + dimRL.getNamespace() + "_" + dimRL.getPath());
                    return new MiningRecipeCategory(
                            reg.getJeiHelpers().getGuiHelper(),
                            RecipeType.create(CreateRNS.ID, "mining" + dimSuffix, MiningRecipe.class));
                },
                (o, n) -> o,
                Object2ObjectOpenHashMap::new
        ));

        for (var category : categories.values()) {
            reg.addRecipeCategories(category);
        }

    }

    @Override
    public void registerRecipes(IRecipeRegistration reg) {
        registerMiningRecipes(reg);
        registerCatalystInfoPages(reg);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration reg) {
        for (var category : categories.values()) {
            for (var cs : MiningRecipeCategory.CATALYSTS) {
                reg.addRecipeCatalyst(cs.get(), category.getRecipeType());
            }
        }
    }

    private static void registerMiningRecipes(IRecipeRegistration reg) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;

        var recipes = level.getRecipeManager().getAllRecipesFor(RNSRecipeTypes.MINING_RECIPE_TYPE.get());
        for (var e : categories.object2ObjectEntrySet()) {
            var dim = e.getKey();
            var category = e.getValue();
            reg.addRecipes(category.getRecipeType(), recipes.stream()
                    .filter(r -> r.getDimension() == dim)
                    .toList());
        }
    }

    private static void registerCatalystInfoPages(IRecipeRegistration reg) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;

        var crsRegistry = level.registryAccess().registryOrThrow(CatalystRequirementSet.REGISTRY_KEY);
        var crsList = crsRegistry.stream()
                .sorted(Comparator.comparingInt(crs -> crs.displayPriority))
                .toList();
        for (var crs : crsList) {
            reg.addItemStackInfo(
                    Stream.concat(Stream.of(RNSBlocks.MINER_BEARING.get().asItem(), RNSBlocks.MINE_HEAD.asItem()),
                                    crs.representativeItems.stream())
                            .distinct()
                            .map(ItemStack::new)
                            .toList(),
                    crs.getNameComponent()
                            .append("\n")
                            .append(CreateRNS.translatable("catalyst." + crs.name + ".description"))
                            .append("\n\n")
            );
        }
    }
}
