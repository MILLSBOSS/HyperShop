package me.MILLSBOSS.ecoShop.pricing;

import me.MILLSBOSS.ecoShop.HyperShop;
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

    public enum Rarity { BASIC, COMMON, UNCOMMON, RARE, EPIC, NETHERITE_BLOCK, NETHERITE_INGOT, NETHERITE_SCRAP, ANCIENT_DEBRIS, LEGENDARY, MYTHIC, ELYTRA }

    private static final Map<Rarity, Double> perRarityBase = new EnumMap<>(Rarity.class);
    // Armor-specific base values per rarity
    private static final Map<Rarity, Double> perRarityBaseArmor = new EnumMap<>(Rarity.class);
    // Blocks-specific base values per rarity
    private static final Map<Rarity, Double> perRarityBaseBlocks = new EnumMap<>(Rarity.class);
    // Ores-specific base values per rarity
    private static final Map<Rarity, Double> perRarityBaseOres = new EnumMap<>(Rarity.class);
    // Ore drops-specific base values per rarity (e.g., COAL, RAW_IRON, REDSTONE, DIAMOND, etc.)
    private static final Map<Rarity, Double> perRarityBaseOreDrops = new EnumMap<>(Rarity.class);
    // Ingots-specific base values per rarity
    private static final Map<Rarity, Double> perRarityBaseIngots = new EnumMap<>(Rarity.class);
    // Spawners-specific base values per rarity
    private static final Map<Rarity, Double> perRarityBaseSpawners = new EnumMap<>(Rarity.class);
    // Spawn eggs-specific base values per rarity
    private static final Map<Rarity, Double> perRarityBaseSpawnEggs = new EnumMap<>(Rarity.class);
    // Mob heads-specific base values per rarity
    private static final Map<Rarity, Double> perRarityBaseMobHeads = new EnumMap<>(Rarity.class);
    // Enchantment books-specific base values per rarity (BASIC..RARE)
    private static final Map<Rarity, Double> perRarityBaseEnchantmentBooks = new EnumMap<>(Rarity.class);
    // Saplings and seeds-specific base values per rarity (BASIC..RARE)
    private static final Map<Rarity, Double> perRarityBaseSaplingsSeeds = new EnumMap<>(Rarity.class);
    // Froglights-specific base values per rarity (BASIC only)
    private static final Map<Rarity, Double> perRarityBaseFroglights = new EnumMap<>(Rarity.class);
    private static ConfigurationSection overridesSection;
    // New: rarity overrides by material via overrides.Blocks/Craftables
    private static final Map<Material, Rarity> rarityOverrides = new HashMap<>();
    // New: spawner rarity overrides by entity type via overrides.Spawners
    private static final Map<EntityType, Rarity> spawnerRarityOverrides = new HashMap<>();
    // New: enchantment book rarity overrides by enchantment via overrides.EnchantmentBooks
    private static final Map<Enchantment, Rarity> enchantmentRarityOverrides = new HashMap<>();
    // Default rarity for a plain spawner (no entity set)
    private static Rarity defaultSpawnerRarity = Rarity.RARE;

    // Dedicated base maps for different item families
    private static final Map<Rarity, Double> perRarityBaseResourceBlocks = new EnumMap<>(Rarity.class);

    private Pricing() {}

    public static void load(HyperShop plugin) {
        // Ensure defaults
        plugin.saveDefaultConfig();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("pricing");
        if (section == null) {
            section = plugin.getConfig().createSection("pricing");
        }
        // Primary location (new): pricing.per_rarity_base_craftables; Legacy fallback: pricing.per_rarity_base
        ConfigurationSection perRarity = section.getConfigurationSection("per_rarity_base_craftables");
        if (perRarity == null) {
            perRarity = section.getConfigurationSection("per_rarity_base");
        }
        if (perRarity == null) perRarity = section.createSection("per_rarity_base_craftables");
        // Load with sensible defaults if missing. Expect BASIC only (BASE removed from config).
        double basicVal;
        if (perRarity.isSet("BASIC")) {
            basicVal = perRarity.getDouble("BASIC", 2.0);
        } else {
            basicVal = 2.0;
        }
        perRarityBase.put(Rarity.BASIC, basicVal);
        perRarityBase.put(Rarity.COMMON, perRarity.getDouble("COMMON", 5.0));
        perRarityBase.put(Rarity.UNCOMMON, perRarity.getDouble("UNCOMMON", 20.0));
        perRarityBase.put(Rarity.RARE, perRarity.getDouble("RARE", 100.0));
        perRarityBase.put(Rarity.EPIC, perRarity.getDouble("EPIC", 500.0));
        // NETHERITE_BLOCK now belongs to per_rarity_base_resource_blocks (not general craftables)
        // Dedicated Netherite Scrap rarity. Defaults to EPIC to preserve previous behavior unless configured.
        double netheriteScrapDefault = perRarity.getDouble("EPIC", 500.0);
        double netheriteScrapVal = perRarity.getDouble("NETHERITE_SCRAP", netheriteScrapDefault);
        perRarityBase.put(Rarity.NETHERITE_SCRAP, netheriteScrapVal);
        // Support misspelling "LEGANDERY" in existing configs, fall back to default if absent
        double legendaryDefault = 2000.0;
        double legendaryVal = perRarity.isSet("LEGENDARY") ? perRarity.getDouble("LEGENDARY", legendaryDefault)
                : perRarity.getDouble("LEGANDERY", legendaryDefault);
        perRarityBase.put(Rarity.LEGENDARY, legendaryVal);
        // NETHERITE_INGOT now belongs to per_rarity_base_ingots (not general craftables)
        // New dedicated Elytra rarity. If not configured, fall back to LEGENDARY value to preserve previous behavior.
        double elytraDefault = legendaryVal; // preserve old behavior by default
        double elytraVal = perRarity.getDouble("ELYTRA", elytraDefault);
        perRarityBase.put(Rarity.ELYTRA, elytraVal);
        // Ancient Debris dedicated rarity now belongs to per_rarity_base_ores (not general craftables)

        // Armor-specific per-rarity base, defaults to general per_rarity_base if missing to preserve compatibility
        // Primary location (legacy): pricing.per_rarity_base_armor
        ConfigurationSection perRarityArmor = section.getConfigurationSection("per_rarity_base_armor");
        if (perRarityArmor == null) {
            // New supported locations inside overrides:
            // 1) pricing.overrides.per_rarity_base_armor (near Craftables)
            // 2) pricing.overrides.Craftables.per_rarity_base_armor (directly above the Armor subsection)
            ConfigurationSection overridesRoot = section.getConfigurationSection("overrides");
            if (overridesRoot != null) {
                perRarityArmor = overridesRoot.getConfigurationSection("per_rarity_base_armor");
                if (perRarityArmor == null) {
                    ConfigurationSection craftablesRoot = overridesRoot.getConfigurationSection("Craftables");
                    if (craftablesRoot != null) {
                        perRarityArmor = craftablesRoot.getConfigurationSection("per_rarity_base_armor");
                    }
                }
            }
        }
        if (perRarityArmor == null) perRarityArmor = section.createSection("per_rarity_base_armor");
        perRarityBaseArmor.put(Rarity.BASIC, perRarityArmor.getDouble("BASIC", perRarityBase.get(Rarity.BASIC)));
        perRarityBaseArmor.put(Rarity.COMMON, perRarityArmor.getDouble("COMMON", perRarityBase.get(Rarity.COMMON)));
        perRarityBaseArmor.put(Rarity.UNCOMMON, perRarityArmor.getDouble("UNCOMMON", perRarityBase.get(Rarity.UNCOMMON)));
        perRarityBaseArmor.put(Rarity.RARE, perRarityArmor.getDouble("RARE", perRarityBase.get(Rarity.RARE)));
        perRarityBaseArmor.put(Rarity.EPIC, perRarityArmor.getDouble("EPIC", perRarityBase.get(Rarity.EPIC)));
        perRarityBaseArmor.put(Rarity.LEGENDARY, perRarityArmor.getDouble("LEGENDARY", perRarityBase.get(Rarity.LEGENDARY)));
        // MYTHIC defaults to LEGENDARY value from general per_rarity_base if not specified
        perRarityBaseArmor.put(Rarity.MYTHIC, perRarityArmor.getDouble("MYTHIC", perRarityBase.get(Rarity.LEGENDARY)));

        // Blocks-specific per-rarity base, defaults to general per_rarity_base if missing
        // Primary location (legacy): pricing.per_rarity_base_blocks
        ConfigurationSection perRarityBlocks = section.getConfigurationSection("per_rarity_base_blocks");
        if (perRarityBlocks == null) {
            // New supported location: pricing.overrides.per_rarity_base_blocks (placed directly above overrides.Blocks in config.yml)
            ConfigurationSection overridesRoot = section.getConfigurationSection("overrides");
            if (overridesRoot != null) {
                perRarityBlocks = overridesRoot.getConfigurationSection("per_rarity_base_blocks");
            }
        }
        if (perRarityBlocks == null) perRarityBlocks = section.createSection("per_rarity_base_blocks");
        // Only read BASIC..RARE from the blocks-specific section; higher tiers fall back to general per_rarity_base
        perRarityBaseBlocks.put(Rarity.BASIC, perRarityBlocks.getDouble("BASIC", perRarityBase.get(Rarity.BASIC)));
        perRarityBaseBlocks.put(Rarity.COMMON, perRarityBlocks.getDouble("COMMON", perRarityBase.get(Rarity.COMMON)));
        perRarityBaseBlocks.put(Rarity.UNCOMMON, perRarityBlocks.getDouble("UNCOMMON", perRarityBase.get(Rarity.UNCOMMON)));
        perRarityBaseBlocks.put(Rarity.RARE, perRarityBlocks.getDouble("RARE", perRarityBase.get(Rarity.RARE)));
        // For EPIC/LEGENDARY tiers, use the general base values; MYTHIC falls back to LEGENDARY by design
        perRarityBaseBlocks.put(Rarity.EPIC, perRarityBase.get(Rarity.EPIC));
        perRarityBaseBlocks.put(Rarity.LEGENDARY, perRarityBase.get(Rarity.LEGENDARY));
        perRarityBaseBlocks.put(Rarity.MYTHIC, perRarityBase.get(Rarity.LEGENDARY));

        // Resource Blocks-specific per-rarity base (for GOLD_BLOCK, IRON_BLOCK, etc.)
        // Primary location: pricing.per_rarity_base_resource_blocks
        ConfigurationSection perRarityResourceBlocks = section.getConfigurationSection("per_rarity_base_resource_blocks");
        if (perRarityResourceBlocks == null) {
            // Support new location under overrides (placed directly above overrides.ResourceBlocks in config.yml)
            ConfigurationSection overridesRoot = section.getConfigurationSection("overrides");
            if (overridesRoot != null) {
                perRarityResourceBlocks = overridesRoot.getConfigurationSection("per_rarity_base_resource_blocks");
            }
        }
        if (perRarityResourceBlocks == null) perRarityResourceBlocks = section.createSection("per_rarity_base_resource_blocks");
        // Only BASIC..RARE are defined here; higher tiers fall back to general per_rarity_base similar to normal blocks
        perRarityBaseResourceBlocks.put(Rarity.BASIC, perRarityResourceBlocks.getDouble("BASIC", perRarityBaseBlocks.getOrDefault(Rarity.BASIC, perRarityBase.get(Rarity.BASIC))));
        perRarityBaseResourceBlocks.put(Rarity.COMMON, perRarityResourceBlocks.getDouble("COMMON", perRarityBaseBlocks.getOrDefault(Rarity.COMMON, perRarityBase.get(Rarity.COMMON))));
        perRarityBaseResourceBlocks.put(Rarity.UNCOMMON, perRarityResourceBlocks.getDouble("UNCOMMON", perRarityBaseBlocks.getOrDefault(Rarity.UNCOMMON, perRarityBase.get(Rarity.UNCOMMON))));
        perRarityBaseResourceBlocks.put(Rarity.RARE, perRarityResourceBlocks.getDouble("RARE", perRarityBaseBlocks.getOrDefault(Rarity.RARE, perRarityBase.get(Rarity.RARE))));
        // Dedicated NETHERITE_BLOCK base now lives in resource blocks map; default to EPIC general value if missing
        double nethBlockDefault = perRarityBase.getOrDefault(Rarity.EPIC, 500.0);
        perRarityBaseResourceBlocks.put(Rarity.NETHERITE_BLOCK, perRarityResourceBlocks.getDouble("NETHERITE_BLOCK", nethBlockDefault));
        perRarityBaseResourceBlocks.put(Rarity.EPIC, perRarityBase.get(Rarity.EPIC));
        perRarityBaseResourceBlocks.put(Rarity.LEGENDARY, perRarityBase.get(Rarity.LEGENDARY));
        perRarityBaseResourceBlocks.put(Rarity.MYTHIC, perRarityBase.get(Rarity.LEGENDARY));

        // Ores-specific per-rarity base, defaults to general per_rarity_base if missing
        // Primary location (legacy): pricing.per_rarity_base_ores
        ConfigurationSection perRarityOres = section.getConfigurationSection("per_rarity_base_ores");
        if (perRarityOres == null) {
            // New supported location: pricing.overrides.per_rarity_base_ores (placed directly above overrides.Ores in config.yml)
            ConfigurationSection overridesRoot = section.getConfigurationSection("overrides");
            if (overridesRoot != null) {
                perRarityOres = overridesRoot.getConfigurationSection("per_rarity_base_ores");
            }
        }
        if (perRarityOres == null) perRarityOres = section.createSection("per_rarity_base_ores");
        perRarityBaseOres.put(Rarity.BASIC, perRarityOres.getDouble("BASIC", perRarityBase.get(Rarity.BASIC)));
        perRarityBaseOres.put(Rarity.COMMON, perRarityOres.getDouble("COMMON", perRarityBase.get(Rarity.COMMON)));
        perRarityBaseOres.put(Rarity.UNCOMMON, perRarityOres.getDouble("UNCOMMON", perRarityBase.get(Rarity.UNCOMMON)));
        perRarityBaseOres.put(Rarity.RARE, perRarityOres.getDouble("RARE", perRarityBase.get(Rarity.RARE)));
        perRarityBaseOres.put(Rarity.EPIC, perRarityOres.getDouble("EPIC", perRarityBase.get(Rarity.EPIC)));
        perRarityBaseOres.put(Rarity.LEGENDARY, perRarityOres.getDouble("LEGENDARY", perRarityBase.get(Rarity.LEGENDARY)));
        // MYTHIC defaults to LEGENDARY
        perRarityBaseOres.put(Rarity.MYTHIC, perRarityOres.getDouble("MYTHIC", perRarityBase.get(Rarity.LEGENDARY)));
        // Dedicated Ancient Debris base now lives in ores map; default to EPIC from general base if not set
        double ancientDebrisDefaultOres = perRarityBase.getOrDefault(Rarity.EPIC, 500.0);
        perRarityBaseOres.put(Rarity.ANCIENT_DEBRIS, perRarityOres.getDouble("ANCIENT_DEBRIS", ancientDebrisDefaultOres));

        // Ingots-specific per-rarity base, defaults to general per_rarity_base if missing
        // Supported locations (prefer overrides):
        // 1) pricing.overrides.per_rarity_base_ingots (placed directly above overrides.Ingots in config.yml)
        // 2) pricing.per_rarity_base_ingots (legacy/root)
        ConfigurationSection perRarityIngots = null;
        ConfigurationSection overridesRootForIngots = section.getConfigurationSection("overrides");
        if (overridesRootForIngots != null) {
            perRarityIngots = overridesRootForIngots.getConfigurationSection("per_rarity_base_ingots");
        }
        if (perRarityIngots == null) {
            perRarityIngots = section.getConfigurationSection("per_rarity_base_ingots");
        }
        if (perRarityIngots == null) perRarityIngots = (overridesRootForIngots != null)
                ? overridesRootForIngots.createSection("per_rarity_base_ingots")
                : section.createSection("per_rarity_base_ingots");
        perRarityBaseIngots.put(Rarity.BASIC, perRarityIngots.getDouble("BASIC", perRarityBase.get(Rarity.BASIC)));
        perRarityBaseIngots.put(Rarity.COMMON, perRarityIngots.getDouble("COMMON", perRarityBase.get(Rarity.COMMON)));
        perRarityBaseIngots.put(Rarity.UNCOMMON, perRarityIngots.getDouble("UNCOMMON", perRarityBase.get(Rarity.UNCOMMON)));
        perRarityBaseIngots.put(Rarity.RARE, perRarityIngots.getDouble("RARE", perRarityBase.get(Rarity.RARE)));
        perRarityBaseIngots.put(Rarity.EPIC, perRarityIngots.getDouble("EPIC", perRarityBase.get(Rarity.EPIC)));
        perRarityBaseIngots.put(Rarity.LEGENDARY, perRarityIngots.getDouble("LEGENDARY", perRarityBase.get(Rarity.LEGENDARY)));
        perRarityBaseIngots.put(Rarity.MYTHIC, perRarityIngots.getDouble("MYTHIC", perRarityBase.get(Rarity.LEGENDARY)));
        // Dedicated Netherite Ingot base now lives in ingots map; default to general LEGENDARY if missing
        double netheriteIngotDefault = perRarityBase.getOrDefault(Rarity.LEGENDARY, 100000.0);
        perRarityBaseIngots.put(Rarity.NETHERITE_INGOT, perRarityIngots.getDouble("NETHERITE_INGOT", netheriteIngotDefault));

        // Spawners-specific per-rarity base, defaults to general per_rarity_base if missing
        // Primary location (legacy): pricing.per_rarity_base_spawners
        ConfigurationSection perRaritySpawners = section.getConfigurationSection("per_rarity_base_spawners");
        if (perRaritySpawners == null) {
            // New supported location: pricing.overrides.per_rarity_base_spawners (placed directly above overrides.Spawners in config.yml)
            ConfigurationSection overridesRoot = section.getConfigurationSection("overrides");
            if (overridesRoot != null) {
                perRaritySpawners = overridesRoot.getConfigurationSection("per_rarity_base_spawners");
            }
        }
        if (perRaritySpawners == null) perRaritySpawners = section.createSection("per_rarity_base_spawners");
        perRarityBaseSpawners.put(Rarity.BASIC, perRaritySpawners.getDouble("BASIC", perRarityBase.get(Rarity.BASIC)));
        perRarityBaseSpawners.put(Rarity.COMMON, perRaritySpawners.getDouble("COMMON", perRarityBase.get(Rarity.COMMON)));
        perRarityBaseSpawners.put(Rarity.UNCOMMON, perRaritySpawners.getDouble("UNCOMMON", perRarityBase.get(Rarity.UNCOMMON)));
        perRarityBaseSpawners.put(Rarity.RARE, perRaritySpawners.getDouble("RARE", perRarityBase.get(Rarity.RARE)));
        perRarityBaseSpawners.put(Rarity.EPIC, perRaritySpawners.getDouble("EPIC", perRarityBase.get(Rarity.EPIC)));
        perRarityBaseSpawners.put(Rarity.LEGENDARY, perRaritySpawners.getDouble("LEGENDARY", perRarityBase.get(Rarity.LEGENDARY)));
        // MYTHIC defaults to LEGENDARY
        perRarityBaseSpawners.put(Rarity.MYTHIC, perRaritySpawners.getDouble("MYTHIC", perRarityBase.get(Rarity.LEGENDARY)));

        // Spawn eggs-specific per-rarity base, defaults to general per_rarity_base if missing
        // Primary location (legacy): pricing.per_rarity_base_spawn_eggs
        ConfigurationSection perRaritySpawnEggs = section.getConfigurationSection("per_rarity_base_spawn_eggs");
        if (perRaritySpawnEggs == null) {
            // New supported location: pricing.overrides.per_rarity_base_spawn_eggs (placed directly above overrides.SpawnEggs in config.yml)
            ConfigurationSection overridesRoot = section.getConfigurationSection("overrides");
            if (overridesRoot != null) {
                perRaritySpawnEggs = overridesRoot.getConfigurationSection("per_rarity_base_spawn_eggs");
            }
        }
        if (perRaritySpawnEggs == null) perRaritySpawnEggs = section.createSection("per_rarity_base_spawn_eggs");
        perRarityBaseSpawnEggs.put(Rarity.BASIC, perRaritySpawnEggs.getDouble("BASIC", perRarityBase.get(Rarity.BASIC)));
        perRarityBaseSpawnEggs.put(Rarity.COMMON, perRaritySpawnEggs.getDouble("COMMON", perRarityBase.get(Rarity.COMMON)));
        perRarityBaseSpawnEggs.put(Rarity.UNCOMMON, perRaritySpawnEggs.getDouble("UNCOMMON", perRarityBase.get(Rarity.UNCOMMON)));
        perRarityBaseSpawnEggs.put(Rarity.RARE, perRaritySpawnEggs.getDouble("RARE", perRarityBase.get(Rarity.RARE)));
        perRarityBaseSpawnEggs.put(Rarity.EPIC, perRaritySpawnEggs.getDouble("EPIC", perRarityBase.get(Rarity.EPIC)));
        perRarityBaseSpawnEggs.put(Rarity.LEGENDARY, perRaritySpawnEggs.getDouble("LEGENDARY", perRarityBase.get(Rarity.LEGENDARY)));
        // MYTHIC defaults to LEGENDARY
        perRarityBaseSpawnEggs.put(Rarity.MYTHIC, perRaritySpawnEggs.getDouble("MYTHIC", perRarityBase.get(Rarity.LEGENDARY)));

        // Mob heads-specific per-rarity base. Primary location: pricing.per_rarity_base_mob_heads
        ConfigurationSection perRarityMobHeads = section.getConfigurationSection("per_rarity_base_mob_heads");
        if (perRarityMobHeads == null) {
            // Supported location: pricing.overrides.per_rarity_base_mob_heads (placed directly above overrides.MobHeads)
            ConfigurationSection overridesRoot = section.getConfigurationSection("overrides");
            if (overridesRoot != null) {
                perRarityMobHeads = overridesRoot.getConfigurationSection("per_rarity_base_mob_heads");
            }
        }
        if (perRarityMobHeads == null) perRarityMobHeads = section.createSection("per_rarity_base_mob_heads");
        perRarityBaseMobHeads.put(Rarity.BASIC, perRarityMobHeads.getDouble("BASIC", perRarityBase.get(Rarity.BASIC)));
        perRarityBaseMobHeads.put(Rarity.COMMON, perRarityMobHeads.getDouble("COMMON", perRarityBase.get(Rarity.COMMON)));
        perRarityBaseMobHeads.put(Rarity.UNCOMMON, perRarityMobHeads.getDouble("UNCOMMON", perRarityBase.get(Rarity.UNCOMMON)));
        perRarityBaseMobHeads.put(Rarity.RARE, perRarityMobHeads.getDouble("RARE", perRarityBase.get(Rarity.RARE)));
        perRarityBaseMobHeads.put(Rarity.EPIC, perRarityMobHeads.getDouble("EPIC", perRarityBase.get(Rarity.EPIC)));
        perRarityBaseMobHeads.put(Rarity.LEGENDARY, perRarityMobHeads.getDouble("LEGENDARY", perRarityBase.get(Rarity.LEGENDARY)));
        perRarityBaseMobHeads.put(Rarity.MYTHIC, perRarityMobHeads.getDouble("MYTHIC", perRarityBase.get(Rarity.LEGENDARY)));

        // Saplings and seeds-specific per-rarity base (BASIC..RARE)
        ConfigurationSection overridesRootForSaplings = section.getConfigurationSection("overrides");
        ConfigurationSection craftablesRoot = overridesRootForSaplings != null ? overridesRootForSaplings.getConfigurationSection("Craftables") : null;
        ConfigurationSection perRaritySaplingsSeeds = null;
        if (craftablesRoot != null) {
            perRaritySaplingsSeeds = craftablesRoot.getConfigurationSection("per_rarity_base_saplings_seeds");
            if (perRaritySaplingsSeeds == null) {
                // accept alias with "and"
                perRaritySaplingsSeeds = craftablesRoot.getConfigurationSection("per_rarity_base_saplings_and_seeds");
            }
        }
        if (perRaritySaplingsSeeds == null) {
            // create under overrides.Craftables for persistence with sensible defaults
            if (craftablesRoot == null) {
                overridesRootForSaplings = overridesRootForSaplings == null ? section.createSection("overrides") : overridesRootForSaplings;
                craftablesRoot = overridesRootForSaplings.getConfigurationSection("Craftables");
                if (craftablesRoot == null) craftablesRoot = overridesRootForSaplings.createSection("Craftables");
            }
            perRaritySaplingsSeeds = craftablesRoot.createSection("per_rarity_base_saplings_seeds");
        }
        perRarityBaseSaplingsSeeds.put(Rarity.BASIC, perRaritySaplingsSeeds.getDouble("BASIC", perRarityBase.get(Rarity.BASIC)));
        perRarityBaseSaplingsSeeds.put(Rarity.COMMON, perRaritySaplingsSeeds.getDouble("COMMON", perRarityBase.get(Rarity.COMMON)));
        perRarityBaseSaplingsSeeds.put(Rarity.UNCOMMON, perRaritySaplingsSeeds.getDouble("UNCOMMON", perRarityBase.get(Rarity.UNCOMMON)));
        perRarityBaseSaplingsSeeds.put(Rarity.RARE, perRaritySaplingsSeeds.getDouble("RARE", perRarityBase.get(Rarity.RARE)));

        // Froglights-specific per-rarity base (BASIC only). Located at pricing.overrides.Craftables.per_rarity_base_froglights
        ConfigurationSection perRarityFroglights = null;
        {
            ConfigurationSection overridesRoot = section.getConfigurationSection("overrides");
            if (overridesRoot != null) {
                ConfigurationSection craftables = overridesRoot.getConfigurationSection("Craftables");
                if (craftables != null) {
                    perRarityFroglights = craftables.getConfigurationSection("per_rarity_base_froglights");
                }
            }
            if (perRarityFroglights == null) {
                // Create the section for convenience and to persist defaults
                ConfigurationSection overridesRootOut = section.getConfigurationSection("overrides");
                if (overridesRootOut == null) overridesRootOut = section.createSection("overrides");
                ConfigurationSection craftablesOut = overridesRootOut.getConfigurationSection("Craftables");
                if (craftablesOut == null) craftablesOut = overridesRootOut.createSection("Craftables");
                perRarityFroglights = craftablesOut.createSection("per_rarity_base_froglights");
            }
        }
        // Only BASIC is considered for Froglights; other rarities are intentionally omitted
        perRarityBaseFroglights.put(Rarity.BASIC, perRarityFroglights.getDouble("BASIC", perRarityBase.get(Rarity.BASIC)));

        // Ore drops-specific per-rarity base. Primary: pricing.per_rarity_base_ore_drops
        // Additional supported locations for convenience:
        // - pricing.overrides.per_rarity_base_ore_drops
        // - pricing.overrides.Ores.per_rarity_base_ore_drops (directly above the Common ore drops list)
        ConfigurationSection perRarityOreDrops = section.getConfigurationSection("per_rarity_base_ore_drops");
        if (perRarityOreDrops == null) {
            ConfigurationSection overridesRoot = section.getConfigurationSection("overrides");
            if (overridesRoot != null) {
                perRarityOreDrops = overridesRoot.getConfigurationSection("per_rarity_base_ore_drops");
                if (perRarityOreDrops == null) {
                    ConfigurationSection oresRoot = overridesRoot.getConfigurationSection("Ores");
                    if (oresRoot != null) {
                        perRarityOreDrops = oresRoot.getConfigurationSection("per_rarity_base_ore_drops");
                    }
                }
            }
        }
        if (perRarityOreDrops == null) perRarityOreDrops = section.createSection("per_rarity_base_ore_drops");
        // Defaults: mirror per_rarity_base_ores for BASIC..RARE, higher tiers fall back to general base
        perRarityBaseOreDrops.put(Rarity.BASIC, perRarityOreDrops.getDouble("BASIC", perRarityBaseOres.getOrDefault(Rarity.BASIC, perRarityBase.get(Rarity.BASIC))));
        perRarityBaseOreDrops.put(Rarity.COMMON, perRarityOreDrops.getDouble("COMMON", perRarityBaseOres.getOrDefault(Rarity.COMMON, perRarityBase.get(Rarity.COMMON))));
        perRarityBaseOreDrops.put(Rarity.UNCOMMON, perRarityOreDrops.getDouble("UNCOMMON", perRarityBaseOres.getOrDefault(Rarity.UNCOMMON, perRarityBase.get(Rarity.UNCOMMON))));
        perRarityBaseOreDrops.put(Rarity.RARE, perRarityOreDrops.getDouble("RARE", perRarityBaseOres.getOrDefault(Rarity.RARE, perRarityBase.get(Rarity.RARE))));
        perRarityBaseOreDrops.put(Rarity.EPIC, perRarityBase.get(Rarity.EPIC));
        perRarityBaseOreDrops.put(Rarity.LEGENDARY, perRarityBase.get(Rarity.LEGENDARY));
        perRarityBaseOreDrops.put(Rarity.MYTHIC, perRarityBase.get(Rarity.LEGENDARY));
        // Dedicated tier for Netherite Scrap within ore drops base map; falls back to general NETHERITE_SCRAP (or EPIC)
        double nsDefault = perRarityBase.getOrDefault(Rarity.NETHERITE_SCRAP, perRarityBase.get(Rarity.EPIC));
        perRarityBaseOreDrops.put(Rarity.NETHERITE_SCRAP, perRarityOreDrops.getDouble("NETHERITE_SCRAP", nsDefault));

        // Enchantment books-specific per-rarity base (BASIC..RARE). Primary: pricing.per_rarity_base_enchantment_books
        ConfigurationSection perRarityEnchBooks = section.getConfigurationSection("per_rarity_base_enchantment_books");
        if (perRarityEnchBooks == null) {
            // Optional supported location: pricing.overrides.per_rarity_base_enchantment_books
            ConfigurationSection overridesRoot = section.getConfigurationSection("overrides");
            if (overridesRoot != null) {
                perRarityEnchBooks = overridesRoot.getConfigurationSection("per_rarity_base_enchantment_books");
            }
        }
        if (perRarityEnchBooks == null) perRarityEnchBooks = section.createSection("per_rarity_base_enchantment_books");
        perRarityBaseEnchantmentBooks.put(Rarity.BASIC, perRarityEnchBooks.getDouble("BASIC", perRarityBase.get(Rarity.BASIC)));
        perRarityBaseEnchantmentBooks.put(Rarity.COMMON, perRarityEnchBooks.getDouble("COMMON", perRarityBase.get(Rarity.COMMON)));
        perRarityBaseEnchantmentBooks.put(Rarity.UNCOMMON, perRarityEnchBooks.getDouble("UNCOMMON", perRarityBase.get(Rarity.UNCOMMON)));
        perRarityBaseEnchantmentBooks.put(Rarity.RARE, perRarityEnchBooks.getDouble("RARE", perRarityBase.get(Rarity.RARE)));

        overridesSection = section.getConfigurationSection("overrides");
        // Parse rarity overrides from overrides.Blocks and Craftables
        rarityOverrides.clear();
        spawnerRarityOverrides.clear();
        enchantmentRarityOverrides.clear();
        // Also honor top-level pricing.Blocks list for block rarity mapping (e.g., Overworld/Nature blocks)
        {
            ConfigurationSection blocksTopLevel = section.getConfigurationSection("Blocks");
            if (blocksTopLevel != null) {
                for (String key : blocksTopLevel.getKeys(false)) {
                    if (key == null) continue;
                    String materialKey = key.trim().toUpperCase(Locale.ROOT);
                    Material mat = Material.matchMaterial(materialKey);
                    if (mat == null) continue; // skip unknown materials
                    String rarityStr = blocksTopLevel.getString(key);
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
        }
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
            // New: parse overrides.ResourceBlocks (storage/resource blocks like GOLD_BLOCK, IRON_BLOCK, etc.)
            ConfigurationSection resourceBlocks = overridesSection.getConfigurationSection("ResourceBlocks");
            if (resourceBlocks != null) {
                for (String key : resourceBlocks.getKeys(false)) {
                    if (key == null) continue;
                    String materialKey = key.trim().toUpperCase(Locale.ROOT);
                    Material mat = Material.matchMaterial(materialKey);
                    if (mat == null) continue; // skip unknown materials
                    String rarityStr = resourceBlocks.getString(key);
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
            // New: parse overrides.Ingots (IRON_INGOT, GOLD_INGOT, COPPER_INGOT, NETHERITE_INGOT)
            ConfigurationSection ingots = overridesSection.getConfigurationSection("Ingots");
            if (ingots != null) {
                for (String key : ingots.getKeys(false)) {
                    if (key == null) continue;
                    String materialKey = key.trim().toUpperCase(Locale.ROOT);
                    Material mat = Material.matchMaterial(materialKey);
                    if (mat == null) continue; // skip unknown materials
                    String rarityStr = ingots.getString(key);
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
            // New: parse overrides.MobHeads (e.g., ZOMBIE_HEAD, CREEPER_HEAD, DRAGON_HEAD, etc.)
            ConfigurationSection mobHeads = overridesSection.getConfigurationSection("MobHeads");
            if (mobHeads != null) {
                for (String key : mobHeads.getKeys(false)) {
                    if (key == null) continue;
                    String materialKey = key.trim().toUpperCase(Locale.ROOT);
                    Material mat = Material.matchMaterial(materialKey);
                    if (mat == null) continue; // skip unknown materials
                    String rarityStr = mobHeads.getString(key);
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

        // Persist defaults back under new key pricing.per_rarity_base_craftables
        ConfigurationSection perRarityOut = section.getConfigurationSection("per_rarity_base_craftables");
        if (perRarityOut == null) perRarityOut = section.createSection("per_rarity_base_craftables");
        perRarityOut.set("BASIC", perRarityBase.get(Rarity.BASIC));
        // Also persist alias key for convenience
        perRarityOut.set("BASE", perRarityBase.get(Rarity.BASIC));
        perRarityOut.set("COMMON", perRarityBase.get(Rarity.COMMON));
        perRarityOut.set("UNCOMMON", perRarityBase.get(Rarity.UNCOMMON));
        perRarityOut.set("RARE", perRarityBase.get(Rarity.RARE));
        perRarityOut.set("EPIC", perRarityBase.get(Rarity.EPIC));
        // Do not persist NETHERITE_BLOCK under general craftables; it lives under resource blocks now
        perRarityOut.set("NETHERITE_SCRAP", perRarityBase.get(Rarity.NETHERITE_SCRAP));
        // Do not persist NETHERITE_INGOT under general craftables; it lives under ingots now
        perRarityOut.set("LEGENDARY", perRarityBase.get(Rarity.LEGENDARY));
        perRarityOut.set("ELYTRA", perRarityBase.get(Rarity.ELYTRA));
        perRarityOut.set("ANCIENT_DEBRIS", perRarityBase.get(Rarity.ANCIENT_DEBRIS));
        // If user had the misspelled key, clean it up
        if (perRarity.isSet("LEGANDERY")) perRarity.set("LEGANDERY", null);

        // Save armor-specific per-rarity base as well
        ConfigurationSection perRarityArmorOut = section.getConfigurationSection("per_rarity_base_armor");
        if (perRarityArmorOut == null) perRarityArmorOut = section.createSection("per_rarity_base_armor");
        perRarityArmorOut.set("BASIC", perRarityBaseArmor.get(Rarity.BASIC));
        perRarityArmorOut.set("COMMON", perRarityBaseArmor.get(Rarity.COMMON));
        perRarityArmorOut.set("UNCOMMON", perRarityBaseArmor.get(Rarity.UNCOMMON));
        perRarityArmorOut.set("RARE", perRarityBaseArmor.get(Rarity.RARE));
        perRarityArmorOut.set("EPIC", perRarityBaseArmor.get(Rarity.EPIC));
        perRarityArmorOut.set("LEGENDARY", perRarityBaseArmor.get(Rarity.LEGENDARY));
        perRarityArmorOut.set("MYTHIC", perRarityBaseArmor.get(Rarity.MYTHIC));

        // Save blocks-specific per-rarity base as well
        ConfigurationSection perRarityBlocksOut = section.getConfigurationSection("per_rarity_base_blocks");
        if (perRarityBlocksOut == null) perRarityBlocksOut = section.createSection("per_rarity_base_blocks");
        perRarityBlocksOut.set("BASIC", perRarityBaseBlocks.get(Rarity.BASIC));
        perRarityBlocksOut.set("COMMON", perRarityBaseBlocks.get(Rarity.COMMON));
        perRarityBlocksOut.set("UNCOMMON", perRarityBaseBlocks.get(Rarity.UNCOMMON));
        perRarityBlocksOut.set("RARE", perRarityBaseBlocks.get(Rarity.RARE));
        // Do not persist EPIC, LEGENDARY, or MYTHIC under blocks-specific section; these use general per_rarity_base

        // Save ores-specific per-rarity base as well
        ConfigurationSection perRarityOresOut = section.getConfigurationSection("per_rarity_base_ores");
        if (perRarityOresOut == null) perRarityOresOut = section.createSection("per_rarity_base_ores");
        perRarityOresOut.set("BASIC", perRarityBaseOres.get(Rarity.BASIC));
        perRarityOresOut.set("COMMON", perRarityBaseOres.get(Rarity.COMMON));
        perRarityOresOut.set("UNCOMMON", perRarityBaseOres.get(Rarity.UNCOMMON));
        perRarityOresOut.set("RARE", perRarityBaseOres.get(Rarity.RARE));
        perRarityOresOut.set("EPIC", perRarityBaseOres.get(Rarity.EPIC));
        perRarityOresOut.set("LEGENDARY", perRarityBaseOres.get(Rarity.LEGENDARY));
        perRarityOresOut.set("MYTHIC", perRarityBaseOres.get(Rarity.MYTHIC));
        perRarityOresOut.set("ANCIENT_DEBRIS", perRarityBaseOres.get(Rarity.ANCIENT_DEBRIS));

        // Save ingots-specific per-rarity base as well (under root and overrides for persistence)
        ConfigurationSection perRarityIngotsOut = section.getConfigurationSection("per_rarity_base_ingots");
        if (perRarityIngotsOut == null) perRarityIngotsOut = section.createSection("per_rarity_base_ingots");
        perRarityIngotsOut.set("BASIC", perRarityBaseIngots.get(Rarity.BASIC));
        perRarityIngotsOut.set("COMMON", perRarityBaseIngots.get(Rarity.COMMON));
        perRarityIngotsOut.set("UNCOMMON", perRarityBaseIngots.get(Rarity.UNCOMMON));
        perRarityIngotsOut.set("RARE", perRarityBaseIngots.get(Rarity.RARE));
        perRarityIngotsOut.set("EPIC", perRarityBaseIngots.get(Rarity.EPIC));
        perRarityIngotsOut.set("LEGENDARY", perRarityBaseIngots.get(Rarity.LEGENDARY));
        perRarityIngotsOut.set("MYTHIC", perRarityBaseIngots.get(Rarity.MYTHIC));
        perRarityIngotsOut.set("NETHERITE_INGOT", perRarityBaseIngots.get(Rarity.NETHERITE_INGOT));

        // Save spawners-specific per-rarity base as well
        ConfigurationSection perRaritySpawnersOut = section.getConfigurationSection("per_rarity_base_spawners");
        if (perRaritySpawnersOut == null) perRaritySpawnersOut = section.createSection("per_rarity_base_spawners");
        perRaritySpawnersOut.set("BASIC", perRarityBaseSpawners.get(Rarity.BASIC));
        perRaritySpawnersOut.set("COMMON", perRarityBaseSpawners.get(Rarity.COMMON));
        perRaritySpawnersOut.set("UNCOMMON", perRarityBaseSpawners.get(Rarity.UNCOMMON));
        perRaritySpawnersOut.set("RARE", perRarityBaseSpawners.get(Rarity.RARE));
        perRaritySpawnersOut.set("EPIC", perRarityBaseSpawners.get(Rarity.EPIC));
        perRaritySpawnersOut.set("LEGENDARY", perRarityBaseSpawners.get(Rarity.LEGENDARY));
        perRaritySpawnersOut.set("MYTHIC", perRarityBaseSpawners.get(Rarity.MYTHIC));

        // Save spawn eggs-specific per-rarity base as well
        ConfigurationSection perRaritySpawnEggsOut = section.getConfigurationSection("per_rarity_base_spawn_eggs");
        if (perRaritySpawnEggsOut == null) perRaritySpawnEggsOut = section.createSection("per_rarity_base_spawn_eggs");
        perRaritySpawnEggsOut.set("BASIC", perRarityBaseSpawnEggs.get(Rarity.BASIC));
        perRaritySpawnEggsOut.set("COMMON", perRarityBaseSpawnEggs.get(Rarity.COMMON));
        perRaritySpawnEggsOut.set("UNCOMMON", perRarityBaseSpawnEggs.get(Rarity.UNCOMMON));
        perRaritySpawnEggsOut.set("RARE", perRarityBaseSpawnEggs.get(Rarity.RARE));
        perRaritySpawnEggsOut.set("EPIC", perRarityBaseSpawnEggs.get(Rarity.EPIC));
        perRaritySpawnEggsOut.set("LEGENDARY", perRarityBaseSpawnEggs.get(Rarity.LEGENDARY));
        perRaritySpawnEggsOut.set("MYTHIC", perRarityBaseSpawnEggs.get(Rarity.MYTHIC));

        // Save enchantment books per-rarity base as well
        ConfigurationSection perRarityEnchBooksOut = section.getConfigurationSection("per_rarity_base_enchantment_books");
        if (perRarityEnchBooksOut == null) perRarityEnchBooksOut = section.createSection("per_rarity_base_enchantment_books");
        perRarityEnchBooksOut.set("BASIC", perRarityBaseEnchantmentBooks.get(Rarity.BASIC));
        perRarityEnchBooksOut.set("COMMON", perRarityBaseEnchantmentBooks.get(Rarity.COMMON));
        perRarityEnchBooksOut.set("UNCOMMON", perRarityBaseEnchantmentBooks.get(Rarity.UNCOMMON));
        perRarityEnchBooksOut.set("RARE", perRarityBaseEnchantmentBooks.get(Rarity.RARE));

        // Save mob heads-specific per-rarity base as well
        ConfigurationSection perRarityMobHeadsOut = section.getConfigurationSection("per_rarity_base_mob_heads");
        if (perRarityMobHeadsOut == null) perRarityMobHeadsOut = section.createSection("per_rarity_base_mob_heads");
        perRarityMobHeadsOut.set("BASIC", perRarityBaseMobHeads.get(Rarity.BASIC));
        perRarityMobHeadsOut.set("COMMON", perRarityBaseMobHeads.get(Rarity.COMMON));
        perRarityMobHeadsOut.set("UNCOMMON", perRarityBaseMobHeads.get(Rarity.UNCOMMON));
        perRarityMobHeadsOut.set("RARE", perRarityBaseMobHeads.get(Rarity.RARE));
        perRarityMobHeadsOut.set("EPIC", perRarityBaseMobHeads.get(Rarity.EPIC));
        perRarityMobHeadsOut.set("LEGENDARY", perRarityBaseMobHeads.get(Rarity.LEGENDARY));
        perRarityMobHeadsOut.set("MYTHIC", perRarityBaseMobHeads.get(Rarity.MYTHIC));

        // Persist defaults back for resource blocks base
        ConfigurationSection perRarityResourceBlocksOut = section.getConfigurationSection("per_rarity_base_resource_blocks");
        if (perRarityResourceBlocksOut == null) perRarityResourceBlocksOut = section.createSection("per_rarity_base_resource_blocks");
        perRarityResourceBlocksOut.set("BASIC", perRarityBaseResourceBlocks.get(Rarity.BASIC));
        perRarityResourceBlocksOut.set("COMMON", perRarityBaseResourceBlocks.get(Rarity.COMMON));
        perRarityResourceBlocksOut.set("UNCOMMON", perRarityBaseResourceBlocks.get(Rarity.UNCOMMON));
        perRarityResourceBlocksOut.set("RARE", perRarityBaseResourceBlocks.get(Rarity.RARE));
        perRarityResourceBlocksOut.set("NETHERITE_BLOCK", perRarityBaseResourceBlocks.get(Rarity.NETHERITE_BLOCK));

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
            // Use spawners-specific base map with fallback to general per_rarity_base
            Double val = perRarityBaseSpawners.get(r);
            if (val == null) val = perRarityBase.get(r);
            perUnit = val != null ? val : 5.0;
        } else if (mat == Material.ENCHANTED_BOOK) {
            // Enchanted book: determine rarity based on stored enchantments using overrides.EnchantmentBooks
            Rarity chosen = Rarity.COMMON;
            double best = perRarityBaseEnchantmentBooks.getOrDefault(chosen, perRarityBase.getOrDefault(chosen, 5.0));
            try {
                if (item.hasItemMeta() && item.getItemMeta() instanceof EnchantmentStorageMeta) {
                    EnchantmentStorageMeta meta = (EnchantmentStorageMeta) item.getItemMeta();
                    Map<Enchantment, Integer> enchants = meta.getStoredEnchants();
                    if (enchants != null && !enchants.isEmpty()) {
                        for (Enchantment ench : enchants.keySet()) {
                            Rarity er = enchantmentRarityOverrides.get(ench);
                            if (er == null) er = Rarity.COMMON; // fallback if not specified
                            double val = perRarityBaseEnchantmentBooks.getOrDefault(er, perRarityBase.getOrDefault(er, 5.0));
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
            perUnit = perRarityBaseEnchantmentBooks.getOrDefault(chosen, perRarityBase.getOrDefault(chosen, 5.0));
        } else {
            // Next, check rarity override from blocks/craftables category
            Rarity rOverride = rarityOverrides.get(mat);
            Rarity r = (rOverride != null) ? rOverride : inferRarity(mat);
            Map<Rarity, Double> baseMap;
            if (isArmor(mat)) {
                baseMap = perRarityBaseArmor;
            } else if (isOreBlock(mat)) {
                baseMap = perRarityBaseOres;
            } else if (isOreDrop(mat)) {
                baseMap = perRarityBaseOreDrops;
            } else if (isIngot(mat)) {
                baseMap = perRarityBaseIngots;
            } else if (isSpawnEgg(mat)) {
                baseMap = perRarityBaseSpawnEggs;
            } else if (isSaplingOrSeed(mat)) {
                baseMap = perRarityBaseSaplingsSeeds;
            } else if (isFroglight(mat)) {
                baseMap = perRarityBaseFroglights;
            } else if (isResourceBlock(mat)) {
                baseMap = perRarityBaseResourceBlocks;
            } else if (isMobHead(mat)) {
                baseMap = perRarityBaseMobHeads;
            } else if (mat.isBlock()) {
                baseMap = perRarityBaseBlocks;
            } else {
                baseMap = perRarityBase;
            }
            Double val = baseMap.get(r);
            // Prefer the general per_rarity_base for rarities that the chosen base map doesn't define
            if (val == null) {
                val = perRarityBase.get(r);
            }
            // As a last resort for block families with limited tiers, fall back to that family's RARE tier
            if (val == null) {
                if (baseMap == perRarityBaseBlocks) {
                    val = perRarityBaseBlocks.get(Rarity.RARE);
                } else if (baseMap == perRarityBaseResourceBlocks) {
                    val = perRarityBaseResourceBlocks.get(Rarity.RARE);
                }
            }
            perUnit = val != null ? val : 5.0;
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
        // Accept common aliases used in config: "base" should map to BASIC
        if ("BASE".equals(v)) return Rarity.BASIC;
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
        if (mat == Material.ANCIENT_DEBRIS) return Rarity.ANCIENT_DEBRIS; // Dedicated rarity for Ancient Debris
        String name = mat.name();
        // Very lightweight heuristic
        if (containsAny(name, "NETHERITE", "DRAGON", "BEACON", "TOTEM", "MACE")) return Rarity.EPIC;
        if (containsAny(name, "DIAMOND", "ANCIENT_DEBRIS", "NETHER_STAR", "SPONGE", "SHULKER", "OMINOUS_BOTTLE")) return Rarity.RARE;
        if (containsAny(name, "GOLD", "EMERALD", "ENCHANTED", "TRIDENT", "AMETHYST", "WIND_CHARGE", "SCULK_SHRIEKER")) return Rarity.UNCOMMON;
        if (containsAny(name, "IRON", "COPPER", "LAPIS", "REDSTONE", "CRAFTER", "SCULK_SENSOR")) return Rarity.UNCOMMON;
        // Blocks and common resources
        if (containsAny(name, "DIRT", "STONE", "SAND", "GRAVEL", "WOOD", "PLANKS", "SAPLING", "SEEDS", "WOOL", "GLASS", "LEAVES", "TUFF", "MOSS", "TORCH", "LANTERN", "END_ROD")) return Rarity.COMMON;
        return Rarity.COMMON;
    }

    private static boolean containsAny(String name, String... terms) {
        for (String t : terms) if (name.contains(t)) return true;
        return false;
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private static boolean isOreBlock(Material mat) {
        String n = mat.name();
        if (n.endsWith("_ORE")) return true; // covers DEEPSLATE_*_ORE and standard *_ORE
        return mat == Material.ANCIENT_DEBRIS;
    }

    private static boolean isOreDrop(Material mat) {
        switch (mat) {
            case COAL:
            case RAW_IRON:
            case RAW_COPPER:
            case RAW_GOLD:
            case REDSTONE:
            case LAPIS_LAZULI:
            case DIAMOND:
            case EMERALD:
            case QUARTZ:
            case AMETHYST_SHARD:
            case NETHERITE_SCRAP:
                return true;
            default:
                return false;
        }
    }

    private static boolean isIngot(Material mat) {
        switch (mat) {
            case IRON_INGOT:
            case COPPER_INGOT:
            case GOLD_INGOT:
            case NETHERITE_INGOT:
                return true;
            default:
                return false;
        }
    }

    private static boolean isArmor(Material mat) {
        // Treat wearable armor and elytra as armor for base selection
        String n = mat.name();
        if (n.endsWith("_HELMET") || n.endsWith("_CHESTPLATE") || n.endsWith("_LEGGINGS") || n.endsWith("_BOOTS")) return true;
        return mat == Material.TURTLE_HELMET || mat == Material.ELYTRA;
    }

    private static boolean isSpawnEgg(Material mat) {
        // Bukkit names for spawn eggs always end with _SPAWN_EGG
        return mat.name().endsWith("_SPAWN_EGG");
    }

    private static boolean isSaplingOrSeed(Material mat) {
        String n = mat.name();
        if (n.contains("SAPLING") || n.contains("SEEDS") || n.contains("PROPAGULE") || n.contains("FUNGUS")) return true;
        switch (mat) {
            case NETHER_WART:
            case PITCHER_POD:
                return true;
            default:
                return false;
        }
    }

    private static boolean isFroglight(Material mat) {
        switch (mat) {
            case OCHRE_FROGLIGHT:
            case VERDANT_FROGLIGHT:
            case PEARLESCENT_FROGLIGHT:
                return true;
            default:
                return false;
        }
    }

    private static boolean isResourceBlock(Material mat) {
        switch (mat) {
            case GOLD_BLOCK:
            case IRON_BLOCK:
            case REDSTONE_BLOCK:
            case LAPIS_BLOCK:
            case EMERALD_BLOCK:
            case DIAMOND_BLOCK:
            case COPPER_BLOCK:
            case COAL_BLOCK:
            case NETHERITE_BLOCK:
                return true;
            default:
                return false;
        }
    }

    private static boolean isMobHead(Material mat) {
        switch (mat) {
            case CREEPER_HEAD:
            case ZOMBIE_HEAD:
            case SKELETON_SKULL:
            case WITHER_SKELETON_SKULL:
            case DRAGON_HEAD:
            case PIGLIN_HEAD:
                return true;
            default:
                return false;
        }
    }
}
