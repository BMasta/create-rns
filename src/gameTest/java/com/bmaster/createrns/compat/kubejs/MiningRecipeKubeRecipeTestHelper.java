package com.bmaster.createrns.compat.kubejs;

import com.bmaster.createrns.CreateRNS;
import com.google.gson.JsonObject;
import dev.latvian.mods.kubejs.recipe.RecipeTypeFunction;
import dev.latvian.mods.kubejs.recipe.schema.RecipeNamespace;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchemaType;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public final class MiningRecipeKubeRecipeTestHelper {
    private static final RecipeSchemaType SCHEMA_TYPE = new RecipeSchemaType(
            new RecipeNamespace(CreateRNS.ID),
            CreateRNS.asResource("mining"),
            MiningRecipeKubeSchema.schema()
    );
    private static final RecipeTypeFunction TYPE = new RecipeTypeFunction(null, SCHEMA_TYPE);

    public static MiningRecipeKubeRecipe create(String id) {
        var recipe = (MiningRecipeKubeRecipe) SCHEMA_TYPE.schema.factory.get();
        recipe.type = TYPE;
        recipe.id = testId(id);
        recipe.json = new JsonObject();
        recipe.json.addProperty("type", TYPE.idString);
        recipe.newRecipe = true;
        recipe.initValues(true);
        return recipe;
    }

    private static ResourceLocation testId(String id) {
        return CreateRNS.asResource("kubejs_builder_test/" + id);
    }

    private MiningRecipeKubeRecipeTestHelper() {
    }
}
