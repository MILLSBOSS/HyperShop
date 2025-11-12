package me.MILLSBOSS.ecoShop;

import me.MILLSBOSS.ecoShop.model.Category;
import me.MILLSBOSS.ecoShop.model.Listing;
import me.MILLSBOSS.ecoShop.storage.ListingsManager;
import me.MILLSBOSS.ecoShop.gui.Constants;
import me.MILLSBOSS.ecoShop.pricing.Pricing;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class ShopListener implements Listener {

    private final EcoShopPro plugin;

    // pending sell: player -> item to list
    private final Map<UUID, ItemStack> pendingSell = new HashMap<>();

    public ShopListener(EcoShopPro plugin) {
        this.plugin = plugin;
    }

    private boolean isMainTitle(String title) {
        String t = ChatColor.stripColor(title);
        return t.equalsIgnoreCase("EcoShopPro");
    }

    private boolean isCategoryTitle(String title) {
        String t = ChatColor.stripColor(title);
        return t.contains(" - Page ");
    }

    private boolean isMyListingsTitle(String title) {
        String t = ChatColor.stripColor(title);
        return t.startsWith("My Listings - Page ");
    }

    private int parsePage(String title) {
        String t = ChatColor.stripColor(title);
        int idx = t.lastIndexOf("Page ");
        if (idx == -1) return 0;
        try { return Integer.parseInt(t.substring(idx + 5).trim()) - 1; } catch (Exception e) { return 0; }
    }

    private Category parseCategory(String title) {
        String t = ChatColor.stripColor(title);
        if (!t.contains(" - Page ")) return null;
        String name = t.substring(0, t.indexOf(" - Page "));
        for (Category c : Category.values()) {
            if (c.getDisplay().equalsIgnoreCase(name)) return c;
        }
        return null;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        Inventory inv = e.getView().getTopInventory();
        if (inv == null) return;
        String title = e.getView().getTitle();

        if (isMainTitle(title)) {
            int slot = e.getRawSlot();
            // Prevent shift-clicking items from player inventory into the GUI
            if (e.getClickedInventory() != null && !e.getClickedInventory().equals(inv)) {
                if (e.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                    e.setCancelled(true);
                    return;
                }
            }
            // Block number-key hotbar swaps in the main GUI entirely
            if (e.getAction() == InventoryAction.HOTBAR_SWAP || e.getClick() == org.bukkit.event.inventory.ClickType.NUMBER_KEY) {
                e.setCancelled(true);
                return;
            }
            if (slot < inv.getSize()) {
                ItemStack clicked = e.getCurrentItem();
                // Clicking categories
                if (clicked != null && ShopMenus.isCategoryIcon(clicked)) {
                    e.setCancelled(true);
                    Category c = ShopMenus.getCategoryFromIcon(clicked);
                    if (c != null) ShopMenus.openCategory(p, c, 0);
                    return;
                }
                // My listings button
                if (slot == 50) {
                    e.setCancelled(true);
                    ShopMenus.openMyListings(p, 0);
                    return;
                }
                // Player head - just cancel
                if (slot == 53) {
                    e.setCancelled(true);
                    return;
                }
                // Sell slot handling
                if (ShopMenus.isSellSlot(slot)) {
                    ItemStack cursor = e.getCursor();
                    // Block picking up the placeholder pane, but allow swapping with cursor so players can place items into the sell slot
                    if (clicked != null && clicked.getType() == Material.LIGHT_GRAY_STAINED_GLASS_PANE && (e.getAction() == InventoryAction.PICKUP_ALL || e.getAction() == InventoryAction.PICKUP_HALF || e.getAction() == InventoryAction.PICKUP_ONE || e.getAction() == InventoryAction.PICKUP_SOME)) {
                        e.setCancelled(true);
                        return;
                    }
                    // Only allow placing items from cursor into the sell slot
                    if (cursor != null && cursor.getType() != Material.AIR && (e.getAction() == InventoryAction.PLACE_ALL || e.getAction() == InventoryAction.PLACE_ONE || e.getAction() == InventoryAction.PLACE_SOME || e.getAction() == InventoryAction.SWAP_WITH_CURSOR)) {
                        // allow the place, then process next tick
                        e.setCancelled(false);
                        Bukkit.getScheduler().runTask(plugin, () -> handleSellSlotItem(p));
                        return;
                    }
                    // In all other cases, cancel interaction with sell slot
                    e.setCancelled(true);
                    return;
                }
                e.setCancelled(true);
            }
        } else if (isMyListingsTitle(title)) {
            int slot = e.getRawSlot();
            if (slot < inv.getSize()) {
                ItemStack clicked = e.getCurrentItem();
                if (clicked == null) { e.setCancelled(true); return; }
                String name = clicked.hasItemMeta() && clicked.getItemMeta().hasDisplayName() ? ChatColor.stripColor(clicked.getItemMeta().getDisplayName()) : "";
                if (clicked.getType() == Material.BARRIER) {
                    e.setCancelled(true);
                    ShopMenus.openMain(p);
                    return;
                }
                if (name.equalsIgnoreCase("Previous Page")) {
                    e.setCancelled(true);
                    int page = parsePage(title);
                    if (page > 0) ShopMenus.openMyListings(p, page - 1);
                    return;
                }
                if (name.equalsIgnoreCase("Next Page")) {
                    e.setCancelled(true);
                    int page = parsePage(title);
                    ShopMenus.openMyListings(p, page + 1);
                    return;
                }
                UUID id = ShopMenus.getListingId(clicked);
                if (id != null) {
                    e.setCancelled(true);
                    ListingsManager lm = plugin.getListingsManager();
                    Listing l = lm.getById(id);
                    if (l == null || !l.getSeller().equals(p.getUniqueId())) { p.sendMessage(ChatColor.RED + "Listing not found."); return; }
                    // remove and return item
                    lm.removeListing(id);
                    ItemStack toGive = l.getItem().clone();
                    HashMap<Integer, ItemStack> left = p.getInventory().addItem(toGive);
                    for (ItemStack rem : left.values()) p.getWorld().dropItemNaturally(p.getLocation(), rem);
                    p.sendMessage(ChatColor.YELLOW + "Listing removed and item returned.");
                    ShopMenus.openMyListings(p, parsePage(title));
                    return;
                }
                e.setCancelled(true);
            }
        } else if (isCategoryTitle(title)) {
            int slot = e.getRawSlot();
            if (slot < inv.getSize()) {
                ItemStack clicked = e.getCurrentItem();
                if (clicked == null) { e.setCancelled(true); return; }
                String name = clicked.hasItemMeta() && clicked.getItemMeta().hasDisplayName() ? ChatColor.stripColor(clicked.getItemMeta().getDisplayName()) : "";
                // Back
                if (clicked.getType() == Material.BARRIER) {
                    e.setCancelled(true);
                    ShopMenus.openMain(p);
                    return;
                }
                // Navigation
                if (name.equalsIgnoreCase("Previous Page")) {
                    e.setCancelled(true);
                    int page = parsePage(title);
                    Category cat = parseCategory(title);
                    if (cat != null && page > 0) ShopMenus.openCategory(p, cat, page - 1);
                    return;
                }
                if (name.equalsIgnoreCase("Next Page")) {
                    e.setCancelled(true);
                    int page = parsePage(title);
                    Category cat = parseCategory(title);
                    if (cat != null) ShopMenus.openCategory(p, cat, page + 1);
                    return;
                }
                // Buy listing -> open confirm menu
                UUID id = ShopMenus.getListingId(clicked);
                if (id != null) {
                    e.setCancelled(true);
                    ListingsManager lm = plugin.getListingsManager();
                    Listing l = lm.getById(id);
                    if (l == null) { p.sendMessage(ChatColor.RED + "That listing is no longer available."); ShopMenus.openCategory(p, parseCategory(title), parsePage(title)); return; }
                    Category cat = parseCategory(title);
                    int page = parsePage(title);
                    // Prevent buying own listing
                    if (l.getSeller() != null && l.getSeller().equals(p.getUniqueId())) {
                        p.sendMessage(ChatColor.RED + "You cannot buy your own listing.");
                        ShopMenus.openCategory(p, cat, page);
                        return;
                    }
                    ShopMenus.openConfirm(p, l, cat, page, 1);
                    return;
                }
                e.setCancelled(true);
            }
        } else if (ChatColor.stripColor(title).equalsIgnoreCase("Confirm Purchase")) {
            int slot = e.getRawSlot();
            if (slot < inv.getSize()) {
                ItemStack clicked = e.getCurrentItem();
                if (clicked == null) { e.setCancelled(true); return; }
                if (!clicked.hasItemMeta()) { e.setCancelled(true); return; }
                String name = clicked.getItemMeta().hasDisplayName() ? ChatColor.stripColor(clicked.getItemMeta().getDisplayName()) : "";
                String type = clicked.getItemMeta().getPersistentDataContainer().get(Constants.KEY_TYPE, PersistentDataType.STRING);
                if (!Constants.GUI_CONFIRM.equals(type)) { e.setCancelled(true); return; }
                UUID listingId = ShopMenus.getListingId(clicked);
                if (listingId == null) { e.setCancelled(true); return; }
                ListingsManager lm = plugin.getListingsManager();
                Listing l = lm.getById(listingId);
                if (l == null) { e.setCancelled(true); p.sendMessage(ChatColor.RED + "Listing no longer available."); ShopMenus.openMain(p); return; }
                int currentQty = Optional.ofNullable(clicked.getItemMeta().getPersistentDataContainer().get(Constants.KEY_QUANTITY, PersistentDataType.INTEGER)).orElse(1);
                Category cat = null;
                String catName = clicked.getItemMeta().getPersistentDataContainer().get(Constants.KEY_CATEGORY, PersistentDataType.STRING);
                if (catName != null && !catName.isEmpty()) {
                    try { cat = Category.valueOf(catName); } catch (Exception ignored) {}
                }
                int page = Optional.ofNullable(clicked.getItemMeta().getPersistentDataContainer().get(Constants.KEY_PAGE, PersistentDataType.INTEGER)).orElse(0);
                int maxQty = Math.max(1, l.getItem().getAmount());

                e.setCancelled(true);

                // Quantity adjustment
                if (name.equals("-32")) {
                    int q = Math.max(1, currentQty - 32);
                    ShopMenus.openConfirm(p, l, cat, page, q);
                    return;
                } else if (name.equals("-1")) {
                    int q = Math.max(1, currentQty - 1);
                    ShopMenus.openConfirm(p, l, cat, page, q);
                    return;
                } else if (name.equals("+1")) {
                    int q = Math.min(maxQty, currentQty + 1);
                    ShopMenus.openConfirm(p, l, cat, page, q);
                    return;
                } else if (name.equals("+32")) {
                    int q = Math.min(maxQty, currentQty + 32);
                    ShopMenus.openConfirm(p, l, cat, page, q);
                    return;
                } else if (name.equalsIgnoreCase("Buy All")) {
                    ShopMenus.openConfirm(p, l, cat, page, maxQty);
                    return;
                } else if (name.equalsIgnoreCase("Cancel")) {
                    if (cat != null) ShopMenus.openCategory(p, cat, page); else ShopMenus.openMain(p);
                    return;
                } else if (name.equalsIgnoreCase("Confirm")) {
                    // Prevent buying own listing (double-check at confirm time)
                    if (l.getSeller() != null && l.getSeller().equals(p.getUniqueId())) {
                        p.sendMessage(ChatColor.RED + "You cannot buy your own listing.");
                        if (cat != null) ShopMenus.openCategory(p, cat, page); else ShopMenus.openMain(p);
                        return;
                    }
                    // Perform purchase for currentQty
                    if (plugin.getEconomy() == null) { p.sendMessage(ChatColor.RED + "No economy found."); return; }
                    int qty = Math.min(currentQty, maxQty);
                    double unitPrice = l.getPrice() / Math.max(1, l.getItem().getAmount());
                    double total = unitPrice * qty;
                    if (!plugin.getEconomy().has(p, total)) { p.sendMessage(ChatColor.RED + "You don't have enough money."); return; }
                    // Prepare item to give with selected quantity
                    ItemStack toGive = l.getItem().clone();
                    toGive.setAmount(qty);
                    HashMap<Integer, ItemStack> left = p.getInventory().addItem(toGive);
                    for (ItemStack rem : left.values()) p.getWorld().dropItemNaturally(p.getLocation(), rem);
                    // Economy transfer
                    plugin.getEconomy().withdrawPlayer(p, total);
                    if (Bukkit.getPlayer(l.getSeller()) != null && plugin.getEconomy() != null) {
                        plugin.getEconomy().depositPlayer(Bukkit.getOfflinePlayer(l.getSeller()), total);
                    }
                    // Update or remove listing
                    ItemStack remaining = l.getItem().clone();
                    int newAmt = remaining.getAmount() - qty;
                    ListingsManager manager = plugin.getListingsManager();
                    if (newAmt <= 0) {
                        manager.removeListing(l.getId());
                    } else {
                        remaining.setAmount(newAmt);
                        l.setItem(remaining);
                        l.setPrice(unitPrice * newAmt); // Keep unit price consistent
                        manager.updateListing(l);
                    }
                    manager.addToSales(l.getSeller(), total);

                    // Notify seller if online
                    org.bukkit.entity.Player sellerPlayer = Bukkit.getPlayer(l.getSeller());
                    if (sellerPlayer != null && sellerPlayer.isOnline()) {
                        String itemName = prettyItemName(toGive);
                        sellerPlayer.sendMessage(ChatColor.YELLOW + p.getName() + ChatColor.GRAY + " bought " + ChatColor.AQUA + qty + "x " + itemName + ChatColor.GRAY + " from your listing for " + ChatColor.GREEN + String.format(Locale.US, "%.2f", total) + ChatColor.GRAY + ".");
                    }

                    p.sendMessage(ChatColor.GREEN + "Purchased " + qty + " for " + total);
                    if (cat != null) ShopMenus.openCategory(p, cat, page); else ShopMenus.openMain(p);
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        Inventory top = e.getView().getTopInventory();
        if (top == null) return;
        String title = e.getView().getTitle();
        if (!isMainTitle(title)) return;

        // Identify which raw slots of the TOP inventory would be affected by this drag
        // Cancel if any top-inventory slot other than the sell slot is targeted
        boolean affectsTop = false;
        boolean affectsOnlySell = true;
        for (int raw : e.getRawSlots()) {
            if (raw < top.getSize()) { // inside top inventory GUI
                affectsTop = true;
                if (raw != 49) {
                    affectsOnlySell = false;
                    break;
                }
            }
        }

        if (affectsTop && !affectsOnlySell) {
            // Disallow dragging into any other top slots
            e.setCancelled(true);
            return;
        }

        if (affectsOnlySell && e.getRawSlots().contains(49)) {
            // Allow the drag into the sell slot, then process on the next tick
            // Ensure event not cancelled
            e.setCancelled(false);
            Bukkit.getScheduler().runTask(plugin, () -> handleSellSlotItem(p));
        }
    }

    @EventHandler
    public void onInventoryClickSellPlace(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        Inventory inv = e.getView().getTopInventory();
        String title = e.getView().getTitle();
        if (inv == null || !isMainTitle(title)) return;
        if (e.getAction() == InventoryAction.PLACE_ALL || e.getAction() == InventoryAction.SWAP_WITH_CURSOR || e.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY || e.getAction() == InventoryAction.PLACE_SOME || e.getAction() == InventoryAction.PLACE_ONE) {
            if (e.getRawSlot() == 49) {
                Bukkit.getScheduler().runTask(plugin, () -> handleSellSlotItem(p));
            }
        }
    }

    private void handleSellSlotItem(Player p) {
        Inventory inv = p.getOpenInventory().getTopInventory();
        String title = p.getOpenInventory().getTitle();
        if (inv == null || !isMainTitle(title)) return;
        ItemStack inSell = inv.getItem(49);
        if (inSell == null || inSell.getType() == Material.AIR) return;
        // Ignore if it's our tagged placeholder
        if (inSell.hasItemMeta() && Constants.GUI_SELL_PLACEHOLDER.equals(inSell.getItemMeta().getPersistentDataContainer().get(Constants.KEY_TYPE, org.bukkit.persistence.PersistentDataType.STRING))) {
            return;
        }
        // Take the item into pending and clear sell slot
        ItemStack toSell = inSell.clone();
        pendingSell.put(p.getUniqueId(), toSell);
        inv.setItem(49, ShopMenus.sellSlotItem());
        // Clear player cursor just in case and close GUI to prompt in chat to avoid further GUI interactions
        p.setItemOnCursor(new ItemStack(Material.AIR));
        p.closeInventory();
        double suggestion = Pricing.suggestMaxPrice(toSell);
        if (suggestion > 0) {
            p.sendMessage(ChatColor.YELLOW + String.format("You can sell this for a max of %.2f", suggestion));
        }
        p.sendMessage(ChatColor.GREEN + "Enter a sale price in chat (max above), or type 'cancel' to abort.");
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        Player p = e.getPlayer();
        if (!pendingSell.containsKey(p.getUniqueId())) return;
        e.setCancelled(true);
        String msg = e.getMessage().trim();
        if (msg.equalsIgnoreCase("cancel")) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                // return item
                ItemStack item = pendingSell.remove(p.getUniqueId());
                if (item != null) {
                    HashMap<Integer, ItemStack> left = p.getInventory().addItem(item);
                    for (ItemStack rem : left.values()) p.getWorld().dropItemNaturally(p.getLocation(), rem);
                }
                p.sendMessage(ChatColor.YELLOW + "Sale cancelled.");
                if (p.getOpenInventory() != null && isMainTitle(p.getOpenInventory().getTitle())) {
                    ShopMenus.openMain(p);
                }
            });
            return;
        }
        try {
            double price = Double.parseDouble(msg);
            if (price <= 0) throw new NumberFormatException();
            ItemStack item = pendingSell.get(p.getUniqueId());
            if (item == null) return;
            double max = Pricing.suggestMaxPrice(item);
            if (price > max) {
                p.sendMessage(ChatColor.RED + String.format("You can sell this for a max of %.2f", max));
                p.sendMessage(ChatColor.YELLOW + "Please enter a price at or below the max, or type 'cancel'.");
                return;
            }
            // Accept and list
            ItemStack itemToList = pendingSell.remove(p.getUniqueId());
            final ItemStack finalItem = itemToList;
            final double finalPrice = Math.round(price * 100.0) / 100.0;
            final UUID sellerId = p.getUniqueId();
            Bukkit.getScheduler().runTask(plugin, () -> {
                Category cat = Category.categorize(finalItem.getType());
                plugin.getListingsManager().addListing(sellerId, finalItem, finalPrice, cat);
                p.sendMessage(ChatColor.GREEN + "Item listed for " + finalPrice);
                ShopMenus.openMain(p);
            });
        } catch (NumberFormatException ex) {
            p.sendMessage(ChatColor.RED + "Invalid number. Type a positive price or 'cancel'.");
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        // nothing special
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        UUID id = e.getPlayer().getUniqueId();
        ItemStack item = pendingSell.remove(id);
        if (item != null) {
            HashMap<Integer, ItemStack> left = e.getPlayer().getInventory().addItem(item);
            for (ItemStack rem : left.values()) e.getPlayer().getWorld().dropItemNaturally(e.getPlayer().getLocation(), rem);
        }
    }

    private static String prettyItemName(ItemStack item) {
        if (item == null) return "Item";
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            String dn = ChatColor.stripColor(item.getItemMeta().getDisplayName());
            if (dn != null && !dn.trim().isEmpty()) return dn;
        }
        String[] parts = item.getType().name().toLowerCase(Locale.ROOT).split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) continue;
            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1)).append(" ");
        }
        String name = sb.toString().trim();
        int amt = Math.max(1, item.getAmount());
        // Do not prefix quantity here; caller decides, but include for clarity if needed elsewhere
        return name;
    }
}
