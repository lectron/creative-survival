package me.sky.creativesurvival.utils;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Collections;
import java.util.List;

public class Messages {

    private Config config;

    private static Messages instance;

    public Messages(Plugin plugin) {
        instance = this;
        config = new Config("plugins/" + plugin.getName(), "messages.yml", plugin);
        config.create();
        if (!config.exists()) {
            config.setDefault("messages.yml");
            config.getConfig().options().copyDefaults(true);
            config.saveConfig();
            config.reloadConfig();
        } else {
            Config c = new Config("plugins/" + plugin.getName(), "temp.yml", plugin);
            c.create();
            c.setDefault("messages.yml");
            c.getConfig().options().copyDefaults(true);
            for (String key : c.getConfig().getKeys(false)) {
                if (!config.getConfig().contains(key)) {
                    config.getConfig().set(key, c.getConfig().getString(key));
                }
            }
            config.saveConfig();
            config.reloadConfig();
        }
    }

    public static Messages get() {
        return instance;
    }

    public String getMessage(String s) {
        if (!config.getConfig().contains(s)) {
            config.getConfig().set(s, "&4Example Message");
            config.saveConfig();
            config.reloadConfig();
            return "§cMessage does not exist!";
        }
        return ChatColor.translateAlternateColorCodes('&', config.getConfig().getString(s));
    }

    public String getMessage(Player player, String s) {
        String msg = getMessage(s);
        msg = msg.replace("%player%", player.getName());
        return msg;
    }

    public List<String> getMessageList(String s) {
        if (!config.getConfig().contains(s)) {
            config.getConfig().set(s, Collections.singletonList("&4Example Message"));
            config.saveConfig();
            config.reloadConfig();
            return Collections.singletonList("§cMessage does not exist!");
        }
        List<String> lore = config.getConfig().getStringList(s);
        lore.replaceAll(s1 -> ChatColor.translateAlternateColorCodes('&', s1));
        return lore;
    }

    public List<String> getMessageList(Player player, String s) {
        List<String> msg = getMessageList(s);
        msg.replaceAll(s1 -> s1.replace("%player%", player.getName()));
        return msg;
    }

    public Config getConfig() {
        return config;
    }

    public void set(String key, String value) {
        config.getConfig().set(key, value);
        config.saveConfig();
        config.reloadConfig();
    }
}
