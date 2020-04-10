package me.sky.creativesurvival.utils.nbt;

import me.sky.creativesurvival.utils.nbt.item_1_13.NBTItemData_1_13;
import me.sky.creativesurvival.utils.nbt.item_1_14.NBTItemData_1_14;
import me.sky.creativesurvival.utils.nbt.item_1_15.NBTItemData_1_15;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.Set;

public class NBTItemData {

    private static final ServerVersion version = ServerVersion.getVersion();

    public static ItemStack set(String base, String path, String i, ItemStack itemStack) {
        switch (version) {
            case VERSION_1_13:
                return NBTItemData_1_13.set(base, path, i, itemStack);
            case VERSION_1_14:
                return NBTItemData_1_14.set(base, path, i, itemStack);
            case VERSION_1_15:
                return NBTItemData_1_15.set(base, path, i, itemStack);
        }
        return itemStack;
    }

    public static ItemStack remove(String base, String path, ItemStack itemStack) {
        switch (version) {
            case VERSION_1_13:
                return NBTItemData_1_13.remove(base, path, itemStack);
            case VERSION_1_14:
                return NBTItemData_1_14.remove(base, path, itemStack);
            case VERSION_1_15:
                return NBTItemData_1_15.remove(base, path, itemStack);
        }
        return itemStack;
    }

    public static String get(String base, String path, ItemStack itemStack) {
        switch (version) {
            case VERSION_1_13:
                return NBTItemData_1_13.get(base, path, itemStack);
            case VERSION_1_14:
                return NBTItemData_1_14.get(base, path, itemStack);
            case VERSION_1_15:
                return NBTItemData_1_15.get(base, path, itemStack);
        }
        return null;
    }

    public static Set<String> getList(String base, ItemStack itemStack) {
        switch (version) {
            case VERSION_1_13:
                return NBTItemData_1_13.getList(base, itemStack);
            case VERSION_1_14:
                return NBTItemData_1_14.getList(base, itemStack);
            case VERSION_1_15:
                return NBTItemData_1_15.getList(base, itemStack);
        }
        return new HashSet<>();
    }

    public static boolean isDefault(String base, ItemStack itemStack) {
        switch (version) {
            case VERSION_1_13:
                return NBTItemData_1_13.isDefault(base, itemStack);
            case VERSION_1_14:
                return NBTItemData_1_14.isDefault(base, itemStack);
            case VERSION_1_15:
                return NBTItemData_1_15.isDefault(base, itemStack);
        }
        return false;
    }

}
