package me.dw1e.kbm;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.utility.MinecraftVersion;
import me.dw1e.kbm.api.DefaultKnockbackManagerAPI;
import me.dw1e.kbm.api.KnockbackManagerAPI;
import me.dw1e.kbm.command.KBMCommand;
import me.dw1e.kbm.config.ConfigValue;
import me.dw1e.kbm.config.KBFile;
import me.dw1e.kbm.data.DataManager;
import me.dw1e.kbm.listener.PlayerStateListener;
import me.dw1e.kbm.listener.PotionListener;
import me.dw1e.kbm.listener.VelocityListener;
import me.dw1e.kbm.module.HitDetection;
import me.dw1e.kbm.module.LagCompensator;
import me.dw1e.kbm.packet.MisplaceHandler;
import me.dw1e.kbm.packet.PingHandler;
import me.dw1e.kbm.placeholder.KBMPlaceholder;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashSet;
import java.util.Set;

public final class KnockbackManager extends JavaPlugin {

    public static final String PREFIX = "§8[§eKBM§8]";

    private static KnockbackManager instance;

    private final Set<Listener> listeners = new HashSet<>();

    private KnockbackManagerAPI api;
    private DataManager dataManager;
    private MisplaceHandler misplaceHandler;
    private PingHandler pingHandler;
    private KBFile kbFile;
    private KBMPlaceholder kbmPlaceholder;
    private LagCompensator lagCompensator;
    private HitDetection hitDetection;

    private ProtocolManager protocolManager;

    private int tick;
    private BukkitTask tickTask;

    private boolean isAtLeast1_16, isAtLeast1_17;

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

        saveDefaultConfig();
        ConfigValue.updateConfig(getConfig());

        kbFile = new KBFile(this);
        kbFile.load(Bukkit.getConsoleSender());

        dataManager = new DataManager();
        dataManager.enable();

        api = new DefaultKnockbackManagerAPI(this);

        listeners.add(new PlayerStateListener(this));
        listeners.add(new PotionListener(this));
        listeners.add(new VelocityListener(this));

        checkDepends();

        if (protocolManager != null) {
            MinecraftVersion version = protocolManager.getMinecraftVersion();

            isAtLeast1_16 = version.isAtLeast(MinecraftVersion.NETHER_UPDATE);
            isAtLeast1_17 = version.isAtLeast(MinecraftVersion.CAVES_CLIFFS_1);

            misplaceHandler = new MisplaceHandler(this);
            misplaceHandler.enable();

            pingHandler = new PingHandler(this);
            pingHandler.enable();

            listeners.add(lagCompensator = new LagCompensator()); // 只有HitDetection需要用到, 所以放在这里
            listeners.add(hitDetection = new HitDetection(this, lagCompensator));

            hitDetection.enable();
        }

        listeners.forEach(listener -> Bukkit.getPluginManager().registerEvents(listener, this));

        PluginCommand pluginCommand = getCommand("kbm");
        if (pluginCommand != null) {
            KBMCommand kbmCommand = new KBMCommand(this);

            pluginCommand.setExecutor(kbmCommand);
            pluginCommand.setTabCompleter(kbmCommand);
        }

        tickTask = Bukkit.getScheduler().runTaskTimer(this, () -> {
            tick++;

            if (misplaceHandler != null) misplaceHandler.tick(tick);
            if (pingHandler != null) pingHandler.tick();
            if (hitDetection != null) hitDetection.onTick();
        }, 0L, 1L);
    }

    @Override
    public void onDisable() {
        HandlerList.unregisterAll(this);

        if (hitDetection != null) {
            hitDetection.disable();
            hitDetection = null;
        }

        lagCompensator = null;

        listeners.clear();

        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }

        if (kbmPlaceholder != null) {
            kbmPlaceholder.unregister();
            kbmPlaceholder = null;
        }

        if (pingHandler != null) {
            pingHandler.disable();
            pingHandler = null;
        }

        if (misplaceHandler != null) {
            misplaceHandler.disable();
            misplaceHandler = null;
        }

        api = null;

        if (dataManager != null) {
            dataManager.disable();
            dataManager = null;
        }

        kbFile = null;

        instance = null;
    }

    public void reload() {
        reloadConfig();
        ConfigValue.updateConfig(getConfig());
        if (hitDetection != null) hitDetection.loadConfig();
    }

    private void checkDepends() {
        PluginManager pluginManager = Bukkit.getPluginManager();
        Plugin pLib = pluginManager.getPlugin("ProtocolLib");

        if (pLib != null && pLib.isEnabled()) {
            String pLibDesc = pLib.getDescription().getVersion();
            int pLibVer = Integer.parseInt(pLibDesc.split("\\.")[0]);

            if (pLibVer < 5) {
                consoleLog("§c不支持的 ProtocolLib 版本: §e" + pLibDesc + "§c, 请使用 5.0.0 或更高的版本, 数据包功能将禁用!");
            } else {
                consoleLog("§a检测到 ProtocolLib §e" + pLibDesc + "§a, 数据包功能已启用!");

                protocolManager = ProtocolLibrary.getProtocolManager();
            }
        } else consoleLog("§c未检测到 ProtocolLib, 数据包功能将禁用!");

        if (pluginManager.getPlugin("PlaceholderAPI") != null) {
            kbmPlaceholder = new KBMPlaceholder(this);
            kbmPlaceholder.register();

            consoleLog("§a检测到 PlaceholderAPI, 占位符功能已启用!");
        }
    }

    public KnockbackManagerAPI getAPI() {
        return api;
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    public MisplaceHandler getPacketHandler() {
        return misplaceHandler;
    }

    public KBFile getKbFile() {
        return kbFile;
    }

    public int getTick() {
        return tick;
    }

    public ProtocolManager getProtocolManager() {
        return protocolManager;
    }

    public boolean isAtLeast1_16() {
        return isAtLeast1_16;
    }

    public boolean isAtLeast1_17() {
        return isAtLeast1_17;
    }

    public void consoleLog(String s) {
        Bukkit.getConsoleSender().sendMessage(PREFIX + " " + s);
    }
}
