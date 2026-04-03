package com.bmaster.createrns.compat.jei;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.RNSBlocks;
import com.bmaster.createrns.RNSRecipeTypes;
import com.bmaster.createrns.compat.Mods;
import com.bmaster.createrns.content.deposit.mining.recipe.MiningRecipe;
import com.bmaster.createrns.content.deposit.mining.recipe.Yield;
import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.CatalystRequirementSet;
import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.CatalystRequirementSetLookup;
import com.bmaster.createrns.util.FlexibleLayoutHelper;
import com.bmaster.createrns.util.Utils;
import com.mojang.blaze3d.systems.RenderSystem;
import com.simibubi.create.compat.jei.EmptyBackground;
import com.simibubi.create.compat.jei.ItemIcon;
import com.simibubi.create.compat.jei.category.CreateRecipeCategory;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotRichTooltipCallback;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class MiningRecipeCategory extends CreateRecipeCategory<MiningRecipe> {
    public static final List<Supplier<? extends ItemStack>> CATALYSTS = List.of(
            () -> new ItemStack(RNSBlocks.MINER_BEARING.get()),
            () -> new ItemStack(RNSBlocks.MINE_HEAD.get()),
            () -> new ItemStack(RNSBlocks.RESONATOR.get()),
            () -> new ItemStack(RNSBlocks.SHATTERING_RESONATOR.get()),
            () -> new ItemStack(RNSBlocks.STABILIZING_RESONATOR.get())
    );

    private static final boolean EMI_LOADED = Mods.EMI.isLoaded();

    private static final AnimatedMiner MINER = new AnimatedMiner();

    private static final String SCROLL_GROUP = "miner_yields";
    private static final int YIELD_COLS = 4;
    private static final int YIELD_ROWS = 5;

    private static Info<MiningRecipe> getInfo(RecipeType<RecipeHolder<MiningRecipe>> recipeType) {
        return new Info<>(
                recipeType, CreateRNS.translatable("jei.title." + recipeType.getUid().getPath()),
                new EmptyBackground(177, 100),
                new ItemIcon(() -> new ItemStack(RNSBlocks.MINER_BEARING)),
                (() -> {
                    var level = Minecraft.getInstance().level;
                    if (level == null) return List.of();
                    return level.getRecipeManager().getAllRecipesFor(RNSRecipeTypes.MINING_RECIPE_TYPE.get());
                }),
                CATALYSTS
        );
    }

    protected static IRecipeSlotRichTooltipCallback addStochasticTooltip(MinerOutput output) {
        return (view, tooltip) -> {
            for (var crs : output.crsList) {
                tooltip.add(CreateRNS.lang()
                        .translate("jei.catalyst." + ((crs.optional) ? "optional" : "required"))
                        .space()
                        .add(crs.getNameComponent())
                        .component());
            }

            // If yield contains more than one item, print how many items are in the same group
            if (output.itemsInGroup > 1) {
                tooltip.add(CreateRNS.lang().translate("jei.item_in_group", output.itemsInGroup).component());
            }

            // No tooltips needed. Item is always mined.
            if (output.chance * output.weightRatio == 1) return;

            Function<LangBuilder, LangBuilder> forGroup = l -> output.weightRatio == 1 ? l :
                    l.space().translate("jei.for_group");
            Function<LangBuilder, LangBuilder> forItem = l ->
                    l.space().translate("jei.for_item");

            var minChanceGroup = Utils.fancyChanceArg(output.chance).style(ChatFormatting.GOLD);
            var minChanceItem = Utils.fancyChanceArg(output.chance * output.weightRatio).style(ChatFormatting.GOLD);

            // Min chance for group
            tooltip.add(forGroup.apply(CreateRNS.lang()
                    .translate("jei.chance", minChanceGroup)
                    .style(ChatFormatting.GRAY)).component());

            // Group/item chances are equivalent
            if (output.weightRatio == 1) return;

            // Min chance for item
            tooltip.add(forItem.apply(CreateRNS.lang()
                    .translate("jei.chance", minChanceItem)
                    .style(ChatFormatting.GRAY)).component());
        };
    }

    IDrawable SLOT;

    public MiningRecipeCategory(IGuiHelper gui, RecipeType<RecipeHolder<MiningRecipe>> recipeType) {
        super(getInfo(recipeType));
        SLOT = gui.getSlotDrawable();
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, MiningRecipe recipe, IFocusGroup focuses) {
        var slotBGs = new Int2ObjectOpenHashMap<TintedDrawable>();
        var conn = Minecraft.getInstance().getConnection();

        recipe.initialize((conn != null) ? conn.registryAccess() : RegistryAccess.EMPTY);

        // Mined deposit block
        builder.addSlot(RecipeIngredientRole.INPUT, 33, 7) // Y=5 is level with the top of the yield grid
                .setBackground(SLOT, -1, -1)
                .addItemStack(new ItemStack(recipe.getDepositBlock().asItem()));

        var slots = recipe.getYields().stream()
                .flatMap(y -> y.items.stream().map(i -> new MinerOutput(y, i, 1)))
                .collect(Collectors.toCollection(ArrayList::new));

        if (EMI_LOADED) {
            var layout = new FlexibleLayoutHelper(
                    slots.size(), YIELD_ROWS, 18, 18, 0,
                    FlexibleLayoutHelper.Anchor.CENTER, FlexibleLayoutHelper.Anchor.START);

            for (var slot : slots) {
                builder.addSlot(RecipeIngredientRole.OUTPUT, 124 + layout.getX(), 6 + layout.getY())
                        .setBackground(slotBGs.computeIfAbsent(slot.bgColor, c -> new TintedDrawable(SLOT, c)), -1, -1)
                        .addRichTooltipCallback(addStochasticTooltip(slot))
                        .addItemStack(new ItemStack(slot.item, slot.count));
                layout.next();
            }
            return;
        }

        for (var slot : slots) {
            builder.addSlot(RecipeIngredientRole.OUTPUT)
                    .setBackground(slotBGs.computeIfAbsent(slot.bgColor, c -> new TintedDrawable(SLOT, c)), -1, -1)
                    .setSlotName(SCROLL_GROUP)
                    .addRichTooltipCallback(addStochasticTooltip(slot))
                    .addItemStack(new ItemStack(slot.item, slot.count));
        }
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, RecipeHolder<MiningRecipe> recipe, IFocusGroup focuses) {
        if (EMI_LOADED) return;

        List<IRecipeSlotDrawable> scrollSlots = builder.getRecipeSlots().getSlots().stream()
                .filter(s -> s.getSlotName().filter(SCROLL_GROUP::equals).isPresent())
                .toList();
        builder.addScrollGridWidget(scrollSlots, YIELD_COLS, YIELD_ROWS).setPosition(85, 4);
    }

    @Override
    public void draw(MiningRecipe r, IRecipeSlotsView rsv, GuiGraphics gui, double mX, double mY) {
        MINER.draw(gui, 38, 45, r.getDepositBlock(), 13); // 38 X diff between miner and input slot
    }

    public static class TintedDrawable implements IDrawable {
        protected static int BASE_COLOR = 0x8B8B8B;

        private final IDrawable delegate;
        private final float r, g, b;

        public TintedDrawable(IDrawable delegate, int targetColor) {
            this.delegate = delegate;

            float br = ((BASE_COLOR >>> 16) & 0xFF) / 255f;
            float bg = ((BASE_COLOR >>> 8) & 0xFF) / 255f;
            float bb = ((BASE_COLOR) & 0xFF) / 255f;

            if (targetColor == 0) {
                r = 1;
                g = 1;
                b = 1;
                return;
            }

            float cr = ((targetColor >>> 16) & 0xFF) / 255f;
            float cg = ((targetColor >>> 8) & 0xFF) / 255f;
            float cb = ((targetColor) & 0xFF) / 255f;

            float eps = 1e-6f;

            float tr = cr / Math.max(br, eps);
            float tg = cg / Math.max(bg, eps);
            float tb = cb / Math.max(bb, eps);

            r = tr;
            g = tg;
            b = tb;
        }

        @Override
        public int getWidth() {
            return delegate.getWidth();
        }

        @Override
        public int getHeight() {
            return delegate.getHeight();
        }

        @Override
        public void draw(GuiGraphics gui, int xOffset, int yOffset) {
            RenderSystem.setShaderColor(r, g, b, 1);
            delegate.draw(gui, xOffset, yOffset);
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        }
    }

    protected static class MinerOutput {
        public final Item item;
        public int count;
        public final int itemsInGroup;
        public final float weightRatio;
        public final float chance;
        public final int bgColor;
        public final List<CatalystRequirementSet> crsList;

        public MinerOutput(Yield yield, Yield.WeightedItem wItem, int count) {
            var conn = Minecraft.getInstance().getConnection();
            var access = (conn != null) ? conn.registryAccess() : RegistryAccess.EMPTY;

            this.item = wItem.item;
            this.count = count;
            this.itemsInGroup = yield.items.size();
            this.weightRatio = (float) wItem.weight / yield.getTotalWeight();
            this.chance = yield.chance;
            this.bgColor = yield.slotColor;
            this.crsList = yield.crsNames.stream()
                    .map(crsName -> CatalystRequirementSetLookup.get(access, crsName))
                    .toList();
        }
    }
}
