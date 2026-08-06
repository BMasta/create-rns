package com.bmaster.createrns.compat.kubejs;

import com.bmaster.createrns.CreateRNS;
import com.google.gson.JsonObject;
import dev.latvian.mods.kubejs.recipe.RecipeTypeFunction;
import dev.latvian.mods.kubejs.recipe.RecipesKubeEvent;
import dev.latvian.mods.kubejs.recipe.schema.RecipeNamespace;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchemaType;
import dev.latvian.mods.kubejs.script.SourceLine;
import dev.latvian.mods.kubejs.util.RegistryAccessContainer;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import sun.misc.Unsafe;

import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.reflect.Field;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public final class MiningRecipeKubeRecipeTestHelper {
    private static final Unsafe UNSAFE = lookupUnsafe();
    private static final RecipeSchemaType SCHEMA_TYPE = new RecipeSchemaType(
            new RecipeNamespace(null, CreateRNS.ID),
            CreateRNS.asResource("mining"),
            MiningRecipeKubeSchema.schema()
    );
    private static final RecipeTypeFunction TYPE = new RecipeTypeFunction(createFakeEvent(), SCHEMA_TYPE);

    public static MiningRecipeKubeRecipe create(String id) {
        var recipe = (MiningRecipeKubeRecipe) SCHEMA_TYPE.schema.recipeFactory.create(TYPE, SourceLine.UNKNOWN, true);
        recipe.id = testId(id);
        recipe.json = new JsonObject();
        recipe.newRecipe = true;
        return recipe;
    }

    private static RecipesKubeEvent createFakeEvent() {
        var event = allocate(RecipesKubeEvent.class);
        putObject(RecipesKubeEvent.class, event, "registries", RegistryAccessContainer.BUILTIN);
        putObject(RecipesKubeEvent.class, event, "ops", RegistryAccessContainer.BUILTIN);
        return event;
    }

    @SuppressWarnings("unchecked")
    private static <T> T allocate(Class<T> type) {
        try {
            return (T) UNSAFE.allocateInstance(type);
        } catch (InstantiationException e) {
            throw new IllegalStateException("Failed to allocate " + type.getName(), e);
        }
    }

    private static void putObject(Class<?> owner, Object target, String fieldName, Object value) {
        try {
            var field = owner.getDeclaredField(fieldName);
            var offset = UNSAFE.objectFieldOffset(field);
            UNSAFE.putObject(target, offset, value);
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException("Missing field " + owner.getName() + "." + fieldName, e);
        }
    }

    private static Unsafe lookupUnsafe() {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            return (Unsafe) field.get(null);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to access Unsafe", e);
        }
    }

    private static ResourceLocation testId(String id) {
        return CreateRNS.asResource("kubejs_builder_test/" + id);
    }

    private MiningRecipeKubeRecipeTestHelper() {
    }
}
