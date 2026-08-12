package me.dw1e.kbm.config;

import org.bukkit.configuration.file.FileConfiguration;

public final class KBProfile {

    public double HORIZONTAL_GROUND;
    public double HORIZONTAL_AIR;
    public double HORIZONTAL_SPRINT_EXTRA;

    public double VERTICAL_GROUND;
    public double VERTICAL_AIR;
    public double VERTICAL_SPRINT_EXTRA;

    public boolean PROJECTILE_ENABLED;
    public double PROJECTILE_HORIZONTAL_MULTIPLIER;
    public double PROJECTILE_VERTICAL_MULTIPLIER;
    public boolean PROJECTILE_DIRECTION_OVERRIDE;

    public boolean PACKET_MISPLACE_ENABLED;
    public double PACKET_MISPLACE_DISTANCE;
    public boolean PACKET_DELAY_ENABLED;
    public int PACKET_DELAY_TICKS;

    public boolean STOP_SPRINT;

    public boolean Y_LIMIT_ENABLED;
    public double Y_LIMIT_MAX_HEIGHT;
    public double Y_LIMIT_VERTICAL_KB;

    public int HIT_DELAY;

    public boolean POTION_ENABLED;
    public double POTION_HORIZONTAL_MULTIPLIER;
    public double POTION_VERTICAL_MULTIPLIER;
    public double POTION_COMPENSATION_MULTIPLIER;

    public boolean MODERN_COOLDOWN_AFFECTS_KB;
    public boolean MODERN_NETHERITE_KB_RESISTANCE;

    public KBProfile(FileConfiguration config) {
        updateConfig(config);
    }

    public void updateConfig(FileConfiguration config) {
        HORIZONTAL_GROUND = config.getDouble("horizontal.ground");
        HORIZONTAL_AIR = config.getDouble("horizontal.air");
        HORIZONTAL_SPRINT_EXTRA = config.getDouble("horizontal.sprint_extra");

        VERTICAL_GROUND = config.getDouble("vertical.ground");
        VERTICAL_AIR = config.getDouble("vertical.air");
        VERTICAL_SPRINT_EXTRA = config.getDouble("vertical.sprint_extra");

        PROJECTILE_ENABLED = config.getBoolean("projectile.enabled");
        PROJECTILE_HORIZONTAL_MULTIPLIER = config.getDouble("projectile.horizontal_multiplier");
        PROJECTILE_VERTICAL_MULTIPLIER = config.getDouble("projectile.vertical_multiplier");
        PROJECTILE_DIRECTION_OVERRIDE = config.getBoolean("projectile.direction_override");

        PACKET_MISPLACE_ENABLED = config.getBoolean("packet.misplace.enabled");
        PACKET_MISPLACE_DISTANCE = config.getDouble("packet.misplace.distance");
        PACKET_DELAY_ENABLED = config.getBoolean("packet.delay.enabled");
        PACKET_DELAY_TICKS = config.getInt("packet.delay.ticks");

        STOP_SPRINT = config.getBoolean("stop_sprint");

        Y_LIMIT_ENABLED = config.getBoolean("y_limit.enabled");
        Y_LIMIT_MAX_HEIGHT = config.getDouble("y_limit.max_y_height");
        Y_LIMIT_VERTICAL_KB = config.getDouble("y_limit.vertical_kb_after_limit");

        HIT_DELAY = config.getInt("hit_delay");

        POTION_ENABLED = config.getBoolean("potion.enabled");
        POTION_HORIZONTAL_MULTIPLIER = config.getDouble("potion.horizontal_multiplier");
        POTION_VERTICAL_MULTIPLIER = config.getDouble("potion.vertical_multiplier");
        POTION_COMPENSATION_MULTIPLIER = config.getDouble("potion.compensation_multiplier");

        MODERN_COOLDOWN_AFFECTS_KB = config.getBoolean("modern.cooldown_affects_kb");
        MODERN_NETHERITE_KB_RESISTANCE = config.getBoolean("modern.netherite_kb_resistance");
    }
}
