package com.bmaster.createrns.compat.jade;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.RNSMisc;
import com.bmaster.createrns.infrastructure.ServerConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum DepositBlockComponentProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (ServerConfig.infiniteDeposits) return;
        var sd = accessor.getServerData();
        if (!sd.contains("durability", LongTag.TAG_LONG)) return;
        var dur = sd.getLong("durability");

        MutableComponent durComp;
        if (dur == -1) durComp = Component.translatable("create_rns.jade.deposit_not_generated");
        else if (dur == 0) durComp = Component.translatable("create_rns.mining.infinite");
        else durComp = Component.literal(Long.toString(dur));

        tooltip.add(Component.translatable("create_rns.mining.remaining_deposit_uses").append(" "));
        tooltip.append(durComp);
    }

    @Override
    public ResourceLocation getUid() {
        return CreateRNS.asResource("deposit_block_info");
    }

    @Override
    public void appendServerData(CompoundTag nbt, BlockAccessor accessor) {
        var depData = accessor.getLevel().getData(RNSMisc.LEVEL_DEPOSIT_DATA.get());
        nbt.putLong("durability", depData.getDepositBlockDurability(accessor.getPosition(), false));
    }
}
