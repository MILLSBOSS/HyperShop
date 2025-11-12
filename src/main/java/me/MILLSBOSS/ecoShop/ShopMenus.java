package me.MILLSBOSS.ecoShop;

import me.MILLSBOSS.ecoShop.gui.Constants;
import me.MILLSBOSS.ecoShop.model.Category;
import me.MILLSBOSS.ecoShop.model.Listing;
import me.MILLSBOSS.ecoShop.storage.ListingsManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public final class ShopMenus {

    private static final int SELL_SLOT = 49; // bottom row middle
    private static final int MY_LISTINGS_SLOT = 50;
    private static final int PLAYER_HEAD_SLOT = 53;

    // Confirm GUI layout constants
    private static final int CONFIRM_DISPLAY_SLOT = 13;
    private static final int BTN_MINUS_32 = 19;
    private static final int BTN_MINUS_1 = 21;
    private static final int BTN_PLUS_1 = 23;
    private static final int BTN_PLUS_32 = 25;
    private static final int BTN_BUY_ALL = 40;
    private static final int BTN_CONFIRM = 38;
    private static final int BTN_CANCEL = 42;

    private ShopMenus() {}

    public static void openMain(Player p) {
        Inventory inv = Bukkit.createInventory(p, 54, ChatColor.GREEN + "EcoShopPro");
        tagInventory(inv, Constants.GUI_MAIN, null, -1);
        // Fill with categories
        int[] catSlots = new int[]{10,11,12,13,14,15,16,19,20,21,22};
        Category[] cats = new Category[]{
                Category.BUILDING_BLOCKS,
                Category.TOOLS,
                Category.WEAPONS,
                Category.ARMOR,
                Category.FOOD,
                Category.ORES,
                Category.PLANTS,
                Category.REDSTONE,
                Category.SPAWNERS,
                Category.ENCHANTMENT_BOOKS,
                Category.MISC
        };
        for (int i = 0; i < cats.length && i < catSlots.length; i++) {
            Category c = cats[i];
            inv.setItem(catSlots[i], categoryIcon(c));
        }
        // Sell slot placeholder
        inv.setItem(SELL_SLOT, sellSlotItem());
        // My listings button
        inv.setItem(MY_LISTINGS_SLOT, myListingsItem(p));
        // Player head bottom-right
        inv.setItem(PLAYER_HEAD_SLOT, playerHead(p));
        p.openInventory(inv);
    }

    public static void openCategory(Player p, Category category, int page) {
        Inventory inv = Bukkit.createInventory(p, 54, ChatColor.YELLOW + category.getDisplay() + ChatColor.GRAY + " - Page " + (page+1));
        tagInventory(inv, Constants.GUI_CATEGORY, category, page);
        // Fetch listings and paginate
        ListingsManager lm = EcoShopPro.getInstance().getListingsManager();
        List<Listing> list = lm.getByCategory(category);
        int perPage = 45; // top 5 rows
        int start = page * perPage;
        int end = Math.min(start + perPage, list.size());
        for (int i = start; i < end; i++) {
            Listing l = list.get(i);
            inv.setItem(i - start, listingItem(l));
        }
        // Navigation arrows
        if (page > 0) inv.setItem(45 + 0, navItem(true));
        if (end < list.size()) inv.setItem(45 + 8, navItem(false));
        // Back to main
        inv.setItem(49, backItem());
        p.openInventory(inv);
    }

    public static void openMyListings(Player p, int page) {
        Inventory inv = Bukkit.createInventory(p, 54, ChatColor.AQUA + "My Listings - Page " + (page+1));
        tagInventory(inv, Constants.GUI_MY_LISTINGS, null, page);
        ListingsManager lm = EcoShopPro.getInstance().getListingsManager();
        List<Listing> mine = lm.getBySeller(p.getUniqueId());
        int perPage = 45;
        int start = page * perPage;
        int end = Math.min(start + perPage, mine.size());
        for (int i = start; i < end; i++) {
            Listing l = mine.get(i);
            inv.setItem(i - start, myListingItem(l));
        }
        if (page > 0) inv.setItem(45 + 0, navItem(true));
        if (end < mine.size()) inv.setItem(45 + 8, navItem(false));
        inv.setItem(49, backItem());
        p.openInventory(inv);
    }

    private static void tagInventory(Inventory inv, String type, Category cat, int page) {
        // We'll tag with a hidden item in slot 53 (player head), but better approach is to rely on title and items' PDC.
        // Instead, store type/category/page in the inventory holder via PDC on specific marker items we place.
        // For simplicity, we don't need to tag the whole inventory; we can detect by title and items.
    }

    public static boolean isSellSlot(int rawSlot) {
        return rawSlot == SELL_SLOT;
    }

    public static ItemStack categoryIcon(Category c) {
        ItemStack it = new ItemStack(c.getIcon());
        ItemMeta meta = it.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + c.getDisplay());
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.YELLOW + "Click to browse listings");
        meta.setLore(lore);
        meta.getPersistentDataContainer().set(Constants.KEY_TYPE, PersistentDataType.STRING, Constants.GUI_CATEGORY);
        meta.getPersistentDataContainer().set(Constants.KEY_CATEGORY, PersistentDataType.STRING, c.name());
        it.setItemMeta(meta);
        return it;
    }

    private static ItemStack listingItem(Listing l) {
        ItemStack it = l.getItem().clone();
        ItemMeta meta = it.getItemMeta();
        List<String> lore = meta.hasLore() ? new ArrayList<>(Objects.requireNonNull(meta.getLore())) : new ArrayList<>();
        lore.add(ChatColor.GRAY + "——");
        lore.add(ChatColor.YELLOW + String.format("Price: %.2f", l.getPrice()));
        OfflinePlayer seller = Bukkit.getOfflinePlayer(l.getSeller());
        lore.add(ChatColor.YELLOW + "Seller: " + (seller != null ? seller.getName() : l.getSeller().toString()));
        lore.add(ChatColor.GREEN + "Click to buy");
        meta.setLore(lore);
        meta.getPersistentDataContainer().set(Constants.KEY_LISTING_ID, PersistentDataType.STRING, l.getId().toString());
        it.setItemMeta(meta);
        return it;
    }

    private static ItemStack myListingItem(Listing l) {
        ItemStack it = l.getItem().clone();
        ItemMeta meta = it.getItemMeta();
        List<String> lore = meta.hasLore() ? new ArrayList<>(Objects.requireNonNull(meta.getLore())) : new ArrayList<>();
        lore.add(ChatColor.GRAY + "——");
        lore.add(ChatColor.YELLOW + String.format("Price: %.2f", l.getPrice()));
        lore.add(ChatColor.RED + "Click to cancel and reclaim");
        meta.setLore(lore);
        meta.getPersistentDataContainer().set(Constants.KEY_LISTING_ID, PersistentDataType.STRING, l.getId().toString());
        it.setItemMeta(meta);
        return it;
    }

    private static ItemStack navItem(boolean previous) {
        ItemStack it = new ItemStack(previous ? Material.ARROW : Material.SPECTRAL_ARROW);
        ItemMeta meta = it.getItemMeta();
        meta.setDisplayName(previous ? ChatColor.YELLOW + "Previous Page" : ChatColor.YELLOW + "Next Page");
        it.setItemMeta(meta);
        return it;
    }

    private static ItemStack backItem() {
        ItemStack it = new ItemStack(Material.BARRIER);
        ItemMeta meta = it.getItemMeta();
        meta.setDisplayName(ChatColor.RED + "Back to Main");
        it.setItemMeta(meta);
        return it;
    }

    private static ItemStack myListingsItem(Player p) {
        ItemStack it = new ItemStack(Material.BOOKSHELF);
        ItemMeta meta = it.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + "Your Listings");
        it.setItemMeta(meta);
        return it;
    }

    public static ItemStack sellSlotItem() {
        ItemStack it = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = it.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + "Drop item here to sell");
        meta.setLore(Arrays.asList(ChatColor.GRAY + "Then type a price in chat",
                ChatColor.GRAY + "Type 'cancel' to abort"));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        // Tag as sell placeholder
        meta.getPersistentDataContainer().set(Constants.KEY_TYPE, PersistentDataType.STRING, Constants.GUI_SELL_PLACEHOLDER);
        it.setItemMeta(meta);
        return it;
    }

    private static ItemStack playerHead(Player p) {
        ItemStack it = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) it.getItemMeta();
        meta.setOwningPlayer(p);
        meta.setDisplayName(ChatColor.GOLD + p.getName());
        double bal = 0.0;
        double sales = 0.0;
        Economy eco = EcoShopPro.getInstance().getEconomy();
        if (eco != null) bal = eco.getBalance(p);
        sales = EcoShopPro.getInstance().getListingsManager().getSalesTotal(p.getUniqueId());
        meta.setLore(Arrays.asList(
                ChatColor.YELLOW + String.format("Balance: %.2f", bal),
                ChatColor.YELLOW + String.format("Total Sales: %.2f", sales)
        ));
        it.setItemMeta(meta);
        return it;
    }

    public static boolean isCategoryIcon(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        return Constants.GUI_CATEGORY.equals(pdc.get(Constants.KEY_TYPE, PersistentDataType.STRING));
    }

    public static Category getCategoryFromIcon(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        String name = item.getItemMeta().getPersistentDataContainer().get(Constants.KEY_CATEGORY, PersistentDataType.STRING);
        if (name == null) return null;
        try { return Category.valueOf(name); } catch (Exception e) { return null; }
    }

    public static UUID getListingId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        String id = item.getItemMeta().getPersistentDataContainer().get(Constants.KEY_LISTING_ID, PersistentDataType.STRING);
        if (id == null) return null;
        try { return UUID.fromString(id); } catch (Exception e) { return null; }
    }

    // ==== Confirm GUI ====
    public static void openConfirm(Player p, Listing l, Category category, int page, int quantity) {
        if (quantity < 1) quantity = 1;
        int maxQty = Math.max(1, l.getItem().getAmount());
        if (quantity > maxQty) quantity = maxQty;
        Inventory inv = Bukkit.createInventory(p, 45, ChatColor.GOLD + "Confirm Purchase");
        // Display the item with selected quantity
        ItemStack display = l.getItem().clone();
        display.setAmount(Math.max(1, Math.min(quantity, display.getMaxStackSize())));
        ItemMeta dMeta = display.getItemMeta();
        List<String> lore = dMeta.hasLore() ? new ArrayList<>(Objects.requireNonNull(dMeta.getLore())) : new ArrayList<>();
        double unitPrice = l.getPrice() / Math.max(1, l.getItem().getAmount());
        double total = unitPrice * quantity;
        lore.add(ChatColor.GRAY + "——");
        lore.add(ChatColor.YELLOW + String.format("Unit: %.2f", unitPrice));
        lore.add(ChatColor.YELLOW + String.format("Quantity: %d", quantity));
        lore.add(ChatColor.YELLOW + String.format("Total: %.2f", total));
        dMeta.setLore(lore);
        PersistentDataContainer pdc = dMeta.getPersistentDataContainer();
        pdc.set(Constants.KEY_TYPE, PersistentDataType.STRING, Constants.GUI_CONFIRM);
        pdc.set(Constants.KEY_LISTING_ID, PersistentDataType.STRING, l.getId().toString());
        pdc.set(Constants.KEY_CATEGORY, PersistentDataType.STRING, category != null ? category.name() : "");
        pdc.set(Constants.KEY_PAGE, PersistentDataType.INTEGER, page);
        pdc.set(Constants.KEY_QUANTITY, PersistentDataType.INTEGER, quantity);
        display.setItemMeta(dMeta);
        inv.setItem(CONFIRM_DISPLAY_SLOT, display);

        // Controls
        inv.setItem(BTN_MINUS_32, controlButton(Material.RED_STAINED_GLASS_PANE, ChatColor.RED + "-32", l, category, page, quantity));
        inv.setItem(BTN_MINUS_1, controlButton(Material.RED_STAINED_GLASS_PANE, ChatColor.RED + "-1", l, category, page, quantity));
        inv.setItem(BTN_PLUS_1, controlButton(Material.LIME_STAINED_GLASS_PANE, ChatColor.GREEN + "+1", l, category, page, quantity));
        inv.setItem(BTN_PLUS_32, controlButton(Material.LIME_STAINED_GLASS_PANE, ChatColor.GREEN + "+32", l, category, page, quantity));
        inv.setItem(BTN_CONFIRM, actionButton(Material.EMERALD_BLOCK, ChatColor.GREEN + "Confirm", l, category, page, quantity));
        inv.setItem(BTN_CANCEL, actionButton(Material.BARRIER, ChatColor.RED + "Cancel", l, category, page, quantity));
        inv.setItem(BTN_BUY_ALL, actionButton(Material.GOLD_BLOCK, ChatColor.GOLD + "Buy All", l, category, page, quantity));

        p.openInventory(inv);
    }

    private static ItemStack controlButton(Material mat, String name, Listing l, Category category, int page, int quantity) {
        ItemStack it = new ItemStack(mat);
        ItemMeta meta = it.getItemMeta();
        meta.setDisplayName(name);
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(Constants.KEY_TYPE, PersistentDataType.STRING, Constants.GUI_CONFIRM);
        pdc.set(Constants.KEY_LISTING_ID, PersistentDataType.STRING, l.getId().toString());
        pdc.set(Constants.KEY_CATEGORY, PersistentDataType.STRING, category != null ? category.name() : "");
        pdc.set(Constants.KEY_PAGE, PersistentDataType.INTEGER, page);
        pdc.set(Constants.KEY_QUANTITY, PersistentDataType.INTEGER, quantity);
        it.setItemMeta(meta);
        return it;
    }

    private static ItemStack actionButton(Material mat, String name, Listing l, Category category, int page, int quantity) {
        ItemStack it = controlButton(mat, name, l, category, page, quantity);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            String plain = meta.hasDisplayName() ? ChatColor.stripColor(meta.getDisplayName()) : "";
            double unitPrice = l.getPrice() / Math.max(1, l.getItem().getAmount());
            int qtyForPrice;
            if (plain.equalsIgnoreCase("Confirm")) {
                qtyForPrice = Math.max(1, quantity);
            } else if (plain.equalsIgnoreCase("Buy All")) {
                qtyForPrice = Math.max(1, l.getItem().getAmount());
            } else {
                qtyForPrice = -1; // no lore for other buttons
            }
            if (qtyForPrice > 0) {
                double total = unitPrice * qtyForPrice;
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.YELLOW + String.format("Price: %.2f", total));
                meta.setLore(lore);
                it.setItemMeta(meta);
            }
        }
        return it;
    }
}
