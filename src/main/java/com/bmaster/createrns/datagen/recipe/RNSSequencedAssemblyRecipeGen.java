package com.bmaster.createrns.datagen.recipe;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.RNSContent;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.api.data.recipe.SequencedAssemblyRecipeGen;
import com.simibubi.create.content.kinetics.deployer.DeployerApplicationRecipe;
import net.minecraft.data.PackOutput;

public class RNSSequencedAssemblyRecipeGen extends SequencedAssemblyRecipeGen {
//    GeneratedRecipe MINER_MK1 = create("miner_mk1", b -> b
//            .require(AllBlocks.MECHANICAL_DRILL)
//            .transitionTo(RNSContent.INCOMPLETE_MINER_MK1)
//            .addOutput(RNSContent.MINER_MK1_BLOCK, 1)
//            .loops(1)
//            .addStep(DeployerApplicationRecipe::new, rb -> rb.require(AllBlocks.COGWHEEL))
//            .addStep(DeployerApplicationRecipe::new, rb -> rb.require(AllItems.ELECTRON_TUBE))
//            .addStep(DeployerApplicationRecipe::new, rb -> rb.require(AllBlocks.ANDESITE_FUNNEL)));

    public RNSSequencedAssemblyRecipeGen(PackOutput output) {
        super(output, CreateRNS.MOD_ID);
    }
}
