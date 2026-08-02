package com.bmaster.createrns.content.deposit.claiming;

import net.minecraft.core.BlockPos;

import java.util.Optional;

public interface IDepositClaimerHolder {
    Optional<IDepositBlockClaimer> getClaimer();
    BlockPos getBlockPos();
}
