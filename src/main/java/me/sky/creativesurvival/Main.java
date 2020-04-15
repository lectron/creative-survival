package me.sky.creativesurvival;

import me.sky.creativesurvival.base.InteractionListener;
import me.sky.creativesurvival.commands.ClearCreativeCommand;
import me.sky.creativesurvival.utils.Messages;
import me.sky.creativesurvival.utils.Options;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {
    public void onEnable() {
        new Options(this);
        new Messages(this);

        InteractionListener interactionListener = new InteractionListener(this);

        this.getCommand("clearlore").setExecutor(new ClearCreativeCommand(this, interactionListener));
    }

}
