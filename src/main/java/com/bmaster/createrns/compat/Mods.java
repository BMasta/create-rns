package com.bmaster.createrns.compat;

import net.minecraftforge.fml.loading.LoadingModList;

public enum Mods {
    NEW_AGE("create_new_age"),
    NUCLEAR("createnuclear"),
    AE2("ae2"),
    XAERO("xaeroworldmap"),
    JOURNEY("journeymap");

    public final String ID;

    Mods(String id) {
        this.ID = id;
    }

    public boolean isLoaded() {
		return LoadingModList.get().getModFileById(ID) != null;
	}
}
