package com.bmaster.createrns.compat.jade;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.content.deposit.info.DepositDurabilityManager;
import com.bmaster.createrns.infrastructure.ServerConfig;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public enum DepositBlockComponentProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (ServerConfig.INFINITE_DEPOSITS.get()) return;
        var sd = accessor.getServerData();
        if (!sd.contains("durability", LongTag.TAG_LONG)) return;
        var dur = sd.getLong("durability");

        MutableComponent durComp;
        if (dur == -1) durComp = CreateRNS.translatable("jade.deposit_not_generated");
        else if (dur == 0) durComp = CreateRNS.translatable("mining.infinite");
        else durComp = Component.literal(Long.toString(dur));

        tooltip.add(CreateRNS.translatable("mining.remaining_deposit_uses").append(" "));
        tooltip.append(durComp);
    }

    @Override
    public ResourceLocation getUid() {
        return CreateRNS.asResource("deposit_block_info");
    }

    @Override
    public void appendServerData(CompoundTag nbt, BlockAccessor accessor) {
        nbt.putLong("durability", DepositDurabilityManager.getDepositBlockDurability((ServerLevel) accessor.getLevel(),
                accessor.getPosition(), false));
    }
}
