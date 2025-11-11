package com.bmaster.createrns.compat.jade;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.RNSContent;
import com.bmaster.createrns.infrastructure.ServerConfig;
import net.minecraft.ChatFormatting;
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
        else if (dur == 0) durComp = Component.translatable("create_rns.miner.infinite");
        else durComp = Component.literal("x" + dur);

        tooltip.add(Component.translatable("create_rns.miner.remaining_deposit_uses").append(" "));
        tooltip.append(durComp.withStyle(ChatFormatting.GREEN));
    }

    @Override
    public ResourceLocation getUid() {
        return ResourceLocation.fromNamespaceAndPath(CreateRNS.MOD_ID, "deposit_block_info");
    }

    @Override
    public void appendServerData(CompoundTag nbt, BlockAccessor accessor) {
        var depData = accessor.getLevel().getData(RNSContent.LEVEL_DEPOSIT_DATA.get());
        nbt.putLong("durability", depData.getDepositBlockDurability(accessor.getPosition(), false));
    }
}
