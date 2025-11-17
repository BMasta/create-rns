package com.bmaster.createrns.data.gen.depositworldgen;

public class DepositSetConfigBuilder {
    public static final int DEFAULT_SALT = 591646342;
    public static final int DEFAULT_SPACING = 48;

    private int separation = 8;
    private int spacing = DEFAULT_SPACING;
    private int salt = DEFAULT_SALT;

    public static DepositSetConfigBuilder create() {
        return new DepositSetConfigBuilder();
    }

    public DepositSetConfigBuilder separation(int separation) {
        this.separation = separation;
        return this;
    }

    public DepositSetConfigBuilder spacing(int spacing) {
        this.spacing = spacing;
        return this;
    }

    public DepositSetConfigBuilder salt(int salt) {
        this.salt = salt;
        return this;
    }

    public void save() {
        DepositWorldgenProvider.setConf = new DepositWorldgenProvider.DepositSet(separation, spacing, salt);
    }
}
