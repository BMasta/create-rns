package com.bmaster.createrns.compat;

import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.LoadingModList;

public enum Mods {
    EMI("emi"),
    NEW_AGE("create_new_age"),
    NUCLEAR("createnuclear"),
    AE2("ae2");

    public final String ID;

    Mods(String id) {
        this.ID = id;
    }

    public boolean isLoaded() {
        var modList = ModList.get();
        if (modList != null) return modList.isLoaded(ID);
        else return LoadingModList.get().getModFileById(ID) != null;
    }
}
