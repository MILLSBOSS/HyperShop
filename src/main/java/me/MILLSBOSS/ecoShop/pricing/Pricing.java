package me.MILLSBOSS.ecoShop.pricing;

import me.MILLSBOSS.ecoShop.EcoShopPro;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.EntityType;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.block.ShulkerBox;
import org.bukkit.inventory.Inventory;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class Pricing {

    public enum Rarity { BASIC, COMMON, UNCOMMON, RARE, EPIC, NETHERITE_BLOCK, NETHERITE_INGOT, NETHERITE_SCRAP, LEGENDARY, ELYTRA }

    private static final Map<Rarity, Double> perRarityBase = new EnumMap<>(Rarity.class);
    private static ConfigurationSection overridesSection;
    // New: rarity overrides by material via overrides.Blocks/Craftables
    private static final Map<Material, Rarity> rarityOverrides = new HashMap<>();
    // New: spawner rarity overrides by entity type via overrides.Spawners
    private static final Map<EntityType, Rarity> spawnerRarityOverrides = new HashMap<>();
    // New: enchantment book rarity overrides by enchantment via overrides.EnchantmentBooks
    private static final Map<Enchantment, Rarity> enchantmentRarityOverrides = new HashMap<>();
    // Default rarity for a plain spawner (no entity set)
    private static Rarity defaultSpawnerRarity = Rarity.RARE;

    private Pricing() {}

    public static void load(EcoShopPro plugin) {
        // Ensure defaults
        plugin.saveDefaultConfig();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("pricing");
        if (section == null) {
            section = plugin.getConfig().createSection("pricing");
        }
        ConfigurationSection perRarity = section.getConfigurationSection("per_rarity_base");
        if (perRarity == null) perRarity = section.createSection("per_rarity_base");
        // Load with sensible defaults if missing
        perRarityBase.put(Rarity.BASIC, perRarity.getDouble("BASIC", 2.0));
        perRarityBase.put(Rarity.COMMON, perRarity.getDouble("COMMON", 5.0));
        perRarityBase.put(Rarity.UNCOMMON, perRarity.getDouble("UNCOMMON", 20.0));
        perRarityBase.put(Rarity.RARE, perRarity.getDouble("RARE", 100.0));
        perRarityBase.put(Rarity.EPIC, perRarity.getDouble("EPIC", 500.0));
        // Dedicated Netherite Block rarity. Defaults to EPIC to preserve previous behavior unless configured.
        double netheriteBlockDefault = perRarity.getDouble("EPIC", 500.0);
        double netheriteBlockVal = perRarity.getDouble("NETHERITE_BLOCK", netheriteBlockDefault);
        perRarityBase.put(Rarity.NETHERITE_BLOCK, netheriteBlockVal);
        // Dedicated Netherite Scrap rarity. Defaults to EPIC to preserve previous behavior unless configured.
        double netheriteScrapDefault = perRarity.getDouble("EPIC", 500.0);
        double netheriteScrapVal = perRarity.getDouble("NETHERITE_SCRAP", netheriteScrapDefault);
        perRarityBase.put(Rarity.NETHERITE_SCRAP, netheriteScrapVal);
        // Support misspelling "LEGANDERY" in existing configs, fall back to default if absent
        double legendaryDefault = 2000.0;
        double legendaryVal = perRarity.isSet("LEGENDARY") ? perRarity.getDouble("LEGENDARY", legendaryDefault)
                : perRarity.getDouble("LEGANDERY", legendaryDefault);
        perRarityBase.put(Rarity.LEGENDARY, legendaryVal);
        // Dedicated Netherite Ingot rarity: defaults to LEGENDARY value to preserve old behavior unless configured.
        double netheriteIngotDefault = legendaryVal;
        double netheriteIngotVal = perRarity.getDouble("NETHERITE_INGOT", netheriteIngotDefault);
        perRarityBase.put(Rarity.NETHERITE_INGOT, netheriteIngotVal);
        // New dedicated Elytra rarity. If not configured, fall back to LEGENDARY value to preserve previous behavior.
        double elytraDefault = legendaryVal; // preserve old behavior by default
        double elytraVal = perRarity.getDouble("ELYTRA", elytraDefault);
        perRarityBase.put(Rarity.ELYTRA, elytraVal);

        overridesSection = section.getConfigurationSection("overrides");
        // Parse rarity overrides from overrides.Blocks and Craftables
        rarityOverrides.clear();
        spawnerRarityOverrides.clear();
        enchantmentRarityOverrides.clear();
        if (overridesSection != null) {
            ConfigurationSection blocks = overridesSection.getConfigurationSection("Blocks");
            if (blocks != null) {
                for (String key : blocks.getKeys(false)) {
                    if (key == null) continue;
                    String materialKey = key.trim().toUpperCase(Locale.ROOT);
                    Material mat = Material.matchMaterial(materialKey);
                    if (mat == null) continue; // skip unknown materials
                    String rarityStr = blocks.getString(key);
                    if (rarityStr == null || rarityStr.trim().isEmpty()) {
                        // default to COMMON if unspecified
                        rarityOverrides.put(mat, Rarity.COMMON);
                        continue;
                    }
                    Rarity r = parseRarity(rarityStr);
                    if (r != null) {
                        rarityOverrides.put(mat, r);
                    }
                }
            }
            // Also parse rarity overrides from overrides.Craftables
            ConfigurationSection craftables = overridesSection.getConfigurationSection("Craftables");
            if (craftables != null) {
                for (String key : craftables.getKeys(false)) {
                    if (key == null) continue;
                    String materialKey = key.trim().toUpperCase(Locale.ROOT);
                    Material mat = Material.matchMaterial(materialKey);
                    if (mat == null) continue; // skip unknown materials
                    String rarityStr = craftables.getString(key);
                    if (rarityStr == null || rarityStr.trim().isEmpty()) {
                        // default to COMMON if unspecified
                        rarityOverrides.put(mat, Rarity.COMMON);
                        continue;
                    }
                    Rarity r = parseRarity(rarityStr);
                    if (r != null) {
                        rarityOverrides.put(mat, r);
                    }
                }
            }
            // New: parse overrides.Ores (individual ore items like DIAMOND, IRON_INGOT, REDSTONE, etc.)
            ConfigurationSection ores = overridesSection.getConfigurationSection("Ores");
            if (ores != null) {
                for (String key : ores.getKeys(false)) {
                    if (key == null) continue;
                    String materialKey = key.trim().toUpperCase(Locale.ROOT);
                    Material mat = Material.matchMaterial(materialKey);
                    if (mat == null) continue; // skip unknown materials
                    String rarityStr = ores.getString(key);
                    if (rarityStr == null || rarityStr.trim().isEmpty()) {
                        rarityOverrides.put(mat, Rarity.COMMON);
                        continue;
                    }
                    Rarity r = parseRarity(rarityStr);
                    if (r != null) {
                        rarityOverrides.put(mat, r);
                    }
                }
            }
            // New: parse overrides.SpawnEggs (all spawn egg materials like ZOMBIE_SPAWN_EGG, CREEPER_SPAWN_EGG, etc.)
            ConfigurationSection spawnEggs = overridesSection.getConfigurationSection("SpawnEggs");
            if (spawnEggs != null) {
                for (String key : spawnEggs.getKeys(false)) {
                    if (key == null) continue;
                    String materialKey = key.trim().toUpperCase(Locale.ROOT);
                    Material mat = Material.matchMaterial(materialKey);
                    if (mat == null) continue; // skip unknown materials
                    String rarityStr = spawnEggs.getString(key);
                    if (rarityStr == null || rarityStr.trim().isEmpty()) {
                        rarityOverrides.put(mat, Rarity.COMMON);
                        continue;
                    }
                    Rarity r = parseRarity(rarityStr);
                    if (r != null) {
                        rarityOverrides.put(mat, r);
                    }
                }
            }
            // New: parse overrides.EnchantmentBooks (rarity per enchantment for enchanted books)
            ConfigurationSection enchantBooks = overridesSection.getConfigurationSection("EnchantmentBooks");
            if (enchantBooks != null) {
                for (String key : enchantBooks.getKeys(false)) {
                    if (key == null) continue;
                    String enchKey = key.trim().toUpperCase(Locale.ROOT);
                    Enchantment ench = Enchantment.getByName(enchKey);
                    if (ench == null) continue; // skip unknown enchantments on this server version
                    String rarityStr = enchantBooks.getString(key);
                    if (rarityStr == null || rarityStr.trim().isEmpty()) {
                        enchantmentRarityOverrides.put(ench, Rarity.COMMON);
                        continue;
                    }
                    Rarity r = parseRarity(rarityStr);
                    if (r != null) {
                        enchantmentRarityOverrides.put(ench, r);
                    }
                }
            }
            // Parse spawner rarity overrides from overrides.Spawners
            ConfigurationSection spawners = overridesSection.getConfigurationSection("Spawners");
            if (spawners != null) {
                for (String key : spawners.getKeys(false)) {
                    if (key == null) continue;
                    String rawKey = key.trim();
                    String upper = rawKey.toUpperCase(Locale.ROOT);
                    String rarityStr = spawners.getString(key);
                    if ("PLAIN".equals(upper)) {
                        // Special default rarity for a plain (unset) spawner item
                        Rarity r = parseRarity(rarityStr != null ? rarityStr : "rare");
                        if (r != null) defaultSpawnerRarity = r;
                        continue;
                    }
                    EntityType type;
                    try {
                        type = EntityType.valueOf(upper);
                    } catch (IllegalArgumentException ex) {
                        continue; // skip unknown entity types
                    }
                    if (rarityStr == null || rarityStr.trim().isEmpty()) {
                        spawnerRarityOverrides.put(type, Rarity.COMMON);
                        continue;
                    }
                    Rarity r = parseRarity(rarityStr);
                    if (r != null) {
                        spawnerRarityOverrides.put(type, r);
                    }
                }
            }
        }

        // Persist defaults back if not present
        perRarity.set("BASIC", perRarityBase.get(Rarity.BASIC));
        perRarity.set("COMMON", perRarityBase.get(Rarity.COMMON));
        perRarity.set("UNCOMMON", perRarityBase.get(Rarity.UNCOMMON));
        perRarity.set("RARE", perRarityBase.get(Rarity.RARE));
        perRarity.set("EPIC", perRarityBase.get(Rarity.EPIC));
        perRarity.set("NETHERITE_BLOCK", perRarityBase.get(Rarity.NETHERITE_BLOCK));
        perRarity.set("NETHERITE_SCRAP", perRarityBase.get(Rarity.NETHERITE_SCRAP));
        perRarity.set("NETHERITE_INGOT", perRarityBase.get(Rarity.NETHERITE_INGOT));
        perRarity.set("LEGENDARY", perRarityBase.get(Rarity.LEGENDARY));
        perRarity.set("ELYTRA", perRarityBase.get(Rarity.ELYTRA));
        // If user had the misspelled key, clean it up
        if (perRarity.isSet("LEGANDERY")) perRarity.set("LEGANDERY", null);
        plugin.saveConfig();
    }

    public static double suggestMaxPrice(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return 0.0;
        Material mat = item.getType();

        // Special handling: Shulker boxes should be priced based on their contents
        try {
            if (item.hasItemMeta() && item.getItemMeta() instanceof BlockStateMeta) {
                BlockStateMeta bsm = (BlockStateMeta) item.getItemMeta();
                BlockState state = bsm.getBlockState();
                if (state instanceof ShulkerBox) {
                    Inventory inv = ((ShulkerBox) state).getInventory();
                    double total = 0.0;
                    if (inv != null) {
                        for (ItemStack is : inv.getContents()) {
                            if (is == null || is.getType() == Material.AIR) continue;
                            total += suggestMaxPrice(is);
                        }
                    }
                    return round2(total);
                }
            }
        } catch (Throwable ignored) {
            // If server API differs, fall through to normal pricing
        }

        // Per-material numeric override (per unit) if present
        double perUnit;
        Double override = getNumericOverride(mat);
        if (override != null) {
            perUnit = override;
        } else if (mat == Material.SPAWNER) {
            // Special handling for spawners: use the spawned entity type mapping if available
            Rarity r = null;
            try {
                if (item.hasItemMeta() && item.getItemMeta() instanceof BlockStateMeta) {
                    BlockStateMeta meta = (BlockStateMeta) item.getItemMeta();
                    BlockState state = meta.getBlockState();
                    if (state instanceof CreatureSpawner) {
                        EntityType type = ((CreatureSpawner) state).getSpawnedType();
                        if (type != null) {
                            r = spawnerRarityOverrides.get(type);
                        }
                    }
                }
            } catch (Throwable ignored) {
                // In case server API/version doesn't support reading block state meta, fall back below
            }
            if (r == null) {
                r = defaultSpawnerRarity; // default assumption for plain/unspecified spawners
            }
            perUnit = perRarityBase.getOrDefault(r, 5.0);
        } else if (mat == Material.ENCHANTED_BOOK) {
            // Enchanted book: determine rarity based on stored enchantments using overrides.EnchantmentBooks
            Rarity chosen = Rarity.COMMON;
            double best = perRarityBase.getOrDefault(chosen, 5.0);
            try {
                if (item.hasItemMeta() && item.getItemMeta() instanceof EnchantmentStorageMeta) {
                    EnchantmentStorageMeta meta = (EnchantmentStorageMeta) item.getItemMeta();
                    Map<Enchantment, Integer> enchants = meta.getStoredEnchants();
                    if (enchants != null && !enchants.isEmpty()) {
                        for (Enchantment ench : enchants.keySet()) {
                            Rarity er = enchantmentRarityOverrides.get(ench);
                            if (er == null) er = Rarity.COMMON; // fallback if not specified
                            double val = perRarityBase.getOrDefault(er, 5.0);
                            if (val > best) {
                                best = val;
                                chosen = er;
                            }
                        }
                    }
                }
            } catch (Throwable ignored) {
                // If API differs, fall back to common
            }
            perUnit = perRarityBase.getOrDefault(chosen, 5.0);
        } else {
            // Next, check rarity override from blocks/craftables category
            Rarity rOverride = rarityOverrides.get(mat);
            Rarity r = (rOverride != null) ? rOverride : inferRarity(mat);
            perUnit = perRarityBase.getOrDefault(r, 5.0);
        }
        int amount = Math.max(1, item.getAmount());
        return round2(perUnit * amount);
    }

    private static Double getNumericOverride(Material mat) {
        if (overridesSection == null) return null;
        String key = mat.name().toUpperCase(Locale.ROOT);
        if (overridesSection.isDouble(key)) {
            return overridesSection.getDouble(key);
        }
        if (overridesSection.isInt(key)) {
            return (double) overridesSection.getInt(key);
        }
        return null;
    }

    private static Rarity parseRarity(String s) {
        String v = s.trim().toUpperCase(Locale.ROOT);
        try {
            return Rarity.valueOf(v);
        } catch (IllegalArgumentException ex) {
            // Support lower-case terms like "common" etc., already upper-cased; if unknown, return null
            return null;
        }
    }

    public static Rarity inferRarity(Material mat) {
        if (mat == Material.ELYTRA) return Rarity.ELYTRA; // Elytra has its own rarity tier
        if (mat == Material.NETHERITE_BLOCK) return Rarity.NETHERITE_BLOCK; // Dedicated rarity for Netherite Block
        if (mat == Material.NETHERITE_INGOT) return Rarity.NETHERITE_INGOT; // Dedicated rarity for Netherite Ingot
        if (mat == Material.NETHERITE_SCRAP) return Rarity.NETHERITE_SCRAP; // Dedicated rarity for Netherite Scrap
        String name = mat.name();
        // Very lightweight heuristic
        if (containsAny(name, "NETHERITE", "DRAGON", "BEACON", "TOTEM")) return Rarity.EPIC;
        if (containsAny(name, "DIAMOND", "ANCIENT_DEBRIS", "NETHER_STAR", "SPONGE", "SHULKER")) return Rarity.RARE;
        if (containsAny(name, "GOLD", "EMERALD", "ENCHANTED", "TRIDENT", "AMETHYST")) return Rarity.UNCOMMON;
        if (containsAny(name, "IRON", "COPPER", "LAPIS", "REDSTONE")) return Rarity.UNCOMMON;
        // Blocks and common resources
        if (containsAny(name, "DIRT", "STONE", "SAND", "GRAVEL", "WOOD", "PLANKS", "SAPLING", "SEEDS", "WOOL", "GLASS")) return Rarity.COMMON;
        return Rarity.COMMON;
    }

    private static boolean containsAny(String name, String... terms) {
        for (String t : terms) if (name.contains(t)) return true;
        return false;
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
