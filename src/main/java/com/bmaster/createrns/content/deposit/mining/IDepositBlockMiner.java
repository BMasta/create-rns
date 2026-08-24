package com.bmaster.createrns.content.deposit.mining;

import com.bmaster.createrns.content.deposit.claiming.IDepositBlockClaimer;
import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.Catalyst;

import javax.annotation.Nullable;
import java.util.Set;

public interface IDepositBlockMiner extends IDepositBlockClaimer {
    @Nullable
    MinerSpec getSpec();

    @Nullable MiningProcess getProcess();

    @Nullable Set<Catalyst> getCatalysts();

    boolean isMining();

    int getCurrentProgressIncrement();

    record MinerSpec(
            OperatingDimensions miningDimensions,
            DetectionDimensions crossSublevelMiningDimensions,
            double miningSpeed
    ) {}
}
