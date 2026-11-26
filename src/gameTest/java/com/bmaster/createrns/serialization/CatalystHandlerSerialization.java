package com.bmaster.createrns.serialization;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.content.deposit.mining.recipe.MiningRecipe;
import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.CatalystHandler;
import com.bmaster.createrns.util.CodecHelper;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Set;

@GameTestHolder(CreateRNS.ID)
@PrefixGameTestTemplate(false)
public class CatalystHandlerSerialization {
    @GameTest(template = "empty16x16")
    public void activeCatalystsAndEffectsSurviveRoundTrip(GameTestHelper helper) {
        var recipe = CodecHelper.assertParses(helper, MiningRecipe.CODEC.codec(), CodecHelper.registries(helper), """
                {"deposit_block": "minecraft:stone", "yields": [{"items": [{"item": "minecraft:diamond"}]}]}
                """, "single-yield mining recipe");
        var provider = helper.getLevel().registryAccess();
        var ids = new ListTag();
        ids.add(StringTag.valueOf("resonance"));
        ids.add(StringTag.valueOf("create_rns:overclock"));
        var effects = new CompoundTag();
        effects.putFloat("chance_multiplier", 0.35f);
        effects.putDouble("count_multiplier", 0d);
        var perYield = new CompoundTag();
        perYield.put("0", effects);
        var saved = new CompoundTag();
        saved.put("current_active_catalysts", ids);
        saved.put("current_catalyst_effects", perYield);

        var original = new CatalystHandler(recipe, Set.of());
        original.deserializeNBT(provider, saved);
        var restored = new CatalystHandler(recipe, Set.of());
        restored.deserializeNBT(provider, original.serializeNBT(provider));
        helper.assertValueEqual(restored.getCurrentActiveCRSes(),
                Set.of(CreateRNS.asResource("resonance"), CreateRNS.asResource("overclock")),
                "Qualified and unqualified active catalyst IDs");
        var result = restored.getCurrentCatalystEffects(0);
        CodecHelper.assertFloat(helper, result.chanceMult(), 0.35f, "restored chance multiplier");
        helper.assertValueEqual(result.countMult(), 0d, "an explicit zero count multiplier");

        restored.deserializeNBT(provider, new CompoundTag());
        helper.assertTrue(restored.getCurrentActiveCRSes().isEmpty(), "Empty state should recompute active catalysts");
        helper.assertValueEqual(restored.getCurrentCatalystEffects(0).countMult(), 1d,
                "Recomputed effects for an unconditional yield");
        helper.succeed();
    }
}
