package me.libraryaddict.limit.base;

import me.libraryaddict.limit.Main;
import me.libraryaddict.limit.utils.Location;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.HashMap;

public class StorageApi {

    private static Connection connection;
    private static JavaPlugin mainPlugin;
    private static HashMap<String, HashMap<Location, String>> markedBlocks = new HashMap<>();
    private static String mysqlDatabase, mysqlUsername, mysqlPassword, mysqlHost;
    private static boolean useMysql;

    public StorageApi(Main plugin) {
        mainPlugin = plugin;
    }

    private static Connection connectMysql() {
        try {
            Class.forName("com.mysql.jdbc.Driver").newInstance();
            String conn = "jdbc:mysql://" + mysqlHost + "/" + mysqlDatabase;
            return DriverManager.getConnection(conn, mysqlUsername, mysqlPassword);
        } catch (Exception ex) {
            System.err.println("[LectronCreative] Unknown error while fetching MySQL connection. Is the mysql details correct? "
                    + ex.getMessage());
        }
        return null;
    }

    private static Connection getConnection() {
        try {
            if (connection == null) {
                connection = connectMysql();
                DatabaseMetaData dbmd = connection.getMetaData();
                ResultSet rs = dbmd.getTables(null, null, "LimitCreative", null);
                if (!rs.next()) {
                    connection
                            .createStatement()
                            .execute(
                                    "CREATE TABLE IF NOT EXISTS LimitCreative (world VARCHAR(20), x INT(10), y INT(10), z INT(10), lore VARCHAR(300))");
                }
                return connection;
            }
            try {
                connection.createStatement().execute("DO 1");
            } catch (Exception ex) {
                connection = connectMysql();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return connection;
    }

    public static boolean isMarked(Block block) {
        return markedBlocks.containsKey(block.getWorld().getName())
                && markedBlocks.get(block.getWorld().getName()).containsKey(new Location(block));
    }

    public static void loadBlocksFromFlatfile() {
        File file = new File(mainPlugin.getDataFolder(), "blocks.yml");
        if (file.exists()) {
            try {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                for (String worldName : config.getKeys(false)) {
                    World world = Bukkit.getWorld(worldName);
                    if (world != null) {
                        for (String x : config.getConfigurationSection(worldName).getKeys(false)) {
                            for (String y : config.getConfigurationSection(worldName + "." + x).getKeys(false)) {
                                for (String z : config.getConfigurationSection(worldName + "." + x + "." + y).getKeys(false)) {
                                    if (!markedBlocks.containsKey(worldName)) {
                                        markedBlocks.put(worldName, new HashMap());
                                    }
                                    markedBlocks.get(worldName).put(
                                            new Location(Integer.parseInt(x), Integer.parseInt(y), Integer.parseInt(z)),
                                            config.getString(worldName + "." + x + "." + y + "." + z));
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void loadBlocksFromMysql() {
        try {
            getConnection();
            if (connection != null) {
                PreparedStatement stmt = connection.prepareStatement("SELECT * FROM LimitCreative WHERE `world`=?");
                for (World world : Bukkit.getWorlds()) {
                    stmt.setString(1, world.getName());
                    ResultSet rs = stmt.executeQuery();
                    while (rs.next()) {
                        if (!markedBlocks.containsKey(world.getName())) {
                            markedBlocks.put(world.getName(), new HashMap<>());
                        }
                        markedBlocks.get(world.getName()).put(new Location(rs.getInt("x"), rs.getInt("y"), rs.getInt("z")),
                                rs.getString("lore"));
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void markBlock(Block block, String msg) {
        markBlock(block.getWorld().getName(), new Location(block), msg);
    }

    public static void markBlock(final String world, final Location loc, final String msg) {
        if (!markedBlocks.containsKey(world)) {
            markedBlocks.put(world, new HashMap<>());
        }
        markedBlocks.get(world).put(loc, msg);
        new BukkitRunnable() {
            @Override
            public void run() {
                if (useMysql) {
                    try {
                        PreparedStatement stmt = getConnection().prepareStatement(
                                "INSERT INTO LimitCreative (`world`, `x`, `y`, `z`, `lore`) VALUES (?, ?, ?, ?, ?);");
                        stmt.setString(1, world);
                        stmt.setInt(2, loc.getX());
                        stmt.setInt(3, loc.getY());
                        stmt.setInt(4, loc.getZ());
                        stmt.setString(5, msg);
                        stmt.execute();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                } else {
                    File file = new File(mainPlugin.getDataFolder(), "blocks.yml");
                    try {
                        if (!file.exists()) {
                            file.createNewFile();
                        }
                        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                        config.set(world + "." + loc.getX() + "." + loc.getY() + "." + loc.getZ(), msg);
                        config.save(file);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }.runTaskAsynchronously(mainPlugin);
    }

    public static void saveBlocksToMysql() {
        for (String world : markedBlocks.keySet()) {
            for (Location loc : markedBlocks.get(world).keySet()) {
                markBlock(world, loc, markedBlocks.get(world).get(loc));
            }
        }
    }

    public static void setMainPlugin(JavaPlugin plugin) {
        mainPlugin = plugin;
    }

    public static void setMysqlDetails(String sqlUsername, String sqlPassword, String sqlHost, String sqlDatabase) {
        useMysql = true;
        mysqlDatabase = sqlDatabase;
        mysqlUsername = sqlUsername;
        mysqlHost = sqlHost;
        mysqlPassword = sqlPassword;
    }

    public static String unmarkBlock(Block block) {
        return unmarkBlock(block.getWorld().getName(), new Location(block));
    }

    public static String unmarkBlock(final String world, final Location loc) {
        String msg = markedBlocks.get(world).remove(loc);
        if (markedBlocks.get(world).isEmpty()) {
            markedBlocks.remove(world);
        }
        new BukkitRunnable() {
            @Override
            public void run() {
                if (useMysql) {
                    try {
                        PreparedStatement stmt = getConnection().prepareStatement(
                                "DELETE FROM `LimitCreative` WHERE `world`=? AND `x`=? AND `y`=? AND `z`=?");
                        stmt.setString(1, world);
                        stmt.setInt(2, loc.getX());
                        stmt.setInt(3, loc.getY());
                        stmt.setInt(4, loc.getZ());
                        stmt.execute();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                } else {
                    File file = new File(mainPlugin.getDataFolder(), "blocks.yml");
                    if (file.exists()) {
                        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                        String blockPath = world + "." + loc.getX() + "." + loc.getY() + "." + loc.getZ();
                        if (config.contains(blockPath)) {
                            config.set(blockPath, null);
                        }
                        try {
                            config.save(file);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }.runTaskAsynchronously(mainPlugin);
        return msg;
    }

}