package com.bmaster.createrns.compat.jade;

import com.bmaster.createrns.content.deposit.DepositBlock;
import net.minecraft.MethodsReturnNonnullByDefault;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

import javax.annotation.ParametersAreNonnullByDefault;

@WailaPlugin
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class RNSJade implements IWailaPlugin {

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(DepositBlockComponentProvider.INSTANCE, DepositBlock.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(DepositBlockComponentProvider.INSTANCE, DepositBlock.class);
    }
}
