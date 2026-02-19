package me.dw1e.kbm.config;

import me.dw1e.kbm.KnockbackManager;
import me.dw1e.kbm.util.Pair;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public final class KBFile {

    private final KnockbackManager plugin;
    private final FileConfiguration defaultConfig;
    private final Map<String, Pair<File, FileConfiguration>> kbMap = new HashMap<>();
    private final Map<String, KBProfile> kbProfileMap = new HashMap<>();

    public KBFile(KnockbackManager plugin) {
        this.plugin = plugin;

        try {
            defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(plugin.getResource("default.yml")));
        } catch (NullPointerException e) {
            plugin.consoleLog("§c读取预设KB文件时发生错误!");
            throw new RuntimeException(e.getMessage());
        }
    }

    public void load(CommandSender sender) {
        if (Files.notExists(plugin.getDataFolder().toPath().resolve("default.yml"))) {
            try {
                plugin.saveResource("default.yml", false);
            } catch (IllegalArgumentException e) {
                sender.sendMessage(KnockbackManager.PREFIX + " §c创建默认KB文件时发生错误!");
                throw new RuntimeException(e.getMessage());
            }
        }

        kbMap.clear();

        try (Stream<Path> paths = Files.list(Paths.get(plugin.getDataFolder().getPath()))) {
            paths.filter(path -> Files.isRegularFile(path) && path.toString().endsWith(".yml"))
                    .forEach(path -> kbMap.put(path.getFileName().toString().replace(".yml", ""),
                            new Pair<>(path.toFile(), YamlConfiguration.loadConfiguration(path.toFile()))));
        } catch (IOException e) {
            sender.sendMessage(KnockbackManager.PREFIX + " §c读取KB文件夹时发生错误!");
            throw new RuntimeException(e.getMessage());
        }

        kbMap.forEach((filename, pair) -> {
            FileConfiguration fileConfig = pair.getValue();

            fileConfig.options().copyDefaults(true).copyHeader(true);
            fileConfig.addDefaults(defaultConfig); // 将目前配置与默认配置比对, 查漏补缺

            if (fileConfig.get("horizontal.projectile_multiplier") != null) {
                plugin.consoleLog("§a已将旧版配置文件 §e" + filename + " §a更新!");

                fileConfig.set("projectile.horizontal_multiplier", fileConfig.getDouble("horizontal.projectile_multiplier"));
                fileConfig.set("projectile.vertical_multiplier", fileConfig.getDouble("vertical.projectile_multiplier"));
                fileConfig.set("projectile.direction_override", fileConfig.getBoolean("projectile_knockback_direction_override"));

                fileConfig.set("horizontal.projectile_multiplier", null);
                fileConfig.set("vertical.projectile_multiplier", null);
                fileConfig.set("projectile_knockback_direction_override", null);
            }

            // 限制最大数值
            fileConfig.set("packet.misplace.distance", Math.max(0.0, Math.min(1.0, fileConfig.getDouble("packet.misplace.distance"))));
            fileConfig.set("packet.delay.ticks", Math.max(1, Math.min(5, fileConfig.getInt("packet.delay.ticks"))));
            fileConfig.set("potion.horizontal_multiplier", Math.max(-8.0, Math.min(8.0, fileConfig.getDouble("potion.horizontal_multiplier"))));
            fileConfig.set("potion.vertical_multiplier", Math.max(-7.0, Math.min(7.0, fileConfig.getDouble("potion.vertical_multiplier"))));

            // 删除无用的数值
            fileConfig.getKeys(true).stream()
                    .filter(key -> !fileConfig.isConfigurationSection(key))
                    .filter(key -> !defaultConfig.isSet(key))
                    .forEach(key -> fileConfig.set(key, null));

            // 在删除掉无用配置后如果section为空, 那么把section也删了
            fileConfig.getKeys(true).stream()
                    .filter(fileConfig::isConfigurationSection) // 只处理section
                    .sorted((a, b) -> Integer.compare(b.length(), a.length())) // 按深度倒序(子节点优先)
                    .forEach(key -> {
                        if (fileConfig.getConfigurationSection(key).getKeys(false).isEmpty()) {
                            fileConfig.set(key, null);
                        }
                    });

            kbProfileMap.put(filename, new KBProfile(fileConfig));

            try {
                fileConfig.save(pair.getKey());
            } catch (IOException e) {
                sender.sendMessage(KnockbackManager.PREFIX + " §c保存 " + filename + " 失败, 请查看控制台详细错误日志!");
                throw new RuntimeException(e.getMessage());
            }
        });
    }

    public void create(String filename, CommandSender sender) {
        if (kbMap.containsKey(filename)) {
            sender.sendMessage(KnockbackManager.PREFIX + " §cKB文件 " + filename + " 已存在!");
            return;
        }

        File file = new File(plugin.getDataFolder(), filename + ".yml");

        if (file.getParentFile().mkdir()) {
            try {
                if (!file.createNewFile())
                    sender.sendMessage(KnockbackManager.PREFIX + " §c创建KB文件 " + filename + " 失败!");
            } catch (IOException e) {
                sender.sendMessage(KnockbackManager.PREFIX + " §c创建KB文件 " + filename + " 失败, 请查看控制台详细错误日志!");
                throw new RuntimeException(e.getMessage());
            }
        }

        FileConfiguration fileConfig = YamlConfiguration.loadConfiguration(file);

        fileConfig.options().copyDefaults(true).copyHeader(true);
        fileConfig.addDefaults(defaultConfig);

        try {
            fileConfig.save(file);
        } catch (IOException e) {
            sender.sendMessage(KnockbackManager.PREFIX + " §c保存 " + filename + " 失败, 请查看控制台详细错误日志!");
            throw new RuntimeException(e.getMessage());
        }

        kbMap.put(filename, new Pair<>(file, fileConfig));
        kbProfileMap.put(filename, new KBProfile(fileConfig));

        sender.sendMessage(KnockbackManager.PREFIX + " §a创建 " + filename + " 成功!");
    }

    public void delete(String filename, CommandSender sender) {
        if (filename.equals("default")) {
            sender.sendMessage(KnockbackManager.PREFIX + " §c默认KB文件不允许删除!");
            return;
        }

        if (!kbMap.containsKey(filename)) {
            sender.sendMessage(KnockbackManager.PREFIX + " §cKB文件 " + filename + " 不存在!");
            return;
        }

        plugin.getDataManager().getAllData().stream()
                .filter(data -> data.getProfile().equals(filename))
                .forEach(data -> data.setProfile("default"));

        sender.sendMessage(KnockbackManager.PREFIX + (kbMap.get(filename).getKey().delete()
                ? " §a删除 " + filename + " 成功!" : " §c删除 " + filename + " 失败!"));

        kbProfileMap.remove(filename);
        kbMap.remove(filename);
    }

    public void save(String filename, CommandSender sender) {
        if (!kbMap.containsKey(filename)) {
            sender.sendMessage(KnockbackManager.PREFIX + " §cKB文件 " + filename + " 不存在!");
            return;
        }

        try {
            kbMap.get(filename).getValue().save(kbMap.get(filename).getKey());
            kbProfileMap.get(filename).updateConfig(getConfig(filename));
        } catch (IOException e) {
            sender.sendMessage(KnockbackManager.PREFIX + " §c保存 " + filename + " 失败, 请查看控制台详细错误日志!");
            throw new RuntimeException(e.getMessage());
        }
    }

    public void reload(String filename, CommandSender sender) {
        if (filename.equals("*")) {
            load(sender);

            plugin.getDataManager().getAllData().stream()
                    .filter(data -> !kbMap.containsKey(data.getProfile()))
                    .forEach(data -> data.setProfile("default"));

            sender.sendMessage(KnockbackManager.PREFIX + " §a已重载所有KB文件!");
            return;
        }

        if (!kbMap.containsKey(filename)) {
            sender.sendMessage(KnockbackManager.PREFIX + " §cKB文件 " + filename + " 不存在!");
            return;
        }

        try {
            kbMap.get(filename).getValue().load(kbMap.get(filename).getKey());
            kbProfileMap.get(filename).updateConfig(getConfig(filename));
        } catch (InvalidConfigurationException | IOException e) {
            sender.sendMessage(KnockbackManager.PREFIX + " §c重载 " + filename + " 失败, 请查看控制台详细错误日志!");
            throw new RuntimeException(e.getMessage());
        }

        sender.sendMessage(KnockbackManager.PREFIX + " §a重载 " + filename + " 成功!");
    }

    public Map<String, Pair<File, FileConfiguration>> getKbMap() {
        return kbMap;
    }

    public FileConfiguration getConfig(String name) {
        return kbMap.get(name).getValue();
    }

    public KBProfile getProfile(String name) {
        return kbProfileMap.get(name);
    }
}
