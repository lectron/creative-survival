package me.sky.creativesurvival.utils;

import com.google.common.base.Charsets;
import com.google.common.io.ByteStreams;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginAwareness;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.logging.Level;

public class Config {

    private File f;
    private FileConfiguration cfg;

    private String path_;
    private String fileName_;

    private Plugin main;

    public Config(String path, String fileName, Plugin javaPluginExtender) {
        main = javaPluginExtender;

        path_ = path;
        fileName_ = fileName;
    }

    public void reloadConfig() {
        cfg = YamlConfiguration.loadConfiguration(f);
        InputStream defConfigStream = this.getResource(fileName_);
        if (defConfigStream != null) {
            cfg.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(defConfigStream, Charsets.UTF_8)));
        }
    }

    public Config create() {
        f = new File(path_, fileName_);
        cfg = YamlConfiguration.loadConfiguration(f);
        return this;
    }

    public void setDefault(String filename) {
        InputStream defConfigStream = main.getResource(filename);
        if (defConfigStream == null)
            return;
        YamlConfiguration defConfig;
        if ((isStrictlyUTF8())) {
            defConfig = YamlConfiguration
                    .loadConfiguration(new InputStreamReader(defConfigStream,
                            Charsets.UTF_8));
        } else {
            defConfig = new YamlConfiguration();
            byte[] contents;
            try {
                contents = ByteStreams.toByteArray(defConfigStream);
            } catch (IOException e) {
                main.getLogger().log(Level.SEVERE,
                        "Unexpected failure reading " + filename, e);
                return;
            }
            String text = new String(contents, Charset.defaultCharset());
            if (!(text.equals(new String(contents, Charsets.UTF_8)))) {
                main.getLogger()
                        .warning(
                                "Default system encoding may have misread " + filename + " from plugin jar");
            }
            try {
                defConfig.loadFromString(text);
            } catch (InvalidConfigurationException e) {
                main.getLogger().log(Level.SEVERE,
                        "Cannot load configuration from jar", e);
            }
        }
        cfg.setDefaults(defConfig);
    }

    private boolean isStrictlyUTF8() {
        return main.getDescription().getAwareness().contains(
                PluginAwareness.Flags.UTF8);
    }

    public void saveConfig() {
        try {
            cfg.save(f);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public FileConfiguration getConfig() {
        return cfg;
    }

    public File toFile() {
        return f;
    }

    public void saveDefaultConfig() {
        if (!exists()) {
            saveResource(fileName_, false);
        }
    }

    public void saveResource(String resourcePath, boolean replace) {
        if (resourcePath != null && !resourcePath.equals("")) {
            resourcePath = resourcePath.replace('\\', '/');
            InputStream in = this.getResource(resourcePath);
            if (in == null) {
                throw new IllegalArgumentException("The embedded resource '" + resourcePath + "' cannot be found in " + toFile());
            } else {
                File outFile = new File(path_, resourcePath);
                int lastIndex = resourcePath.lastIndexOf(47);
                File outDir = new File(path_, resourcePath.substring(0, lastIndex >= 0 ? lastIndex : 0));
                if (!outDir.exists()) {
                    outDir.mkdirs();
                }
                try {
                    if (outFile.exists() && !replace) {
                        main.getLogger().log(Level.WARNING, "Could not save " + outFile.getName() + " to " + outFile + " because " + outFile.getName() + " already exists.");
                    } else {
                        OutputStream out = new FileOutputStream(outFile);
                        byte[] buf = new byte[1024];

                        int len;
                        while ((len = in.read(buf)) > 0) {
                            out.write(buf, 0, len);
                        }
                        out.close();
                        in.close();
                    }
                } catch (IOException var10) {
                    main.getLogger().log(Level.SEVERE, "Could not save " + outFile.getName() + " to " + outFile, var10);
                }
            }
        } else {
            throw new IllegalArgumentException("ResourcePath cannot be null or empty");
        }
    }

    public InputStream getResource(String filename) {
        if (filename == null) {
            throw new IllegalArgumentException("Filename cannot be null");
        } else {
            try {
                URL url = this.getClass().getClassLoader().getResource(filename);
                if (url == null) {
                    return null;
                } else {
                    URLConnection connection = url.openConnection();
                    connection.setUseCaches(false);
                    return connection.getInputStream();
                }
            } catch (IOException var4) {
                return null;
            }
        }
    }

    public boolean exists() {
        return f.exists();
    }
}
