package com.bmaster.createrns.compat;

import net.neoforged.fml.ModList;

public enum Mods {
    EMI("emi");

    public final String ID;

    Mods(String id) {
        this.ID = id;
    }

    public boolean isLoaded() {
        return ModList.get().isLoaded(ID);
    }
}
