package me.sky.creativesurvival.utils;

import org.bukkit.plugin.Plugin;

import java.util.List;

public class Options {

    private Config config;
    private Plugin plugin;

    private static Options instance;

    public Options(Plugin plugin) {
        this.plugin = plugin;
        instance = this;
        config = new Config("plugins/" + plugin.getName(), "config.yml", plugin);
        config.create();
        if (!config.exists()) {
            config.setDefault("config.yml");
            config.getConfig().options().copyDefaults(true);
        } else {
            Config c = new Config("plugins/" + plugin.getName(), "temp.yml", plugin);
            c.create();
            c.setDefault("config.yml");
            c.getConfig().options().copyDefaults(true);
            for (String key : c.getConfig().getKeys(false)) {
                if (!config.getConfig().contains(key)) {
                    config.getConfig().set(key, c.getConfig().get(key));
                }
            }
        }
        config.saveConfig();
        config.reloadConfig();
    }

    public static Options get() {
        return instance;
    }

    public String getString(String name) {
        return config.getConfig().getString(name);
    }

    public int getInt(String name) {
        return config.getConfig().getInt(name);
    }

    public List<Integer> getIntList(String name) {
        return config.getConfig().getIntegerList(name);
    }

    public float getFloat(String name) {
        return (float) config.getConfig().getDouble(name);
    }

    public Config getConfig() {
        return config;
    }
}
