package me.sky.creativesurvival.base;

import com.mysql.jdbc.StringUtils;
import me.sky.creativesurvival.utils.Config;
import me.sky.creativesurvival.utils.Location;
import me.sky.creativesurvival.utils.Messages;
import me.sky.creativesurvival.utils.Options;
import me.sky.creativesurvival.utils.nbt.NBTItemData;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Furnace;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class InteractionListener implements Listener {

    private final ArrayList<Material> disallowedItems = new ArrayList<>();
    private final List<String> disallowedWorlds;
    private final JavaPlugin plugin;
    private final Map<Location, String> markedBlocks = new HashMap<>();

    public InteractionListener(JavaPlugin plugin) {
        this.plugin = plugin;
        blocksConfig = new Config("plugins/" + plugin.getName(), "blocks.yml", plugin);
        blocksConfig.create();
        if (!blocksConfig.getConfig().contains("Blocks")) {
            blocksConfig.getConfig().set("Blocks", new ArrayList<>());
        } else {
            blocksConfig.getConfig().getStringList("Blocks").forEach(s -> {
                String[] str = s.split(" ");
                markedBlocks.put(new Location(str[0], Integer.parseInt(str[1]), Integer.parseInt(str[2]), Integer.parseInt(str[3])), str[4]);
            });
        }

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
                System.out.print("[CreativeSurvival] Cannot parse " + disallowed + " to a valid material");
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

    private boolean isCreativeItem(ItemStack item) {
        return NBTItemData.getList("CreativeSurvival", item).contains("CreativeItem");
    }

    private String getSurvivalString(ItemStack item) {
        String who = NBTItemData.get("CreativeSurvival", "SurvivalItem", item);
        return StringUtils.isNullOrEmpty(who) ? null : who;
    }

    private boolean isSurvivalItem(ItemStack item) {
        return NBTItemData.getList("CreativeSurvival", item).contains("SurvivalItem");
    }

    @EventHandler
    public void commandExecute(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE) {
            for (String s : getConfig().getStringList("BlacklistedCommands")) {
                if (event.getMessage().replace("/", "").startsWith(s)) {
                    event.setCancelled(true);
                    player.sendMessage(Messages.get().getMessage("CommandNotAllowedInCreative"));
                    return;
                }
            }
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
            markBlock(event.getBlockPlaced(), event.getPlayer().getName());
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

        String message = isMarked(event.getBlock()) ? unmarkBlock(event.getBlock()) : null;
        if (event.getPlayer().getGameMode() == GameMode.CREATIVE) {
            return;
        }

        event.setExpToDrop(0);
        Collection<ItemStack> drops = event.getBlock().getDrops(event.getPlayer().getItemInHand());
        drops.iterator().forEachRemaining(item -> {
            if (message != null) {
                item = setCreativeItem(message, item);
            } else if (event.getPlayer().getGameMode() == GameMode.CREATIVE) {
                item = setCreativeItem(event.getPlayer().getName(), item);
            } else {
                item = setSurvivalItem(event.getPlayer().getName(), item);
            }
            event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation().clone(), item);
        });
        event.getBlock().setType(Material.AIR);
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
                        items[i] = NBTItemData.set("CreativeSurvival", "CreativeItem", NBTItemData.get("CreativeSurvival", "CreativeItem", event.getContents().getIngredient()), items[i]);
                    } else {
                        items[i] = NBTItemData.set("CreativeSurvival", "SurvivalItem", NBTItemData.get("CreativeSurvival", "SurvivalItem", event.getContents().getIngredient()), items[i]);
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
        if (disallowedItems.contains(event.getCursor().getType())) {
            if (!event.getWhoClicked().hasPermission("creativesurvival.useblacklistitems")) {
                event.setCancelled(true);
                if (event.getWhoClicked() instanceof Player) {
                    event.getWhoClicked().sendMessage(Messages.get().getMessage("ItemUsageNotAllowed"));
                }
            }
        } else {
//        } else if (event.getClickedInventory() == null || !event.getClickedInventory().equals(event.getWhoClicked().getInventory())) {
            event.setCursor(setCreativeItem(event.getWhoClicked().getName(), event.getCursor()));
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
            if (isMarked(block)) {
                String message = unmarkBlock(block);
                block.getDrops().iterator().forEachRemaining(item -> {
                    item = setCreativeItem(message, item);
                    block.getWorld().dropItemNaturally(block.getLocation().clone(), item);
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
                && !isCreativeItem(event.getCurrentItem())) {
            ItemStack item = event.getCurrentItem();
            if (item != null && item.getType() != Material.AIR) {
                if (!isSurvivalItem(item)) {
                    item = setCreativeItem(event.getWhoClicked().getName(), event.getCurrentItem().clone());
                }
                item.setAmount(item.getMaxStackSize());
                event.getWhoClicked().setItemOnCursor(item);
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryDragEvent(InventoryDragEvent event) {
        if (disallowedWorlds.contains(event.getWhoClicked().getWorld().getName())) {
            return;
        }

        if (getConfig().getBoolean("PreventTransfer")) {
            Inventory top = event.getView().getTopInventory();
            Inventory bottom = event.getView().getBottomInventory();

            if (top != null && bottom != null && !top.equals(bottom)) {
                if (event.getOldCursor() != null && event.getOldCursor().getType() != Material.AIR) {
                    if (isCreativeItem(event.getOldCursor())) {
                        event.setCancelled(true);
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryDragEvent(InventoryClickEvent event) {
        if (disallowedWorlds.contains(event.getWhoClicked().getWorld().getName())) {
            return;
        }

        if (getConfig().getBoolean("PreventTransfer") && event.getWhoClicked().getGameMode() == GameMode.CREATIVE) {
            Inventory top = event.getView().getTopInventory();
            Inventory bottom = event.getView().getBottomInventory();

            if (top != null && bottom != null && !top.equals(bottom) && !top.getType().equals(InventoryType.CREATIVE) && !top.getType().equals(InventoryType.CRAFTING)) {
                ItemStack item = event.getCurrentItem();
                if (item != null && item.getType() != Material.AIR) {
                    event.setCancelled(true);
                    event.setResult(Event.Result.DENY);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerPickupItem(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        final Player player = (Player) event.getEntity();
        if (disallowedWorlds.contains(player.getWorld().getName())) {
            return;
        }
        if (!player.hasPermission("creativesurvival.pickupcreativeitem")) {
            if (player.getGameMode() != GameMode.CREATIVE && isCreativeItem(event.getItem().getItemStack())) {
                event.setCancelled(true);
                return;
            }
        }
        if (!player.hasPermission("creativesurvival.pickupsurvivalitem")) {
            if (player.getGameMode() == GameMode.CREATIVE && !isCreativeItem(event.getItem().getItemStack())) {
                event.setCancelled(true);
                return;
            }
        }
        if (!event.isCancelled() && !isSurvivalItem(event.getItem().getItemStack()) && !isCreativeItem(event.getItem().getItemStack())) {
            if (player.getGameMode() == GameMode.CREATIVE ||
                    player.getGameMode() == GameMode.SURVIVAL) {
                addPickupItem(player, event.getItem());
                event.getItem().remove();
                event.setCancelled(true);
            }
        }
    }

    private void addPickupItem(Player player, Item item) {
        ItemStack itemStack = item.getItemStack().clone();
        if (player.getGameMode() == GameMode.CREATIVE) {
            itemStack = setCreativeItem(player.getName(), itemStack);
        } else if (player.getGameMode() == GameMode.SURVIVAL) {
            itemStack = setSurvivalItem(player.getName(), itemStack);
        }
        player.getInventory().addItem(itemStack);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPush(BlockPistonExtendEvent event) {
        if (disallowedWorlds.contains(event.getBlock().getWorld().getName())) {
            return;
        }
        for (Block block : event.getBlocks()) {
            if (isMarked(block)) {
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
            if (isMarked(block)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void itemDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE && isCreativeItem(event.getItemDrop().getItemStack())) {
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
        if (item != null && item.getType() != Material.AIR) {
            return NBTItemData.set("CreativeSurvival", "CreativeItem", who, item);
        }
        return item;
    }

    private ItemStack setSurvivalItem(String who, ItemStack item) {
        if (item != null && item.getType() != Material.AIR) {
            return NBTItemData.set("CreativeSurvival", "SurvivalItem", who, item);
        }
        return item;
    }

    private final Config blocksConfig;

    public boolean isMarked(Block block) {
        for (Location loc : markedBlocks.keySet()) {
            if (loc.equals(new Location(block))) {
                return true;
            }
        }
        return false;
    }

    public void markBlock(Block block, String msg) {
        markBlock(new Location(block), msg);
    }

    public void markBlock(final Location loc, final String msg) {
        markedBlocks.put(loc, msg);
        new BukkitRunnable() {
            @Override
            public void run() {
                List<String> b = blocksConfig.getConfig().getStringList("Blocks");
                b.add(String.format("%s %s %s %s %s", loc.getWorld(), loc.getX(), loc.getY(), loc.getZ(), msg));
                blocksConfig.getConfig().set("Blocks", b);
                blocksConfig.saveConfig();
                blocksConfig.reloadConfig();
            }
        }.runTaskAsynchronously(plugin);
    }

    public String unmarkBlock(Block block) {
        return unmarkBlock(new Location(block));
    }

    public String unmarkBlock(final Location loc) {
        String msg = markedBlocks.remove(loc);
        new BukkitRunnable() {
            @Override
            public void run() {
                List<String> b = blocksConfig.getConfig().getStringList("Blocks");
                if (b.size() > 0) {
                    List<String> remove = new ArrayList<>();
                    b.iterator().forEachRemaining(s -> {
                        String[] str = s.split(" ");
                        Location l = new Location(str[0], Integer.parseInt(str[1]), Integer.parseInt(str[2]), Integer.parseInt(str[3]));
                        if (l.equals(loc)) {
                            remove.add(s);
                        }
                    });
                    b.removeAll(remove);
                }
                blocksConfig.getConfig().set("Blocks", b);
                blocksConfig.saveConfig();
                blocksConfig.reloadConfig();
            }
        }.runTaskAsynchronously(plugin);
        return msg;
    }


}
