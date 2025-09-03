package com.bmaster.createrns.datagen.pack.json;

import com.google.gson.annotations.SerializedName;

import java.util.Collections;
import java.util.Map;

public class DepositStructure {
    public String type = "minecraft:jigsaw";

    @SerializedName("start_pool")
    public String startPool;

    @SerializedName("size")
    public int size = 1;

    @SerializedName("start_height")
    public StartHeight startHeight;

    @SerializedName("project_start_to_heightmap")
    public String projectStartToHeightmap = "OCEAN_FLOOR_WG";

    @SerializedName("step")
    public String step = "underground_ores";

    @SerializedName("biomes")
    public String biomes = "#minecraft:is_overworld";

    @SerializedName("terrain_adaptation")
    public String terrainAdaptation = "none";

    @SerializedName("max_distance_from_center")
    public int maxDistanceFromCenter = 80;

    @SerializedName("use_expansion_hack")
    public boolean useExpansionHack = false;

    @SerializedName("spawn_overrides")
    public Map<String, Object> spawnOverrides = Collections.emptyMap();

    public DepositStructure(String structureStart, int absoluteStartHeight) {
        startPool = structureStart;
        startHeight = new StartHeight(absoluteStartHeight);
    }

    public static final class StartHeight {
        @SerializedName("absolute")
        public int absolute;

        public StartHeight(int absolute) {
            this.absolute = absolute;
        }
    }
}
