package com.bmaster.createrns.content.deposit.operating.space;

import com.bmaster.createrns.compat.Mods;
import com.bmaster.createrns.compat.sable.SableOperatingSpaceAdapter;
import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class OperatingSpaceAdapterHolder {
    private static final OperatingSpaceAdapter ADAPTER = createAdapter();

    private static OperatingSpaceAdapter createAdapter() {
        if (Mods.SABLE.isLoaded()) return SableOperatingSpaceAdapter.create();
        return new VanillaOperatingSpaceAdapter();
    }

    public static OperatingSpaceAdapter getAdapter() {
        return ADAPTER;
    }
}
