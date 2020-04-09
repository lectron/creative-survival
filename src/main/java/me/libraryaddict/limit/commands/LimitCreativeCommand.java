package me.libraryaddict.limit.commands;

import me.libraryaddict.limit.Main;
import me.libraryaddict.limit.base.StorageApi;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class LimitCreativeCommand implements CommandExecutor {

    private Main plugin;

    public LimitCreativeCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if (commandSender.hasPermission("limitcreative.convert")) {
            commandSender.sendMessage(ChatColor.RED + "Now converting flatfile to mysql.. You may see lag");
            StorageApi.loadBlocksFromFlatfile();
            StorageApi.saveBlocksToMysql();
            commandSender.sendMessage(ChatColor.RED + "Converted!");
        }
        return false;
    }
}
