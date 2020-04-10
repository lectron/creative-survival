package me.libraryaddict.limit.commands;

import me.libraryaddict.limit.Main;
import me.libraryaddict.limit.utils.Messages;
import me.libraryaddict.limit.utils.nbt.NBTItemData;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ClearCreativeCommand implements CommandExecutor {

    private Main plugin;

    public ClearCreativeCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String d, String[] strings) {
        if (commandSender.getName().equals("CONSOLE")) {
            commandSender.sendMessage(ChatColor.RED + "Shove off console");
            return true;
        }
        if (commandSender.hasPermission("limitcreative.clearcreative")) {
            ItemStack item = ((Player) commandSender).getItemInHand();
            if (NBTItemData.getList("LectronCreative", item).contains("CreativeItem")) {
                commandSender.sendMessage(Messages.get().getMessage("NoCreativeMessageFound"));
                return false;
            }
            if (item.getType() != Material.AIR) {
                ((Player) commandSender).setItemInHand(NBTItemData.remove("LectronCreative", "CreativeItem", item));
                commandSender.sendMessage(Messages.get().getMessage("RemovedCreativeMessage"));
            } else
                commandSender.sendMessage(Messages.get().getMessage("NotHoldingItem"));
        } else
            commandSender.sendMessage(Messages.get().getMessage("NoPermission"));
        return false;
    }
}
