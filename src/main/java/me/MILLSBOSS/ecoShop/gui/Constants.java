package me.MILLSBOSS.ecoShop.gui;

import me.MILLSBOSS.ecoShop.EcoShopPro;
import org.bukkit.NamespacedKey;

public final class Constants {
    public static final NamespacedKey KEY_TYPE = new NamespacedKey(EcoShopPro.getInstance(), "gui-type");
    public static final NamespacedKey KEY_CATEGORY = new NamespacedKey(EcoShopPro.getInstance(), "category");
    public static final NamespacedKey KEY_LISTING_ID = new NamespacedKey(EcoShopPro.getInstance(), "listing-id");
    public static final NamespacedKey KEY_PAGE = new NamespacedKey(EcoShopPro.getInstance(), "page");
    public static final NamespacedKey KEY_QUANTITY = new NamespacedKey(EcoShopPro.getInstance(), "quantity");

    public static final String GUI_MAIN = "main";
    public static final String GUI_CATEGORY = "category";
    public static final String GUI_MY_LISTINGS = "mylistings";
    public static final String GUI_SELL_PLACEHOLDER = "sell-placeholder";
    public static final String GUI_CONFIRM = "confirm";

    private Constants() {}
}
