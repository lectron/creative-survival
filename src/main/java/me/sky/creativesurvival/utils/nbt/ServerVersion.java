package me.sky.creativesurvival.utils.nbt;

import org.bukkit.Bukkit;

public enum ServerVersion {
    VERSION_1_13,
    VERSION_1_14,
    VERSION_1_15;

    public static ServerVersion getVersion() {
        String a = Bukkit.getServer().getClass().getPackage().getName();
        String version = a.substring(a.lastIndexOf('.') + 1);

        if (version.contains("1_13")) {
            return VERSION_1_13;
        } else if (version.contains("1_14")) {
            return VERSION_1_14;
        } else {
            return VERSION_1_15;
        }
    }
}
