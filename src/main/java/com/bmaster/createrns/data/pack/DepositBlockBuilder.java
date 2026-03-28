package com.bmaster.createrns.data.pack;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.content.deposit.DepositBlock;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.nullness.NonNullFunction;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class DepositBlockBuilder {
    private final ResourceLocation depositBlockId;
    private final Supplier<Boolean> isEnabled;
    private @Nullable BlockBuilder<DepositBlock, CreateRegistrate> delegate;

    public DepositBlockBuilder(String depositName, Supplier<Boolean> isEnabled) {
        this.depositBlockId = CreateRNS.asResource(depositName);
        this.isEnabled = isEnabled;
        if (!DepositStructureBuilder.dumpMode) {
            delegate = CreateRNS.REGISTRATE.block(depositName, DepositBlock::new);
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

    public DepositBlockBuilder recipe(Consumer<DepositBlockBuildingContext> ctx) {
        ctx.accept(new DepositBlockBuildingContext(depositBlockId, isEnabled));
        return this;
    }

    /// Returns null only when in dump mode
    public BlockEntry<DepositBlock> register() {
        if (delegate == null) return null;
        return delegate.register();
    }

    public record DepositBlockBuildingContext(
            ResourceLocation depositBlockId, Supplier<Boolean> isEnabled
    ) {
    }
}
