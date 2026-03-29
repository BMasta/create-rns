package com.bmaster.createrns.data.pack;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.content.deposit.DepositBlock;
import com.bmaster.createrns.data.pack.DepositStructureBuilder.DepositBuildingContext;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.nullness.NonNullFunction;

import javax.annotation.Nullable;
import java.util.function.Consumer;

public class DepositBlockBuilder {
    private final DepositBuildingContext ctx;
    private @Nullable BlockBuilder<DepositBlock, CreateRegistrate> delegate;

    public DepositBlockBuilder(DepositBuildingContext ctx) {
        this.ctx = ctx;
        if (!DepositStructureBuilder.dumpMode) {
            delegate = CreateRNS.REGISTRATE.block(ctx.depositKeyword + "_deposit_block", DepositBlock::new);
        }
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

    public DepositBlockBuilder recipe(Consumer<DepositBuildingContext> recipe) {
        recipe.accept(ctx);
        return this;
    }

    public DepositBlockBuilder spec(Consumer<DepositBuildingContext> spec) {
        spec.accept(ctx);
        return this;
    }

    /// Returns null only when in dump mode
    public BlockEntry<DepositBlock> register() {
        if (delegate == null) return null;
        return delegate.register();
    }
}
