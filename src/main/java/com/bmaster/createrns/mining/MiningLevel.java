//package com.bmaster.createrns.mining;
//
//import com.bmaster.createrns.CreateRNS;
//import net.minecraft.network.chat.Component;
//
//public enum MiningLevel {
//    BASIC(1, "basic"),
//    ADVANCED(2, "advanced");
//
//    private final int level;
//    private final String id;
//
//    MiningLevel(int level, String id) {
//        this.level = level;
//        this.id = id;
//    }
//
//    public int getLevel() {
//        return level;
//    }
//
//    public String getRecipeID() {
//        return id + "_mining";
//    }
//
//    public Component getTitle() {
//        return Component.translatable(CreateRNS.MOD_ID + ".recipe.mining." + id);
//    }
//}
