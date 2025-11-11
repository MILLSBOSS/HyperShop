package me.MILLSBOSS.ecoShop;

import me.MILLSBOSS.ecoShop.storage.ListingsManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class EcoShop extends JavaPlugin {

    private static EcoShop instance;
    private Economy economy;
    private ListingsManager listingsManager;

    public static EcoShop getInstance() { return instance; }

    public Economy getEconomy() { return economy; }
    public ListingsManager getListingsManager() { return listingsManager; }

    @Override
    public void onEnable() {
        instance = this;
        // Storage
        this.listingsManager = new ListingsManager(getDataFolder());
        // Vault
        if (!setupEconomy()) {
            getLogger().warning("Vault not found or no economy provider registered. Balances will show as 0 and buying will be disabled.");
        }
        // Register listeners
        Bukkit.getPluginManager().registerEvents(new ShopListener(this), this);
        getLogger().info("EcoShop enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("EcoShop disabled.");
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
            if (!p.hasPermission("ecoshop.use")) {
                p.sendMessage("You don't have permission.");
                return true;
            }
            ShopMenus.openMain(p);
            return true;
        }
        return false;
    }
}
