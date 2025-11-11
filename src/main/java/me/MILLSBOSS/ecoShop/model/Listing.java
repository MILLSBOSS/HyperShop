package me.MILLSBOSS.ecoShop.model;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class Listing {
    private final UUID id;
    private final UUID seller;
    private final long createdAt;
    private double price;
    private ItemStack item;
    private Category category;

    public Listing(UUID id, UUID seller, long createdAt, double price, ItemStack item, Category category) {
        this.id = id;
        this.seller = seller;
        this.createdAt = createdAt;
        this.price = price;
        this.item = item;
        this.category = category;
    }

    public UUID getId() { return id; }
    public UUID getSeller() { return seller; }
    public long getCreatedAt() { return createdAt; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public ItemStack getItem() { return item; }
    public void setItem(ItemStack item) { this.item = item; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
}
