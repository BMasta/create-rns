package com.bmaster.createrns.content.deposit.claiming;

import com.bmaster.createrns.content.deposit.operating.IDepositBlockOperator;
import com.bmaster.createrns.content.deposit.operating.IOperatorHolderBE;

public interface IClaimerHolderBE extends IOperatorHolderBE {
    IDepositBlockClaimer getClaimer();

    @Override
    default IDepositBlockOperator getOperator() {
        return getClaimer();
    }
}
