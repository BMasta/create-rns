package com.bmaster.createrns.content.deposit.mining.recipe.catalyst.resonance;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.Catalyst;
import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.CatalystRequirement;
import com.bmaster.createrns.util.Utils;
import com.mojang.datafixers.util.Function3;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class AbstractResonanceCatalystRequirement extends CatalystRequirement {
    // One resonator per side directly adjacent to the drill head
    public static final int MAX_RESONATORS = 4;

    public static <T extends AbstractResonanceCatalystRequirement> Codec<T> codec(
            Function3<Float, Float, Integer, T> factory) {
        return RecordCodecBuilder.create(i -> i.group(
                        Codec.floatRange(0, Float.MAX_VALUE).fieldOf("base_chance")
                                .orElse(0f)
                                .forGetter(c -> c.baseChance),
                        Codec.floatRange(0, Float.MAX_VALUE).fieldOf("chance_per_resonator")
                                .orElse(0f)
                                .forGetter(c -> c.chancePerResonator),
                        Codec.intRange(0, Integer.MAX_VALUE).fieldOf("min_resonators")
                                .orElse(Integer.MAX_VALUE)
                                .forGetter(c -> c.minResonators))
                .apply(i, factory));
    }

    public static <T extends AbstractResonanceCatalystRequirement> Codec<T> streamCodec(
            Function3<Float, Float, Integer, T> factory) {
        return RecordCodecBuilder.create(i -> i.group(
                        Codec.FLOAT.fieldOf("base_chance").orElse(1f).forGetter(c -> c.baseChance),
                        Codec.FLOAT.fieldOf("chance_per_resonator").orElse(0f).forGetter(c -> c.chancePerResonator),
                        Codec.INT.fieldOf("min_resonators").orElse(Integer.MAX_VALUE).forGetter(c -> c.minResonators))
                .apply(i, factory));
    }

    public final float baseChance;
    public final float chancePerResonator;
    public final int minResonators;

    public AbstractResonanceCatalystRequirement(float baseChance, float chancePerResonator, int minResonators) {
        this.baseChance = baseChance;
        this.chancePerResonator = chancePerResonator;
        this.minResonators = minResonators;
    }

    @Override
    public boolean useCatalyst(Catalyst catalyst, boolean simulate) {
        return isSatisfiedBy(catalyst);
    }

    protected abstract String langKey();

    protected abstract ChatFormatting style();

    @Override
    public float getMaxChance() {
        return chancePerResonator * MAX_RESONATORS;
    }

    @Override
    public List<MutableComponent> jeiRequirementDescriptions() {
        List<MutableComponent> res = new ArrayList<>();
        // If the chance per resonator is already shown, at least 1 resonator is already implied
        // and doesn't have to be explained in the tooltip.
        var required = CreateRNS.lang()
                .translate("jei.catalyst.required").space()
                .add(Component.translatable(CreateRNS.ID + ".jei.catalyst.resonance." + langKey() + ".requirement")
                        .withStyle(style()))
                .style(ChatFormatting.WHITE);
        if (minResonators > 1) {
            required.space().translate("jei.catalyst.resonance.count", minResonators);
        }
        res.add(required.component());
        return res;
    }

    @Override
    public List<MutableComponent> jeiChanceDescriptions(float weightRatio) {
        List<MutableComponent> res = new ArrayList<>();
        if (chancePerResonator > 0) {
            res.add(CreateRNS.lang().translate("jei.catalyst.resonance." + langKey() + ".chance",
                    Utils.fancyChanceArg(chancePerResonator * weightRatio).style(style())).style(ChatFormatting.GRAY).component());
        }
        return res;
    }
}
