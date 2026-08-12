package com.bmaster.createrns.content.deposit.operating.sublevel;

import com.bmaster.createrns.compat.Mods;
import com.bmaster.createrns.compat.sable.SableOperatingSublevelAdapter;
import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class OperatingSublevelAdapterHolder {
    private static final OperatingSublevelAdapter ADAPTER = createAdapter();

    private static OperatingSublevelAdapter createAdapter() {
        if (Mods.SABLE.isLoaded()) return SableOperatingSublevelAdapter.create();
        return new VanillaOperatingSublevelAdapter();
    }

    public static OperatingSublevelAdapter getAdapter() {
        return ADAPTER;
    }
}
