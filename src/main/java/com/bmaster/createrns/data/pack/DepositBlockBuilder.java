package com.bmaster.createrns.data.pack;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.content.deposit.DepositBlock;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.nullness.NonNullFunction;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class DepositBlockBuilder {
    public static boolean dumpMode = false;

    public static DepositBlockBuilder create(String keyword) {
        return new DepositBlockBuilder(new DepositBuildingContext(keyword));
    }

    private final DepositBuildingContext ctx;
    private @Nullable BlockBuilder<DepositBlock, CreateRegistrate> delegate;
    private final ObjectOpenHashSet<String> compatIndicatorBlocks = new ObjectOpenHashSet<>();

    public DepositBlockBuilder enableWhenBlockPresent(String name) {
        compatIndicatorBlocks.add(name);
        return this;
    }

    public DepositBlockBuilder transform(
            NonNullFunction<BlockBuilder<DepositBlock, CreateRegistrate>,
                    BlockBuilder<DepositBlock, CreateRegistrate>> transform
    ) {
        if (delegate != null) {
            delegate = transform.apply(delegate);
        }
        return this;
    }

    public DepositBlockBuilder attach(Consumer<DepositBuildingContext> attachment) {
        attachment.accept(ctx);
        return this;
    }

    /// Returns null only when in dump mode
    public BlockEntry<DepositBlock> register() {
        if (delegate == null) return null;
        return delegate.register();
    }

    private DepositBlockBuilder(DepositBuildingContext ctx) {
        this.ctx = ctx;
        if (dumpMode) {
            ctx.isEnabled = () -> compatIndicatorBlocks.isEmpty() || DynamicDatapackDumpTool.includeCompat();
        } else {
            delegate = CreateRNS.REGISTRATE.block(ctx.depositKeyword + "_deposit_block", DepositBlock::new);
            ctx.isEnabled = () -> ForgeRegistries.BLOCKS.getKeys().stream().anyMatch(rl ->
                    compatIndicatorBlocks.isEmpty() || compatIndicatorBlocks.contains(rl.getPath()));
        }
    }

    public static class DepositBuildingContext {
        public final String depositKeyword;
        public Supplier<Boolean> isEnabled;

        public DepositBuildingContext(String depositKeyword) {
            this.depositKeyword = depositKeyword;
            isEnabled = () -> true;
        }

        public ResourceLocation depositBlockId() {
            return ResourceLocation.fromNamespaceAndPath(CreateRNS.ID, depositKeyword + "_deposit_block");
        }

        public ResourceLocation depositStructureId(DepositDimension dimension) {
            return ResourceLocation.fromNamespaceAndPath(CreateRNS.ID, "deposit_" + dimension.prefix() + depositKeyword);
        }

        public ResourceLocation depositSpecId() {
            return ResourceLocation.fromNamespaceAndPath(CreateRNS.ID, depositKeyword);
        }
    }
}
