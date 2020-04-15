package me.sky.creativesurvival.commands;

import me.sky.creativesurvival.Main;
import me.sky.creativesurvival.base.InteractionListener;
import me.sky.creativesurvival.utils.Messages;
import me.sky.creativesurvival.utils.nbt.NBTItemData;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ClearCreativeCommand implements CommandExecutor {

    private final Main plugin;
    private final InteractionListener interactionListener;

    public ClearCreativeCommand(Main plugin, InteractionListener interactionListener) {
        this.plugin = plugin;
        this.interactionListener = interactionListener;
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String d, String[] strings) {
        if (commandSender.getName().equals("CONSOLE")) {
            commandSender.sendMessage(ChatColor.RED + "Shove off console");
            return true;
        }
        if (commandSender.hasPermission("creativesurvival.clearlore")) {
            ItemStack item = ((Player) commandSender).getItemInHand();
            if (item.getType() == Material.AIR) {
                commandSender.sendMessage(Messages.get().getMessage("NotHoldingItem"));
                return false;
            }
            if (!NBTItemData.getList("CreativeSurvival", item).contains("CreativeItem")) {
                commandSender.sendMessage(Messages.get().getMessage("NoCreativeMessageFound"));
                return false;
            }
            if (NBTItemData.getList("CreativeSurvival", item).contains("CreativeLoreIndex")) {
                item = interactionListener.removeCreativeLore(item);
                item = NBTItemData.remove("CreativeSurvival", "CreativeLoreIndex", item);
            }
            item = NBTItemData.remove("CreativeSurvival", "CreativeItem", item);
            ((Player) commandSender).setItemInHand(item);
            commandSender.sendMessage(Messages.get().getMessage("RemovedCreativeMessage"));
        } else
            commandSender.sendMessage(Messages.get().getMessage("NoPermission"));
        return false;
    }
}
