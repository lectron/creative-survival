package me.libraryaddict.limit.base;

import com.google.common.base.Objects;
import com.mysql.jdbc.StringUtils;
import me.libraryaddict.limit.utils.Messages;
import me.libraryaddict.limit.utils.Options;
import me.libraryaddict.limit.utils.nbt.NBTItemData;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Furnace;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class InteractionListener implements Listener {

    private final ArrayList<Material> disallowedItems = new ArrayList<>();
    private final List<String> disallowedWorlds;
    private final JavaPlugin plugin;
    private final Map<Block, String> markedBlocks = new HashMap<>();

    public InteractionListener(JavaPlugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        disallowedWorlds = getConfig().getStringList("WorldsDisabled");
        for (String disallowed : getConfig().getStringList("DisabledItems")) {
            try {
                disallowedItems.add(Material.getMaterial(disallowed.toUpperCase()));
            } catch (Exception ex) {
//                try {
//                    disallowedItems.add(Material.getMaterial(Integer.parseInt(disallowed)));
//                } catch (Exception e) {
//                }
                System.out.print("[LectronCreative] Cannot parse " + disallowed + " to a valid material");
            }
        }
    }

    private boolean checkEntity(Entity entity) {
        if (getConfig().getBoolean("PreventUsage") && entity instanceof Player
                && ((Player) entity).getGameMode() != GameMode.CREATIVE && isCreativeItem(((Player) entity).getItemInHand())) {
            return true;
        }
        return false;
    }

    private FileConfiguration getConfig() {
        return Options.get().getConfig().getConfig();
    }

    private String getCreativeString(ItemStack item) {
        String who = NBTItemData.get("LectronCreative", "CreativeItem", item);
        return StringUtils.isNullOrEmpty(who) ? null : who;
    }

    private boolean isCreativeItem(ItemStack item) {
        return NBTItemData.getList("LectronCreative", item).contains("CreativeItem");
    }

    private String getSurvivalString(ItemStack item) {
        String who = NBTItemData.get("LectronCreative", "SurvivalItem", item);
        return StringUtils.isNullOrEmpty(who) ? null : who;
    }

    private boolean isSurvivalItem(ItemStack item) {
        return NBTItemData.getList("LectronCreative", item).contains("SurvivalItem");
    }

    @EventHandler
    public void commandExecute(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE && getConfig().getStringList("BlacklistedCommands").contains(event.getMessage())) {
            event.setCancelled(true);
            player.sendMessage(Messages.get().getMessage("CommandNotAllowedInCreative"));
        }
    }

    @EventHandler
    public void onAttack(EntityDamageByEntityEvent event) {
        if (disallowedWorlds.contains(event.getEntity().getWorld().getName())) {
            return;
        }
        if (checkEntity(event.getDamager())) {
            event.setCancelled(true);
            return;
        }
        if (event.getEntity() instanceof Player && event.getDamager() instanceof Player) {
            Player target = (Player) event.getDamager();
            if (target.getGameMode() == GameMode.CREATIVE && ((Player) event.getEntity()).getGameMode() != GameMode.CREATIVE) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.isCancelled() || disallowedWorlds.contains(event.getBlock().getWorld().getName())) {
            return;
        }
        if (getConfig().getBoolean("MarkBlocks")
                && (isCreativeItem(event.getItemInHand()) || event.getPlayer().getGameMode() == GameMode.CREATIVE)) {
            markedBlocks.put(event.getBlockPlaced(), getCreativeString(event.getItemInHand()));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBreak(BlockBreakEvent event) {
        if (event.isCancelled() || disallowedWorlds.contains(event.getBlock().getWorld().getName())) {
            return;
        }

        if (isCreativeItem(event.getPlayer().getItemInHand()) && event.getPlayer().getGameMode() == GameMode.SURVIVAL) {
            event.setCancelled(true);
            event.setExpToDrop(0);
        }

        if (markedBlocks.containsKey(event.getBlock())) {
            final String message = markedBlocks.get(event.getBlock());
            markedBlocks.remove(event.getBlock());
            if (event.getPlayer().getGameMode() != GameMode.CREATIVE) {
                event.setExpToDrop(0);
                Collection<ItemStack> drops = event.getBlock().getDrops(event.getPlayer().getItemInHand());
                drops.iterator().forEachRemaining(item -> {
                    item = setCreativeItem(message, item);
                    event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation().clone().add(0.5, 0, 0.5), item);
                });
                event.getBlock().setType(Material.AIR);
            }
        }
    }

    @EventHandler
    public void onBrew(BrewEvent event) {
        if (disallowedWorlds.contains(event.getBlock().getWorld().getName())) {
            return;
        }
        boolean survivalItem = isCreativeItem(event.getContents().getIngredient()), creativeItem = isCreativeItem(event.getContents().getIngredient());
        if (creativeItem || survivalItem) {
            ItemStack[] items = event.getContents().getContents();
            for (int i = 0; i < items.length; i++) {
                if (items[i] != null && items[i].getItemMeta() != null) {
                    if (creativeItem) {
                        items[i] = NBTItemData.set("LectronCreative", "CreativeItem", NBTItemData.get("LectronCreative", "CreativeItem", event.getContents().getIngredient()), items[i]);
                    } else {
                        items[i] = NBTItemData.set("LectronCreative", "SurvivalItem", NBTItemData.get("LectronCreative", "SurvivalItem", event.getContents().getIngredient()), items[i]);
                    }
                }
            }
            event.getContents().setContents(items);
        }
    }

    @EventHandler
    public void onCraft(PrepareItemCraftEvent event) {
        if (event.getViewers().isEmpty() || disallowedWorlds.contains(event.getViewers().get(0).getWorld().getName())) {
            return;
        }
        for (ItemStack item : event.getInventory().getMatrix()) {
            if (isCreativeItem(item)) {
                if (event.getViewers().get(0).getGameMode() != GameMode.CREATIVE && getConfig().getBoolean("PreventCrafting")) {
                    event.getInventory().setItem(0, null);
                } else if (getConfig().getBoolean("RenameCrafting")) {
                    setCreativeItem(event.getViewers().get(0).getName(), event.getInventory().getItem(0));
                }
                break;
            }
        }
    }

    @EventHandler
    public void onCreativeClick(InventoryCreativeEvent event) {
        if (disallowedWorlds.contains(event.getWhoClicked().getWorld().getName())) {
            return;
        }
        if (!isSurvivalItem(event.getCursor())) {
            event.setCursor(setCreativeItem(event.getWhoClicked().getName(), event.getCursor()));
        }
        if (disallowedItems.contains(event.getCursor().getType())) {
            if (!event.getWhoClicked().hasPermission("limitcreative.useblacklistitems")) {
                event.setCancelled(true);
                if (event.getWhoClicked() instanceof Player) {
                    event.getWhoClicked().sendMessage(Messages.get().getMessage("ItemUsageNotAllowed"));
                }
            }
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (disallowedWorlds.contains(event.getEntity().getWorld().getName())) {
            return;
        }
        if (getConfig().getBoolean("PreventArmor")) {
            if (event.getEntity() instanceof Player && ((Player) event.getEntity()).getGameMode() != GameMode.CREATIVE) {
                ItemStack[] items = ((Player) event.getEntity()).getInventory().getArmorContents();
                for (int i = 0; i < 4; i++) {
                    ItemStack item = items[i];
                    if (isCreativeItem(item)) {
                        items[i] = new ItemStack(Material.AIR);
                        HashMap<Integer, ItemStack> leftovers = ((Player) event.getEntity()).getInventory().addItem(item);
                        for (ItemStack leftoverItem : leftovers.values()) {
                            event.getEntity().getWorld().dropItem(((Player) event.getEntity()).getEyeLocation(),
                                    leftoverItem);
                        }
                    }
                }
                ((HumanEntity) event.getEntity()).getInventory().setArmorContents(items);
            }
        }
    }

    @EventHandler
    public void onEnchant(PrepareItemEnchantEvent event) {
        if (isCreativeItem(event.getItem()) && event.getEnchanter().getGameMode() != GameMode.CREATIVE) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onExplode(EntityExplodeEvent event) {
        if (disallowedWorlds.contains(event.getLocation().getWorld().getName())) {
            return;
        }
        for (Block block : event.blockList()) {
            if (markedBlocks.containsKey(block)) {
                String message = markedBlocks.remove(block);
                block.getDrops().iterator().forEachRemaining(item -> {
                    item = setCreativeItem(message, item);
                    block.getWorld().dropItemNaturally(block.getLocation().clone().add(0.5, 0, 0.5), item);
                });
                block.setType(Material.AIR);
            }
        }
    }

    @EventHandler
    public void onGameModeSwitch(PlayerGameModeChangeEvent event) {
        if (disallowedWorlds.contains(event.getPlayer().getWorld().getName())) {
            return;
        }
        if (getConfig().getBoolean("PreventArmor") && event.getPlayer().getGameMode() == GameMode.CREATIVE
                && event.getNewGameMode() != GameMode.CREATIVE) {
            ItemStack[] items = event.getPlayer().getInventory().getArmorContents();
            for (int i = 0; i < 4; i++) {
                ItemStack item = items[i];
                if (isCreativeItem(item)) {
                    items[i] = null;
                    HashMap<Integer, ItemStack> leftovers = event.getPlayer().getInventory().addItem(item);
                    for (ItemStack leftoverItem : leftovers.values()) {
                        event.getPlayer().getWorld().dropItem(event.getPlayer().getEyeLocation(), leftoverItem);
                    }
                }
            }
            event.getPlayer().getInventory().setArmorContents(items);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (disallowedWorlds.contains(event.getPlayer().getWorld().getName())) {
            return;
        }
        if (checkEntity(event.getPlayer())) {
            event.setCancelled(true);
            return;
        }
        if (event.getPlayer().getItemInHand().getType().name().contains("EGG")) {
            event.setCancelled(true);
        }
    }

    @SuppressWarnings("deprecation")
    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (disallowedWorlds.contains(event.getPlayer().getWorld().getName())) {
            return;
        }
        if (checkEntity(event.getPlayer())) {
            event.setCancelled(true);
        }
        if (event.getPlayer().getGameMode() == GameMode.CREATIVE && event.getRightClicked() instanceof ItemFrame) {
            ItemStack item = event.getPlayer().getItemInHand();
            if (item.getType() != Material.AIR && !isCreativeItem(item)) {
                ItemFrame frame = (ItemFrame) event.getRightClicked();
                frame.getItem();
                if (frame.getItem().getType() == Material.AIR) {
                    event.getPlayer().setItemInHand(setCreativeItem(event.getPlayer().getName(), item));
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClick(InventoryClickEvent event) {
        if (disallowedWorlds.contains(event.getWhoClicked().getWorld().getName())) {
            return;
        }
        if (event.getWhoClicked().getGameMode() != GameMode.CREATIVE && event.getInventory().getType() == InventoryType.ANVIL
                && isCreativeItem(event.getCurrentItem())) {
            if (getConfig().getBoolean("PreventAnvil")) {
                event.setCancelled(true);
            }
        }
        if (event.getWhoClicked().getGameMode() == GameMode.CREATIVE && event.getAction() == InventoryAction.CLONE_STACK
                && !isCreativeItem(event.getCurrentItem()) && !isSurvivalItem(event.getCurrentItem())) {
            ItemStack item = event.getCurrentItem();
            if (item != null && item.getType() != Material.AIR) {
                item = setCreativeItem(event.getWhoClicked().getName(), event.getCurrentItem().clone());
                item.setAmount(item.getMaxStackSize());
                event.getWhoClicked().setItemOnCursor(item);
                event.setCancelled(true);
            }
        }

        if (!event.getWhoClicked().hasPermission("limitcreative.itemtransfer")) {
            Inventory top = event.getView().getTopInventory();
            Inventory bottom = event.getView().getBottomInventory();

            if (!Objects.equal(top, bottom)) {
                if (event.getCurrentItem() != null && event.getCurrentItem().getType() != Material.AIR) {
                    if (isCreativeItem(event.getCurrentItem())) {
                        event.setCancelled(true);
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryDragEvent(InventoryDragEvent event) {
        if (disallowedWorlds.contains(event.getWhoClicked().getWorld().getName())) {
            return;
        }

        if (!event.getWhoClicked().hasPermission("limitcreative.itemtransfer")) {
            Inventory top = event.getView().getTopInventory();
            Inventory bottom = event.getView().getBottomInventory();

            if (!Objects.equal(top, bottom)) {
                if (event.getOldCursor() != null && event.getOldCursor().getType() != Material.AIR) {
                    if (isCreativeItem(event.getOldCursor())) {
                        event.setCancelled(true);
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerPickupItem(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        final Player player = (Player) event.getEntity();
        if (disallowedWorlds.contains(player.getWorld().getName())) {
            return;
        }
        if (!player.hasPermission("limitcreative.pickupcreativeitem")) {
            if (player.getGameMode() != GameMode.CREATIVE && isCreativeItem(event.getItem().getItemStack())) {
                event.setCancelled(true);
            }
        }
        if (!player.hasPermission("limitcreative.pickupsurvivalitem")) {
            if (player.getGameMode() == GameMode.CREATIVE && !isCreativeItem(event.getItem().getItemStack())) {
                event.setCancelled(true);
            }
        }
        if (!event.isCancelled()) {
            if (player.getGameMode() == GameMode.CREATIVE) {
                event.getItem().setItemStack(setCreativeItem(player.getName(), event.getItem().getItemStack()));
            } else if (player.getGameMode() == GameMode.SURVIVAL) {
                event.getItem().setItemStack(setSurvivalItem(player.getName(), event.getItem().getItemStack()));
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPush(BlockPistonExtendEvent event) {
        if (disallowedWorlds.contains(event.getBlock().getWorld().getName())) {
            return;
        }
        for (Block block : event.getBlocks()) {
            if (markedBlocks.containsKey(block)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRetract(BlockPistonRetractEvent event) {
        if (disallowedWorlds.contains(event.getBlock().getWorld().getName())) {
            return;
        }
        if (event.isSticky()) {
            Block block = event.getBlock().getRelative(event.getDirection()).getRelative(event.getDirection());
            if (markedBlocks.containsKey(block)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void itemDrop(EntityDropItemEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getEntity();
        if (isCreativeItem(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onSmelt(FurnaceSmeltEvent event) {
        if (this.isCreativeItem(event.getSource())
                || isCreativeItem(((Furnace) event.getBlock().getState()).getInventory().getFuel())) {
            event.setCancelled(true);
        }
    }

    private ItemStack setCreativeItem(String who, ItemStack item) {
        if (item != null && item.getType() != Material.AIR && item.getType() != Material.LEGACY_BOOK_AND_QUILL) {
            return NBTItemData.set("LectronCreative", "CreativeItem", who, item);
        }
        return item;
    }

    private ItemStack setSurvivalItem(String who, ItemStack item) {
        if (item != null && item.getType() != Material.AIR && item.getType() != Material.LEGACY_BOOK_AND_QUILL) {
            return NBTItemData.set("LectronCreative", "SurvivalItem", who, item);
        }
        return item;
    }
}
