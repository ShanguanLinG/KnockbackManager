package me.dw1e.kbm.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public final class KB {
    private final File file;
    private final FileConfiguration config;
    private final KBProfile profile;
    private final String name;

    public KB(File file) {
        this.file = file;
        config = YamlConfiguration.loadConfiguration(file);
        profile = new KBProfile(config);
        name = file.getName().replace(".yml", "");
    }

    public File getFile() {
        return file;
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public KBProfile getProfile() {
        return profile;
    }

    public String getName() {
        return name;
    }
}
