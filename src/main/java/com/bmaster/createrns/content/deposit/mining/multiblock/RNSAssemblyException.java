package com.bmaster.createrns.content.deposit.mining.multiblock;

import com.bmaster.createrns.CreateRNS;
import com.simibubi.create.content.contraptions.AssemblyException;
import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.network.chat.Component;

public class RNSAssemblyException extends AssemblyException {
    public RNSAssemblyException(Component component) {
        super(component);
    }

    public RNSAssemblyException(String langKey, Object... objects) {
        super(new LangBuilder(CreateRNS.MOD_ID).translate("gui.assembly.exception." + langKey,
                LangBuilder.resolveBuilders(objects)).component());
    }
}
