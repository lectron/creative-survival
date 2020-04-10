package me.libraryaddict.limit;

import me.libraryaddict.limit.base.InteractionListener;
import me.libraryaddict.limit.commands.ClearLoreCommand;
import me.libraryaddict.limit.commands.LimitCreativeCommand;
import me.libraryaddict.limit.utils.Messages;
import me.libraryaddict.limit.utils.Options;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {
    public void onEnable() {
        new Options(this);
        new Messages(this);

        new InteractionListener(this);

        this.getCommand("clearlore").setExecutor(new ClearLoreCommand(this));
        this.getCommand("limitcreativeconvert").setExecutor(new LimitCreativeCommand(this));
    }

}
