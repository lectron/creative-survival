package me.libraryaddict.limit.commands;

import me.libraryaddict.limit.Main;
import me.libraryaddict.limit.utils.Messages;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ClearLoreCommand implements CommandExecutor {

    private Main plugin;

    public ClearLoreCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String d, String[] strings) {
        if (commandSender.getName().equals("CONSOLE")) {
            commandSender.sendMessage(ChatColor.RED + "Shove off console");
            return true;
        }
        if (commandSender.hasPermission("limitcreative.clearlore")) {
            String creativeMessage = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("ItemMessage"))
                    .replace("%Name%", "");
            ItemStack item = ((Player) commandSender).getItemInHand();
            if (item.getType() != Material.AIR) {
                boolean removed = false;
                if (item.hasItemMeta() && item.getItemMeta().hasLore()) {
                    ItemMeta meta = item.getItemMeta();
                    Iterator<String> itel = meta.getLore().iterator();
                    List<String> lore = new ArrayList<>();
                    while (itel.hasNext()) {
                        String s = itel.next();
                        if (s.startsWith(creativeMessage)) {
                            removed = true;
                        } else
                            lore.add(s);
                    }
                    meta.setLore(lore);
                    item.setItemMeta(meta);
                }
                if (!removed)
                    commandSender.sendMessage(Messages.get().getMessage("NoCreativeMessageFound"));
                else
                    commandSender.sendMessage(Messages.get().getMessage("RemovedCreativeMessage"));
            } else
                commandSender.sendMessage(Messages.get().getMessage("NotHoldingItem"));
        } else
            commandSender.sendMessage(Messages.get().getMessage("NoPermission"));
        return false;
    }
}
