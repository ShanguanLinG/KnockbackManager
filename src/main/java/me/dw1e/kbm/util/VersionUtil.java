package me.dw1e.kbm.util;

import me.dw1e.kbm.KnockbackManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;

public final class VersionUtil {

    private final static Method attackMethod;

    static {
        try {
            String version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];

            Class<?> entityPlayerClass = Class.forName("net.minecraft.server." + version + ".EntityPlayer");
            Class<?> entityClass = Class.forName("net.minecraft.server." + version + ".Entity");

            attackMethod = entityPlayerClass.getMethod("attack", entityClass);

        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public static void attack(Player attacker, Player victim) {
        try {
            Object nmsAttacker = attacker.getClass().getMethod("getHandle").invoke(attacker);
            Object nmsVictim = victim.getClass().getMethod("getHandle").invoke(victim);

            attackMethod.invoke(nmsAttacker, nmsVictim);

        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public static int getPing(Player player) {
        if (player == null) return -1;

        if (KnockbackManager.getInstance().isAtLeast1_17()) {
            return player.getPing();
        } else {
            try {
                Object entityPlayer = player.getClass().getMethod("getHandle").invoke(player);
                return (int) entityPlayer.getClass().getField("ping").get(entityPlayer);
            } catch (Exception e) {
                return -1;
            }
        }
    }
}
