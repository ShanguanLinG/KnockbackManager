package me.dw1e.kbm.config;

import org.bukkit.configuration.file.FileConfiguration;

public final class ConfigValue {

    public static boolean KB_SYNC_ENABLED;

    public static boolean SSHD_ENABLED;
    public static double SSHD_HITBOX_LENGTH;
    public static double SSHD_HITBOX_HEIGHT;
    public static double SSHD_MAX_DISTANCE_SURVIVAL;
    public static double SSHD_MAX_DISTANCE_CREATIVE;

    public static void updateConfig(FileConfiguration config) {
        KB_SYNC_ENABLED = config.getBoolean("kb_sync");

        SSHD_ENABLED = config.getBoolean("server_side_hit_detection.enabled");
        SSHD_HITBOX_LENGTH = config.getDouble("server_side_hit_detection.hitbox_size.length");
        SSHD_HITBOX_HEIGHT = config.getDouble("server_side_hit_detection.hitbox_size.height");
        SSHD_MAX_DISTANCE_SURVIVAL = config.getDouble("server_side_hit_detection.max_distance.survival");
        SSHD_MAX_DISTANCE_CREATIVE = config.getDouble("server_side_hit_detection.max_distance.creative");
    }
}
