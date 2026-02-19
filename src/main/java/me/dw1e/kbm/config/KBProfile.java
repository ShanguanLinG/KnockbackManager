package me.dw1e.kbm.config;

import me.dw1e.kbm.KnockbackManager;
import org.bukkit.configuration.file.FileConfiguration;

public final class KBProfile {

    public static double HORIZONTAL_GROUND;
    public static double HORIZONTAL_AIR;
    public static double HORIZONTAL_SPRINT_EXTRA;

    public static double VERTICAL_GROUND;
    public static double VERTICAL_AIR;
    public static double VERTICAL_SPRINT_EXTRA;

    public static boolean PROJECTILE_ENABLED;
    public static double PROJECTILE_HORIZONTAL_MULTIPLIER;
    public static double PROJECTILE_VERTICAL_MULTIPLIER;
    public static boolean PROJECTILE_DIRECTION_OVERRIDE;

    public static boolean PACKET_MISPLACE_ENABLED;
    public static double PACKET_MISPLACE_DISTANCE;
    public static boolean PACKET_DELAY_ENABLED;
    public static int PACKET_DELAY_TICKS;

    public static boolean STOP_SPRINT;

    public static double Y_LIMIT;

    public static int HIT_DELAY;

    public static boolean POTION_ENABLED;
    public static double POTION_HORIZONTAL_MULTIPLIER;
    public static double POTION_VERTICAL_MULTIPLIER;
    public static double POTION_COMPENSATION_MULTIPLIER;

    private final String name;

    public KBProfile(String name) {
        this.name = name;

        updateConfig();
    }

    public void updateConfig() {
        FileConfiguration config = KnockbackManager.getInstance().getKbFile().getConfig(name);

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

        Y_LIMIT = config.getDouble("y_limit");

        HIT_DELAY = config.getInt("hit_delay");

        POTION_ENABLED = config.getBoolean("potion.enabled");
        POTION_HORIZONTAL_MULTIPLIER = config.getDouble("potion.horizontal_multiplier");
        POTION_VERTICAL_MULTIPLIER = config.getDouble("potion.vertical_multiplier");
        POTION_COMPENSATION_MULTIPLIER = config.getDouble("potion.compensation_multiplier");
    }

    public String getName() {
        return name;
    }
}
