package me.libraryaddict.limit.utils.nbt.item_1_13;

import net.minecraft.server.v1_13_R2.NBTTagCompound;
import org.bukkit.craftbukkit.v1_13_R2.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;

import java.util.Set;

public class NBTItemData_1_13 {

    public static ItemStack set(String base, String path, String i, ItemStack itemStack) {
        net.minecraft.server.v1_13_R2.ItemStack item = CraftItemStack.asNMSCopy(itemStack);
        if (!item.hasTag()) {
            item.setTag(new NBTTagCompound());
        }
        NBTTagCompound tag = item.getTag();
        if (!tag.hasKey(base)) {
            tag.set(base, new NBTTagCompound());
        }
        NBTTagCompound info = tag.getCompound(base);
        info.setString(path, i);
        item.setTag(tag);
        return CraftItemStack.asCraftMirror(item);
    }

    public static ItemStack remove(String base, String path, ItemStack itemStack) {
        net.minecraft.server.v1_13_R2.ItemStack item = CraftItemStack.asNMSCopy(itemStack);
        if (item.hasTag()) {
            NBTTagCompound tag = item.getTag();
            if (tag.hasKey(base)) {
                NBTTagCompound info = tag.getCompound(base);
                info.remove(path);
                item.setTag(tag);
            }
        }
        return CraftItemStack.asCraftMirror(item);
    }

    public static String get(String base, String path, ItemStack itemStack) {
        net.minecraft.server.v1_13_R2.ItemStack item = CraftItemStack.asNMSCopy(itemStack);
        if (!item.hasTag()) {
            item.setTag(new NBTTagCompound());
        }
        NBTTagCompound tag = item.getTag();
        if (!tag.hasKey(base)) {
            tag.set(base, new NBTTagCompound());
        }
        NBTTagCompound info = tag.getCompound(base);
        return info.getString(path);
    }

    public static Set<String> getList(String base, ItemStack itemStack) {
        net.minecraft.server.v1_13_R2.ItemStack item = CraftItemStack.asNMSCopy(itemStack);
        if (!item.hasTag()) {
            item.setTag(new NBTTagCompound());
        }
        NBTTagCompound tag = item.getTag();
        if (!tag.hasKey(base)) {
            tag.set(base, new NBTTagCompound());
        }
        NBTTagCompound info = tag.getCompound(base);
        return info.getKeys();
    }

    public static boolean isDefault(String base, ItemStack itemStack) {
        net.minecraft.server.v1_13_R2.ItemStack item = CraftItemStack.asNMSCopy(itemStack);
        if (!item.hasTag()) {
            return true;
        }
        NBTTagCompound tag = item.getTag();
        if (!tag.hasKey(base)) {
            return true;
        }
        return false;
    }

}
