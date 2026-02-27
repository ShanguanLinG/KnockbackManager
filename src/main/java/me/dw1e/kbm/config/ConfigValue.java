package me.dw1e.kbm.config;

import org.bukkit.configuration.file.FileConfiguration;

public final class ConfigValue {

    public static boolean KB_SYNC;

    public static boolean HIT_DETECTION_ENABLED;
    public static double HIT_DETECTION_BOX_LENGTH;
    public static double HIT_DETECTION_BOX_HEIGHT;
    public static double HIT_DETECTION_MAX_DISTANCE_SURVIVAL;
    public static double HIT_DETECTION_MAX_DISTANCE_CREATIVE;

    public static void updateConfig(FileConfiguration config) {
        KB_SYNC = config.getBoolean("kb_sync");

        HIT_DETECTION_ENABLED = config.getBoolean("server_side_hit_detection.enabled");
        HIT_DETECTION_BOX_LENGTH = config.getDouble("server_side_hit_detection.box_length");
        HIT_DETECTION_BOX_HEIGHT = config.getDouble("server_side_hit_detection.box_height");
        HIT_DETECTION_MAX_DISTANCE_SURVIVAL = config.getDouble("server_side_hit_detection.max_distance.survival");
        HIT_DETECTION_MAX_DISTANCE_CREATIVE = config.getDouble("server_side_hit_detection.max_distance.creative");
    }
}
