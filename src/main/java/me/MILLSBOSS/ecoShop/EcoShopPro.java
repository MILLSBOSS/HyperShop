package me.MILLSBOSS.ecoShop;

import me.MILLSBOSS.ecoShop.pricing.Pricing;
import me.MILLSBOSS.ecoShop.storage.ListingsManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class EcoShopPro extends JavaPlugin {

    private static EcoShopPro instance;
    private Economy economy;
    private ListingsManager listingsManager;
    private long listingThrottleMs;

    public static EcoShopPro getInstance() { return instance; }

    public Economy getEconomy() { return economy; }
    public ListingsManager getListingsManager() { return listingsManager; }
    public long getListingThrottleMs() { return listingThrottleMs; }

    @Override
    public void onEnable() {
        instance = this;
        // Config and pricing
        saveDefaultConfig();
        // Load throttle value (ms) to space out listing actions and avoid anticheat bans
        this.listingThrottleMs = getConfig().getLong("listing_throttle_ms", 600L);
        Pricing.load(this);
        // Storage
        this.listingsManager = new ListingsManager(getDataFolder());
        // Vault
        if (!setupEconomy()) {
            getLogger().warning("Vault not found or no economy provider registered. Balances will show as 0 and buying will be disabled.");
        }
        // Register listeners
        Bukkit.getPluginManager().registerEvents(new ShopListener(this), this);
        getLogger().info("EcoShopPro enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("EcoShopPro disabled.");
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("shop")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Only players can use this command.");
                return true;
            }
            Player p = (Player) sender;
            if (!p.hasPermission("EcoShopPro.use")) {
                p.sendMessage("You don't have permission.");
                return true;
            }
            ShopMenus.openMain(p);
            return true;
        }
        return false;
    }
}
