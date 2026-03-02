package com.bmaster.createrns.compat.jei;

import com.bmaster.createrns.*;
import com.bmaster.createrns.content.deposit.mining.recipe.MiningRecipe;
import com.bmaster.createrns.content.deposit.mining.recipe.Yield;
import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.CatalystRequirement;
import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.CatalystRequirementSetLookup;
import com.bmaster.createrns.util.FlexibleLayoutHelper;
import com.bmaster.createrns.util.Utils;
import com.simibubi.create.compat.jei.EmptyBackground;
import com.simibubi.create.compat.jei.ItemIcon;
import com.simibubi.create.compat.jei.category.CreateRecipeCategory;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotRichTooltipCallback;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class MiningRecipeCategory extends CreateRecipeCategory<MiningRecipe> {
    public static final RecipeType<MiningRecipe> JEI_RECIPE_TYPE = RecipeType.create(
            CreateRNS.ID, "mining", MiningRecipe.class);

    private static final Info<MiningRecipe> INFO = new Info<>(
            JEI_RECIPE_TYPE, Component.translatable(CreateRNS.ID + ".recipe.mining"),
            new EmptyBackground(177, 140),
            new ItemIcon(() -> new ItemStack(RNSBlocks.MINER_MK1_BLOCK)),
            (() -> {
                var level = Minecraft.getInstance().level;
                if (level == null) return List.of();
                return level.getRecipeManager().getAllRecipesFor(RNSRecipeTypes.MINING_RECIPE_TYPE.get());
            }),
            List.of(() -> new ItemStack(RNSBlocks.MINER_MK1_BLOCK.get().asItem()))
    );

    private static final SlotBg SLOT = new SlotBg(asDrawable(RNSGuiTextures.JEI_SLOT), asDrawable(RNSGuiTextures.JEI_CHANCE_SLOT));
    private static final SlotBg RESONANCE_SLOT = new SlotBg(asDrawable(RNSGuiTextures.JEI_RESONANCE_SLOT), asDrawable(RNSGuiTextures.JEI_RESONANCE_CHANCE_SLOT));
    private static final SlotBg SHATTERING_SLOT = new SlotBg(asDrawable(RNSGuiTextures.JEI_SHATTERING_RESONANCE_SLOT), asDrawable(RNSGuiTextures.JEI_SHATTERING_RESONANCE_CHANCE_SLOT));
    private static final SlotBg STABILIZING_SLOT = new SlotBg(asDrawable(RNSGuiTextures.JEI_STABILIZING_RESONANCE_SLOT), asDrawable(RNSGuiTextures.JEI_STABILIZING_RESONANCE_CHANCE_SLOT));

    private static final Map<String, SlotBg> crsToBg = Map.of(
            "half_resonance", RESONANCE_SLOT,
            "full_resonance", RESONANCE_SLOT,
            "shattering_resonance", SHATTERING_SLOT,
            "stabilizing_resonance", STABILIZING_SLOT
    );

    private static final AnimatedMiner MINER = new AnimatedMiner(RNSBlocks.MINER_MK1_BLOCK.get(), RNSPartialModels.MINER_MK1_DRILL);

    private static final int SLOTS_PER_ROW = 9;
    private static final int MAX_ROWS = 3;
    private static final int MAX_SLOTS = SLOTS_PER_ROW * MAX_ROWS;

    protected static IDrawable asDrawable(RNSGuiTextures texture) {
        return new IDrawable() {
            @Override
            public int getWidth() {
                return texture.getWidth();
            }

            @Override
            public int getHeight() {
                return texture.getHeight();
            }

            @Override
            public void draw(GuiGraphics graphics, int xOffset, int yOffset) {
                texture.render(graphics, xOffset, yOffset);
            }
        };
    }

    protected static IRecipeSlotRichTooltipCallback addStochasticTooltip(MinerOutput output) {
        return (view, tooltip) -> {
            var conn = Minecraft.getInstance().getConnection();
            var access = (conn != null) ? conn.registryAccess() : RegistryAccess.EMPTY;

            // If yield contains more than one item, print how many items are in the same group
            if (output.itemsInGroup > 1) {
                tooltip.add(CreateRNS.lang().translate("jei.item_in_group", output.itemsInGroup).component());
            }

            // Add requirement descriptions
            for (var crsName : output.requirements) {
                for (var cr : CatalystRequirementSetLookup.get(access, crsName).requirements) {
                    for (var c : cr.JEIRequirementDescriptions()) {
                        tooltip.add(c);
                    }
                }
            }

            // No tooltips needed. Item is always mined.
            if (output.chance * output.weightRatio == 1) return;

            Function<LangBuilder, LangBuilder> forGroup = l -> output.weightRatio == 1 ? l :
                    l.space().translate("jei.for_group");
            Function<LangBuilder, LangBuilder> forItem = l ->
                    l.space().translate("jei.for_item");

            float maxChance = output.chance + output.requirements.stream()
                    .flatMap(crsName -> CatalystRequirementSetLookup.get(access, crsName).requirements.stream())
                    .map(CatalystRequirement::getMaxChance)
                    .reduce(Float::sum)
                    .orElse(0f);

            var minChanceGroup = Utils.fancyChanceArg(output.chance).style(ChatFormatting.GOLD);
            var maxChanceGroup = Utils.fancyChanceArg(maxChance).style(ChatFormatting.YELLOW).style(ChatFormatting.UNDERLINE);
            var minChanceItem = Utils.fancyChanceArg(output.chance * output.weightRatio).style(ChatFormatting.GOLD);
            var maxChanceItem = Utils.fancyChanceArg(maxChance * output.weightRatio).style(ChatFormatting.YELLOW).style(ChatFormatting.UNDERLINE);

            // Min chance for pool
            tooltip.add(forGroup.apply(CreateRNS.lang()
                    .translate("jei.chance", minChanceGroup)
                    .style(ChatFormatting.GRAY)).component());

            // Added chances from catalysts
            int descriptionsAdded = 0;
            for (var crsName : output.requirements) {
                for (var cr : CatalystRequirementSetLookup.get(access, crsName).requirements) {
                    for (var c : cr.JEIChanceDescriptions(1)) {
                        descriptionsAdded++;
                        tooltip.add(Component.literal("  ").append(c));
                    }
                }
            }

            // Max chance for group
            if (descriptionsAdded > 0 && output.chance != maxChance) {
                tooltip.add(CreateRNS.lang()
                        .text("  ")
                        .translate("jei.max_chance", maxChanceGroup)
                        .style(ChatFormatting.GRAY).component());
            }

            // Group/item chances are equivalent
            if (output.weightRatio == 1) return;

            // Min chance for item
            tooltip.add(forItem.apply(CreateRNS.lang()
                    .translate("jei.chance", minChanceItem)
                    .style(ChatFormatting.GRAY)).component());

            // Added chances from catalysts
            descriptionsAdded = 0;
            for (var crsName : output.requirements) {
                for (var cr : CatalystRequirementSetLookup.get(access, crsName).requirements) {
                    for (var c : cr.JEIChanceDescriptions(output.weightRatio)) {
                        descriptionsAdded++;
                        tooltip.add(Component.literal("  ").append(c));
                    }
                }
            }

            // Max chance for item
            if (descriptionsAdded > 0 && output.chance != maxChance) {
                tooltip.add(CreateRNS.lang()
                        .text("  ")
                        .translate("jei.max_chance", maxChanceItem)
                        .style(ChatFormatting.GRAY).component());
            }
        };
    }

    public MiningRecipeCategory(IGuiHelper gui) {
        super(INFO);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, MiningRecipe recipe, IFocusGroup focuses) {
        // Mined deposit block
        builder.addSlot(RecipeIngredientRole.INPUT, 80, 50) //60
                .addItemStack(new ItemStack(recipe.getDepositBlock().asItem()));

        // Guarantees non-empty sections
        var slots = outputLayout(recipe);
        int yOffset = slots.get(slots.size() - 1).y;

        for (var slot : slots) {
            var bg = slotBg(slot.output);
            builder.addSlot(RecipeIngredientRole.OUTPUT, 89 + slot.x, 120 - yOffset + slot.y)
                    .setBackground(bg, -1, -1)
                    .addRichTooltipCallback(addStochasticTooltip(slot.output))
                    .addItemStack(new ItemStack(slot.output.item, slot.output.count));
        }
    }

    @Override
    public void draw(MiningRecipe r, IRecipeSlotsView rsv, GuiGraphics gui, double mX, double mY) {
        MINER.draw(gui, 71, 35); //45
        AllGuiTextures.JEI_SHADOW.render(gui, 61, 57); //67

//        AllGuiTextures.JEI_DOWN_ARROW.render(gui, 106, 5 + 9 * (MAX_ROWS_PER_SECTION - rows));

        gui.pose().pushPose();
        gui.pose().translate(35, 2.7, 0);
        gui.pose().scale(0.6f, 0.6f, 1f);
        int ic = Objects.requireNonNull(ChatFormatting.GRAY.getColor());
        gui.setColor(((ic >>> 16) & 0xff) / 255f, ((ic >>> 8) & 0xff) / 255f, (ic & 0xff) / 255f, 1);
        gui.setColor(1, 1, 1, 1);
        gui.pose().popPose();
    }

    protected IDrawable slotBg(MinerOutput output) {
        SlotBg slot = SLOT;
        if (!output.requirements.isEmpty() && crsToBg.containsKey(output.requirements.get(0))) {
            slot = crsToBg.get(output.requirements.get(0));
        }
        return output.chance >= 1 ? slot.basic : slot.chance;
    }

    protected List<LayoutEntry> outputLayout(MiningRecipe recipe) {
        // Collect output slots
        var slots = recipe.getYields().stream()
                .flatMap(y -> y.items.stream().map(i -> new LayoutEntry(new MinerOutput(y, i, 1))))
                .toList();

        // Remove slots that don't fit in the layout, however cruel and unjust it may be
        int size = slots.size();
        if (size > MAX_SLOTS) {
            slots.subList(MAX_SLOTS, size).clear();
            size = MAX_SLOTS;
        }

        // Compute positions for slots
        var nRows = Mth.clamp((size - 1) / SLOTS_PER_ROW + 1, 1, MAX_ROWS);
        var layout = new FlexibleLayoutHelper(size, nRows, 18, 18, 1, true, false);
        for (var e : slots) {
            e.x = layout.getX();
            e.y = layout.getY();
            layout.next();
        }

        return slots;
    }

    protected static class LayoutEntry {
        public final MinerOutput output;
        public int x;
        public int y;

        public LayoutEntry(MinerOutput output) {
            this.output = output;
        }
    }

    protected static class MinerOutput {
        public final Item item;
        public int count;
        public final int itemsInGroup;
        public final float weightRatio;
        public final float chance;
        public final List<String> requirements;

        public MinerOutput(Yield yield, Yield.WeightedItem wItem, int count) {
            this.item = wItem.item();
            this.count = count;
            this.itemsInGroup = yield.items.size();
            this.weightRatio = (float) wItem.weight() / yield.getTotalWeight();
            this.chance = yield.chance;
            this.requirements = yield.crsNames;
        }
    }

    protected record SlotBg(IDrawable basic, IDrawable chance) {}
}
