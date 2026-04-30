package me.dw1e.kbm.config;

import me.dw1e.kbm.KnockbackManager;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class KBLoader {

    private final Path KB_PATH;

    private final KnockbackManager plugin;
    private final FileConfiguration defaultConfig;
    private final Map<String, KB> kbMap = new HashMap<>();

    public KBLoader(KnockbackManager plugin) {
        this.plugin = plugin;

        KB_PATH = plugin.getDataFolder().toPath().resolve("knockback");

        try {
            defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(plugin.getResource("knockback/default.yml")));
        } catch (NullPointerException e) {
            plugin.consoleLog("§c读取预设KB文件时发生错误!");
            throw new RuntimeException(e.getMessage());
        }
    }

    public void load(CommandSender sender) {
        if (Files.notExists(KB_PATH.resolve("default.yml"))) {
            try {
                Files.createDirectories(KB_PATH);

                Path defaultFile = KB_PATH.resolve("default.yml");

                if (Files.notExists(defaultFile)) {
                    plugin.saveResource("knockback/default.yml", false);
                }

            } catch (IllegalArgumentException | IOException e) {
                if (sender != null) sender.sendMessage(ConfigValue.PREFIX + " §c创建默认KB文件时发生错误!");
                throw new RuntimeException(e.getMessage());
            }
        }

        kbMap.clear();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(KB_PATH, "*.yml")) {
            for (Path path : stream) {
                KB kb = new KB(path.toFile());
                kbMap.put(kb.getName(), kb);

                String fileName = kb.getFile().getName();
                FileConfiguration config = kb.getConfig();

                config.options().copyDefaults(true).copyHeader(true);
                config.addDefaults(defaultConfig); // 将目前配置与默认配置比对, 查漏补缺

                // 限制最大数值
                config.set("packet.misplace.distance", Math.max(0.0, Math.min(1.0, config.getDouble("packet.misplace.distance"))));
                config.set("packet.delay.ticks", Math.max(1, Math.min(5, config.getInt("packet.delay.ticks"))));
                config.set("potion.horizontal_multiplier", Math.max(-8.0, Math.min(8.0, config.getDouble("potion.horizontal_multiplier"))));
                config.set("potion.vertical_multiplier", Math.max(-7.0, Math.min(7.0, config.getDouble("potion.vertical_multiplier"))));

                // 删除无用的数值
                config.getKeys(true).stream()
                        .filter(key -> !config.isConfigurationSection(key))
                        .filter(key -> !defaultConfig.isSet(key))
                        .forEach(key -> config.set(key, null));

                // 在删除掉无用配置后如果section为空, 那么把section也删了
                config.getKeys(true).stream()
                        .filter(config::isConfigurationSection) // 只处理section
                        .sorted((a, b) -> Integer.compare(b.length(), a.length())) // 按深度倒序(子节点优先)
                        .forEach(key -> {
                            if (config.getConfigurationSection(key).getKeys(false).isEmpty()) {
                                config.set(key, null);
                            }
                        });

                try {
                    config.save(kb.getFile());
                } catch (IOException e) {
                    sender.sendMessage(ConfigValue.PREFIX + " §c保存 " + fileName + " 失败, 请查看控制台详细错误日志!");
                    throw new RuntimeException(e.getMessage());
                }
            }

        } catch (IOException e) {
            sender.sendMessage(ConfigValue.PREFIX + " §c读取KB文件夹时发生错误!");
            throw new RuntimeException(e);
        }
    }

    public void create(String filename, CommandSender sender) {
        if (kbMap.containsKey(filename)) {
            sender.sendMessage(ConfigValue.PREFIX + " §cKB文件 " + filename + " 已存在!");
            return;
        }

        File file = new File(KB_PATH.toFile(), filename + ".yml");

        if (file.getParentFile().mkdir()) {
            try {
                if (!file.createNewFile()) {
                    sender.sendMessage(ConfigValue.PREFIX + " §c创建KB文件 " + filename + " 失败!");
                }
            } catch (IOException e) {
                sender.sendMessage(ConfigValue.PREFIX + " §c创建KB文件 " + filename + " 失败, 请查看控制台详细错误日志!");
                throw new RuntimeException(e.getMessage());
            }
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        config.options().copyDefaults(true).copyHeader(true);
        config.addDefaults(defaultConfig);

        try {
            config.save(file);
        } catch (IOException e) {
            sender.sendMessage(ConfigValue.PREFIX + " §c保存 " + filename + " 失败, 请查看控制台详细错误日志!");
            throw new RuntimeException(e.getMessage());
        }

        kbMap.put(filename, new KB(file));

        sender.sendMessage(ConfigValue.PREFIX + " §a创建 " + filename + " 成功!");
    }

    public void delete(String filename, CommandSender sender) {
        if (filename.equals("default")) {
            sender.sendMessage(ConfigValue.PREFIX + " §c默认KB文件不允许删除!");
            return;
        }

        if (!kbMap.containsKey(filename)) {
            sender.sendMessage(ConfigValue.PREFIX + " §cKB文件 " + filename + " 不存在!");
            return;
        }

        plugin.getDataManager().getAllData().stream()
                .filter(data -> data.getProfile().equals(filename))
                .forEach(data -> data.setProfile("default"));

        sender.sendMessage(ConfigValue.PREFIX + (kbMap.get(filename).getFile().delete()
                ? " §a删除 " + filename + " 成功!" : " §c删除 " + filename + " 失败!"));

        kbMap.remove(filename);
    }

    public void save(String filename, CommandSender sender) {
        if (!kbMap.containsKey(filename)) {
            if (sender != null) sender.sendMessage(ConfigValue.PREFIX + " §cKB文件 " + filename + " 不存在!");
            return;
        }

        try {
            KB kb = kbMap.get(filename);

            kb.getConfig().save(kb.getFile());
            kb.getProfile().updateConfig(kb.getConfig());
        } catch (IOException e) {
            if (sender != null)
                sender.sendMessage(ConfigValue.PREFIX + " §c保存 " + filename + " 失败, 请查看控制台详细错误日志!");
            throw new RuntimeException(e.getMessage());
        }
    }

    public void reload(String filename, CommandSender sender) {
        if (filename.equals("*")) {
            load(sender);

            plugin.getDataManager().getAllData().stream()
                    .filter(data -> !kbMap.containsKey(data.getProfile()))
                    .forEach(data -> data.setProfile("default"));

            if (sender != null) sender.sendMessage(ConfigValue.PREFIX + " §a已重载所有KB文件!");
            return;
        }

        if (!kbMap.containsKey(filename)) {
            if (sender != null) sender.sendMessage(ConfigValue.PREFIX + " §cKB文件 " + filename + " 不存在!");
            return;
        }

        try {
            KB kb = kbMap.get(filename);

            kb.getConfig().load(kb.getFile());
            kb.getProfile().updateConfig(kb.getConfig());
        } catch (Exception e) {
            if (sender != null)
                sender.sendMessage(ConfigValue.PREFIX + " §c重载 " + filename + " 失败, 请查看控制台详细错误日志!");
            throw new RuntimeException(e.getMessage());
        }

        if (sender != null) sender.sendMessage(ConfigValue.PREFIX + " §a重载 " + filename + " 成功!");
    }

    public Map<String, KB> getKbMap() {
        return kbMap;
    }

    public FileConfiguration getConfig(String name) {
        KB kb = kbMap.get(name);
        return kb != null ? kb.getConfig() : null;
    }

    public KBProfile getProfile(String name) {
        KB kb = kbMap.get(name);
        return kb != null ? kb.getProfile() : null;
    }

}
