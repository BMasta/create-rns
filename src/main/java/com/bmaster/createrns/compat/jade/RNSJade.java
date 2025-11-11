package com.bmaster.createrns.compat.jade;

import com.bmaster.createrns.deposit.DepositBlock;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class RNSJade implements IWailaPlugin {

    @Override
    public void register(IWailaCommonRegistration registration) {
        //TODO register data providers
        registration.registerBlockDataProvider(DepositBlockComponentProvider.INSTANCE, DepositBlock.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        //TODO register component providers, icon providers, callbacks, and config options here
        registration.registerBlockComponent(DepositBlockComponentProvider.INSTANCE, DepositBlock.class);
    }
}
