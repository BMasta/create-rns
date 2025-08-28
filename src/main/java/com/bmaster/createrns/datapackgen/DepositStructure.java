package com.bmaster.createrns.datapackgen;

import com.google.gson.annotations.SerializedName;

import java.util.Collections;
import java.util.Map;

public class DepositStructure {
    public String type = "minecraft:jigsaw";

    @SerializedName("start_pool")
    public String startPool = "create_rns:deposit_iron/start";

    public int size = 1;

    @SerializedName("start_height")
    public StartHeight startHeight = new StartHeight(30);

    @SerializedName("project_start_to_heightmap")
    public String projectStartToHeightmap = "OCEAN_FLOOR_WG";

    /** Note: using the exact value you specified. */
    public String step = "underground_ores";

    public String biomes = "#minecraft:is_overworld";

    @SerializedName("terrain_adaptation")
    public String terrainAdaptation = "none";

    @SerializedName("max_distance_from_center")
    public int maxDistanceFromCenter = 80;

    @SerializedName("use_expansion_hack")
    public boolean useExpansionHack = false;

    @SerializedName("spawn_overrides")
    public Map<String, Object> spawnOverrides = Collections.emptyMap();

    public static final class StartHeight {
        public int absolute;
        public StartHeight(int absolute) { this.absolute = absolute; }
    }
}
