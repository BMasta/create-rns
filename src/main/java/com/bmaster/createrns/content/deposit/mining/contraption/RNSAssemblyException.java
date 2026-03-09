package com.bmaster.createrns.content.deposit.mining.contraption;

import com.bmaster.createrns.CreateRNS;
import com.simibubi.create.content.contraptions.AssemblyException;
import net.createmod.catnip.lang.LangBuilder;

public class RNSAssemblyException extends AssemblyException {
    public RNSAssemblyException(String langKey, Object... objects) {
        super(CreateRNS.lang().translate("gui.assembly.exception." + langKey,
                LangBuilder.resolveBuilders(objects)).component());
    }
}
