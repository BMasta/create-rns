package com.bmaster.createrns.datagen;

import com.bmaster.createrns.datagen.recipe.RNSSequencedAssemblyRecipeGen;
import com.simibubi.create.foundation.data.recipe.CreateRecipeProvider;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraftforge.data.event.GatherDataEvent;

public class RNSDatagen {
    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        generator.addProvider(event.includeServer(), new RNSSequencedAssemblyRecipeGen(output));
    }
}
