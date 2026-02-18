package me.MILLSBOSS.ecoShop;

import me.MILLSBOSS.ecoShop.model.Category;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Inventory holder used to reliably identify HyperShop GUIs.
 * This prevents interference with other plugins by avoiding title-based checks.
 */
public final class ShopHolder implements InventoryHolder {
    private final String type; // see Constants.GUI_*
    private final Category category; // optional
    private final int page; // optional

    public ShopHolder(String type, Category category, int page) {
        this.type = type;
        this.category = category;
        this.page = page;
    }

    public String getType() {
        return type;
    }

    public Category getCategory() {
        return category;
    }

    public int getPage() {
        return page;
    }

    @Override
    public Inventory getInventory() {
        // Not used; Bukkit will call this only for some holders. Return null is acceptable.
        return null;
    }
}
