package com.bmaster.createrns.data.deposit;

import com.bmaster.createrns.content.deposit.mining.recipe.DepositDurability;
import com.bmaster.createrns.data.deposit.DepositBlockBuilder.DepositBuildingContext;
import com.bmaster.createrns.data.deposit.DepositData.YieldEntry;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class MiningRecipeBuilder {
    public static MiningRecipeBuilder create(DepositBuildingContext ctx) {
        return new MiningRecipeBuilder(ctx);
    }

    public ResourceKey<Level> dimension = Level.OVERWORLD;
    public final List<YieldEntry> yields = new ArrayList<>();
    public @Nullable ResourceLocation replacementBlockId;
    public @Nullable DepositDurability durability;

    public MiningRecipeBuilder dimension(ResourceKey<Level> dimension) {
        this.dimension = dimension;
        return this;
    }

    public MiningRecipeBuilder replaceWhenDepleted(String blockId) {
        replacementBlockId = ResourceLocation.parse(blockId);
        return this;
    }

    public MiningRecipeBuilder durability(long core, long edge, float randomSpread) {
        if (core <= 0) throw new IllegalArgumentException("Core durability must be positive");
        if (edge <= 0) throw new IllegalArgumentException("Edge durability must be positive");
        if (randomSpread < 0 || randomSpread > 1) {
            throw new IllegalArgumentException("Random spread must be between 0 and 1");
        }

        durability = new DepositDurability(core, edge, randomSpread);
        return this;
    }

    public MiningRecipeBuilder yield(Consumer<YieldBuilder> yield) {
        var builder = new YieldBuilder();
        yield.accept(builder);
        // Skip adding an empty yield
        if (builder.items.isEmpty()) return this;
        yields.add(new YieldEntry(builder.chance, List.copyOf(builder.items),
                List.copyOf(builder.catalysts), builder.jeiSlotColor));
        return this;
    }

    public MiningRecipeBuilder transform(UnaryOperator<MiningRecipeBuilder> transform) {
        return transform.apply(this);
    }

    private MiningRecipeBuilder(DepositBuildingContext ctx) {
        ctx.recipeBuilders.add(this);
    }
}
