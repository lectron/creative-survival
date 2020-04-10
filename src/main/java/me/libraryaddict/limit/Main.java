package me.libraryaddict.limit;

import me.libraryaddict.limit.base.InteractionListener;
import me.libraryaddict.limit.commands.ClearCreativeCommand;
import me.libraryaddict.limit.utils.Messages;
import me.libraryaddict.limit.utils.Options;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {
    public void onEnable() {
        new Options(this);
        new Messages(this);

        new InteractionListener(this);

        this.getCommand("clearcreative").setExecutor(new ClearCreativeCommand(this));
    }

}
