package me.dw1e.kbm;

import me.dw1e.kbm.api.DefaultKnockbackManagerAPI;
import me.dw1e.kbm.api.KnockbackManagerAPI;
import me.dw1e.kbm.command.KBMCommand;
import me.dw1e.kbm.data.DataManager;
import me.dw1e.kbm.listener.EventListener;
import me.dw1e.kbm.listener.PacketHandler;
import me.dw1e.kbm.util.KBFile;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class KnockbackManager extends JavaPlugin {

    public static final String PREFIX = "§8[§eKBM§8]";

    private static KnockbackManager instance;

    private KnockbackManagerAPI api;
    private DataManager dataManager;
    private PacketHandler packetHandler;
    private KBFile kbFile;

    private int ticks;
    private BukkitTask tickTask;

    public static KnockbackManager getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        if (!getDescription().getName().equals("KnockbackManager") || !getDescription().getAuthors().contains("dw1e")) {
            consoleLog("§c检测到插件信息篡改, 现已停止运行!");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        instance = this;

        kbFile = new KBFile(this);
        kbFile.load(Bukkit.getConsoleSender());

        dataManager = new DataManager();
        dataManager.enable();

        api = new DefaultKnockbackManagerAPI(this);

        PluginCommand pluginCommand = getCommand("kbm");

        if (pluginCommand != null) {
            KBMCommand kbmCommand = new KBMCommand(this);

            pluginCommand.setExecutor(kbmCommand);
            pluginCommand.setTabCompleter(kbmCommand);
        }

        Bukkit.getPluginManager().registerEvents(new EventListener(this), this);

        Plugin pLib = Bukkit.getPluginManager().getPlugin("ProtocolLib");

        if (pLib != null && pLib.isEnabled()) {
            String pLibDesc = pLib.getDescription().getVersion();
            int pLibVer = Integer.parseInt(pLibDesc.split("\\.")[0]);

            if (pLibVer < 5) {
                consoleLog("§c不支持的 ProtocolLib 版本: §e" + pLibDesc + "§c, 请使用 5.0.0 或之后的版本, Misplace 模块将不会启用!");
            } else {
                consoleLog("§a检测到 ProtocolLib §e" + pLibDesc + "§a, 已启用 Misplace 模块!");

                packetHandler = new PacketHandler(this);
                packetHandler.enable();
            }
        } else consoleLog("§c未检测到 ProtocolLib 安装/运行, Misplace 模块将不会启用!");

        tickTask = Bukkit.getScheduler().runTaskTimer(this, () -> {
            ticks++;

            if (packetHandler != null) packetHandler.tick(ticks);
        }, 0L, 1L);
    }

    @Override
    public void onDisable() {
        HandlerList.unregisterAll(this);

        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }

        if (packetHandler != null) {
            packetHandler.disable();
            packetHandler = null;
        }

        api = null;

        if (dataManager != null) {
            dataManager.disable();
            dataManager = null;
        }

        kbFile = null;

        instance = null;
    }

    public KnockbackManagerAPI getAPI() {
        return api;
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    public PacketHandler getPacketHandler() {
        return packetHandler;
    }

    public KBFile getKbFile() {
        return kbFile;
    }

    public int getTicks() {
        return ticks;
    }

    public void consoleLog(String s) {
        Bukkit.getConsoleSender().sendMessage(PREFIX + " " + s);
    }
}
