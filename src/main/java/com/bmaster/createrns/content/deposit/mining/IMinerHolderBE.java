package com.bmaster.createrns.content.deposit.mining;

import com.bmaster.createrns.content.deposit.claiming.IClaimerHolderBE;
import com.bmaster.createrns.content.deposit.claiming.IDepositBlockClaimer;

public interface IMinerHolderBE extends IClaimerHolderBE {
    IDepositBlockMiner getMiner();

    @Override
    default IDepositBlockClaimer getClaimer() {
        return getMiner();
    }
}
