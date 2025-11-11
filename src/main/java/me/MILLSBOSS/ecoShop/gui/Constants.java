package me.MILLSBOSS.ecoShop.gui;

import me.MILLSBOSS.ecoShop.EcoShop;
import org.bukkit.NamespacedKey;

public final class Constants {
    public static final NamespacedKey KEY_TYPE = new NamespacedKey(EcoShop.getInstance(), "gui-type");
    public static final NamespacedKey KEY_CATEGORY = new NamespacedKey(EcoShop.getInstance(), "category");
    public static final NamespacedKey KEY_LISTING_ID = new NamespacedKey(EcoShop.getInstance(), "listing-id");
    public static final String GUI_MAIN = "main";
    public static final String GUI_CATEGORY = "category";
    public static final String GUI_MY_LISTINGS = "mylistings";
    public static final String GUI_SELL_PLACEHOLDER = "sell-placeholder";
    private Constants() {}
}
