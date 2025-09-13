package com.bmaster.createrns.data.pack.json;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class DepositStructureTag {
    @SerializedName("values")
    public List<String> values;

    public DepositStructureTag(List<String> structureIds) {
        values = structureIds;
    }
}
