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
import java.util.stream.Collectors;

public final class ShopMenus {

    private static final int SELL_SLOT = 49; // bottom row middle
    private static final int MY_LISTINGS_SLOT = 50;
    private static final int PLAYER_HEAD_SLOT = 53;

    private ShopMenus() {}

    public static void openMain(Player p) {
        Inventory inv = Bukkit.createInventory(p, 54, ChatColor.GREEN + "EcoShop");
        tagInventory(inv, Constants.GUI_MAIN, null, -1);
        // Fill with categories
        int[] catSlots = new int[]{10,11,12,13,14,15,16,19,20};
        Category[] cats = new Category[]{
                Category.BUILDING_BLOCKS,
                Category.TOOLS,
                Category.WEAPONS,
                Category.ARMOR,
                Category.FOOD,
                Category.ORES,
                Category.PLANTS,
                Category.REDSTONE,
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
        ListingsManager lm = EcoShop.getInstance().getListingsManager();
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
        ListingsManager lm = EcoShop.getInstance().getListingsManager();
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
        Economy eco = EcoShop.getInstance().getEconomy();
        if (eco != null) bal = eco.getBalance(p);
        sales = EcoShop.getInstance().getListingsManager().getSalesTotal(p.getUniqueId());
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
}
