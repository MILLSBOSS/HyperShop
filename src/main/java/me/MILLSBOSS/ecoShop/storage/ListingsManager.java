package me.MILLSBOSS.ecoShop.storage;

import me.MILLSBOSS.ecoShop.model.Category;
import me.MILLSBOSS.ecoShop.model.Listing;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class ListingsManager {
    private final File listingsFile;
    private final File salesFile;

    private final Map<UUID, Listing> listings = new LinkedHashMap<>();
    private final Map<UUID, Double> salesTotals = new HashMap<>();
    // Per-seller sale records to announce on next join
    private final Map<UUID, List<SaleRecord>> saleRecords = new HashMap<>();

    public ListingsManager(File dataFolder) {
        if (!dataFolder.exists()) dataFolder.mkdirs();
        this.listingsFile = new File(dataFolder, "listings.yml");
        this.salesFile = new File(dataFolder, "sales.yml");
        load();
        loadSales();
    }

    public synchronized List<Listing> getAll() {
        return new ArrayList<>(listings.values());
    }

    public synchronized List<Listing> getByCategory(Category category) {
        return listings.values().stream().filter(l -> l.getCategory() == category).collect(Collectors.toList());
    }

    public synchronized List<Listing> getBySeller(UUID seller) {
        return listings.values().stream().filter(l -> l.getSeller().equals(seller)).collect(Collectors.toList());
    }

    public synchronized Listing getById(UUID id) {
        return listings.get(id);
    }

    public synchronized Listing addListing(UUID seller, ItemStack item, double price, Category category) {
        UUID id = UUID.randomUUID();
        Listing listing = new Listing(id, seller, System.currentTimeMillis(), price, item.clone(), category);
        listings.put(id, listing);
        saveAsync();
        return listing;
    }

    public synchronized Listing removeListing(UUID id) {
        Listing removed = listings.remove(id);
        if (removed != null) saveAsync();
        return removed;
    }

    public synchronized void updateListing(Listing listing) {
        if (listing == null) return;
        // Ensure the map contains the listing and then just save
        listings.put(listing.getId(), listing);
        saveAsync();
    }

    public synchronized void addToSales(UUID seller, double amount) {
        salesTotals.merge(seller, amount, Double::sum);
        saveSalesAsync();
    }

    public synchronized double getSalesTotal(UUID seller) {
        return salesTotals.getOrDefault(seller, 0.0);
    }

    /**
     * Record a sale entry for a seller. Stored persistently and cleared when the seller joins and is notified.
     */
    public synchronized void addSaleRecord(UUID seller, UUID buyer, String itemName, int quantity, double amount, long time) {
        List<SaleRecord> list = saleRecords.computeIfAbsent(seller, k -> new ArrayList<>());
        list.add(new SaleRecord(buyer, itemName, quantity, amount, time));
        saveSalesAsync();
    }

    /**
     * Returns and clears pending sale records for the given seller.
     */
    public synchronized List<SaleRecord> drainSaleRecords(UUID seller) {
        List<SaleRecord> list = saleRecords.remove(seller);
        if (list == null) return Collections.emptyList();
        saveSalesAsync();
        return list;
    }

    private void load() {
        if (!listingsFile.exists()) return;
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(listingsFile);
        ConfigurationSection sec = cfg.getConfigurationSection("listings");
        if (sec == null) return;
        for (String key : sec.getKeys(false)) {
            try {
                UUID id = UUID.fromString(key);
                ConfigurationSection lsec = sec.getConfigurationSection(key);
                if (lsec == null) continue;
                UUID seller = UUID.fromString(Objects.requireNonNull(lsec.getString("seller")));
                long created = lsec.getLong("created");
                double price = lsec.getDouble("price");
                String catName = lsec.getString("category");
                Category category = Category.valueOf(catName);
                ItemStack item = lsec.getItemStack("item");
                if (item == null) continue;
                listings.put(id, new Listing(id, seller, created, price, item, category));
            } catch (Exception ex) {
                Bukkit.getLogger().warning("Failed to load listing " + key + ": " + ex.getMessage());
            }
        }
    }

    private void loadSales() {
        if (!salesFile.exists()) return;
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(salesFile);
        ConfigurationSection totalsSec = cfg.getConfigurationSection("sales");
        if (totalsSec != null) {
            for (String key : totalsSec.getKeys(false)) {
                try {
                    UUID uid = UUID.fromString(key);
                    double total = totalsSec.getDouble(key);
                    salesTotals.put(uid, total);
                } catch (Exception ignored) {}
            }
        }
        ConfigurationSection recordsSec = cfg.getConfigurationSection("records");
        if (recordsSec != null) {
            for (String sellerKey : recordsSec.getKeys(false)) {
                try {
                    UUID seller = UUID.fromString(sellerKey);
                    List<Map<String, Object>> rawList = (List<Map<String, Object>>) recordsSec.getList(sellerKey);
                    if (rawList == null) continue;
                    List<SaleRecord> list = new ArrayList<>();
                    for (Map<String, Object> map : rawList) {
                        try {
                            UUID buyer = UUID.fromString(String.valueOf(map.get("buyer")));
                            String itemName = String.valueOf(map.getOrDefault("item", "Item"));
                            int qty = Integer.parseInt(String.valueOf(map.getOrDefault("qty", 1)));
                            double amount = Double.parseDouble(String.valueOf(map.getOrDefault("amount", 0.0)));
                            long time = Long.parseLong(String.valueOf(map.getOrDefault("time", System.currentTimeMillis())));
                            list.add(new SaleRecord(buyer, itemName, qty, amount, time));
                        } catch (Exception ignored) {}
                    }
                    if (!list.isEmpty()) saleRecords.put(seller, list);
                } catch (Exception ignored) {}
            }
        }
    }

    private void saveAsync() {
        Bukkit.getScheduler().runTaskAsynchronously(Bukkit.getPluginManager().getPlugin("EcoShopPro"), this::save);
    }

    private void save() {
        FileConfiguration cfg = new YamlConfiguration();
        ConfigurationSection sec = cfg.createSection("listings");
        for (Map.Entry<UUID, Listing> e : listings.entrySet()) {
            String key = e.getKey().toString();
            Listing l = e.getValue();
            ConfigurationSection lsec = sec.createSection(key);
            lsec.set("seller", l.getSeller().toString());
            lsec.set("created", l.getCreatedAt());
            lsec.set("price", l.getPrice());
            lsec.set("category", l.getCategory().name());
            lsec.set("item", l.getItem());
        }
        try {
            cfg.save(listingsFile);
        } catch (IOException ex) {
            Bukkit.getLogger().warning("Failed to save listings: " + ex.getMessage());
        }
    }

    private void saveSalesAsync() {
        Bukkit.getScheduler().runTaskAsynchronously(Bukkit.getPluginManager().getPlugin("EcoShopPro"), this::saveSales);
    }

    private void saveSales() {
        FileConfiguration cfg = new YamlConfiguration();
        ConfigurationSection sec = cfg.createSection("sales");
        for (Map.Entry<UUID, Double> e : salesTotals.entrySet()) {
            sec.set(e.getKey().toString(), e.getValue());
        }
        // Save records section
        ConfigurationSection rec = cfg.createSection("records");
        for (Map.Entry<UUID, List<SaleRecord>> e : saleRecords.entrySet()) {
            List<Map<String, Object>> list = new ArrayList<>();
            for (SaleRecord r : e.getValue()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("buyer", r.buyer.toString());
                m.put("item", r.itemName);
                m.put("qty", r.quantity);
                m.put("amount", r.amount);
                m.put("time", r.time);
                list.add(m);
            }
            rec.set(e.getKey().toString(), list);
        }
        try {
            cfg.save(salesFile);
        } catch (IOException ex) {
            Bukkit.getLogger().warning("Failed to save sales: " + ex.getMessage());
        }
    }

    // Simple immutable sale record
    public static class SaleRecord {
        public final UUID buyer;
        public final String itemName;
        public final int quantity;
        public final double amount;
        public final long time;

        public SaleRecord(UUID buyer, String itemName, int quantity, double amount, long time) {
            this.buyer = buyer;
            this.itemName = itemName;
            this.quantity = quantity;
            this.amount = amount;
            this.time = time;
        }
    }
}
