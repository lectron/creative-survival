package me.libraryaddict.limit;

import me.libraryaddict.limit.base.InteractionListener;
import me.libraryaddict.limit.base.StorageApi;
import me.libraryaddict.limit.commands.ClearLoreCommand;
import me.libraryaddict.limit.commands.LimitCreativeCommand;
import me.libraryaddict.limit.utils.Messages;
import me.libraryaddict.limit.utils.Options;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {
    public void onEnable() {
        new Options(this);
        new Messages(this);

        new InteractionListener(this);
        new StorageApi(this);

        this.getCommand("clearlore").setExecutor(new ClearLoreCommand(this));
        this.getCommand("limitcreativeconvert").setExecutor(new LimitCreativeCommand(this));

        if (getConfig().getBoolean("SaveBlocks")) {
            Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> {
                if (getConfig().getBoolean("UseMysql")) {
                    StorageApi.setMysqlDetails(getConfig().getString("MysqlUsername"),
                            getConfig().getString("MysqlPassword"), getConfig().getString("MysqlHost"), getConfig()
                                    .getString("MysqlDatabase"));
                    StorageApi.loadBlocksFromMysql();
                } else {
                    StorageApi.loadBlocksFromFlatfile();
                }
            }, 10);
        }
    }

}
