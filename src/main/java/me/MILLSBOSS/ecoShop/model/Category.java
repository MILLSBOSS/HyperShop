package me.MILLSBOSS.ecoShop.model;

import org.bukkit.Material;

public enum Category {
    BUILDING_BLOCKS("Building Blocks", Material.BRICKS),
    TOOLS("Tools", Material.IRON_PICKAXE),
    WEAPONS("Weapons", Material.IRON_SWORD),
    ARMOR("Armor", Material.IRON_CHESTPLATE),
    FOOD("Food", Material.COOKED_BEEF),
    ORES("Ores", Material.IRON_INGOT),
    SAPLINGS("Saplings", Material.OAK_SAPLING),
    PLANTS("Plants", Material.OAK_SAPLING),
    REDSTONE("Redstone", Material.REDSTONE),
    SPAWNERS("Spawners", Material.SPAWNER),
    ENCHANTMENT_BOOKS("Enchantment Books", Material.ENCHANTED_BOOK),
    MISC("Miscellaneous", Material.CHEST);

    private final String display;
    private final Material icon;

    Category(String display, Material icon) {
        this.display = display;
        this.icon = icon;
    }

    public String getDisplay() {
        return display;
    }

    public Material getIcon() {
        return icon;
    }

    public static Category categorize(Material mat) {
        // Direct mappings for special categories
        if (mat == Material.SPAWNER) return SPAWNERS;
        if (mat == Material.ENCHANTED_BOOK) return ENCHANTMENT_BOOKS;
        // Eggs should appear under Food
        if (mat == Material.EGG) return FOOD;
        // Ensure storage/resource blocks appear under the Ores page
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
                return ORES;
            // Ensure special nether ore and its drop appear under the Ores page
            case ANCIENT_DEBRIS:
            case NETHERITE_SCRAP:
                return ORES;
            default:
                // continue to heuristics below
        }
        // Simple heuristic to categorize based on material name
        String n = mat.name();
        if (n.contains("SWORD") || n.contains("BOW") || n.contains("CROSSBOW") || n.contains("TRIDENT") || n.contains("MACE") || n.contains("WIND_CHARGE") || n.contains("ARROW") || n.contains("SPEAR")) return WEAPONS;
        if (n.contains("HELMET") || n.contains("CHESTPLATE") || n.contains("LEGGINGS") || n.contains("BOOTS") || n.contains("SHIELD")) return ARMOR;
        if (n.contains("PICKAXE") || n.contains("AXE") || n.contains("SHOVEL") || n.contains("HOE") || n.contains("SHEARS") || n.contains("FISHING_ROD") || n.contains("FLINT_AND_STEEL") || n.contains("BRUSH") || n.contains("COMPASS") || n.equals("CLOCK") || n.equals("WRITABLE_BOOK") || n.equals("SPYGLASS") || n.contains("FROGLIGHT") || n.equals("END_ROD") || n.contains("TORCH") || n.contains("LANTERN")) return TOOLS;
        // Saplings and seeds dedicated category (check before generic PLANTS)
        if (n.contains("SAPLING") || n.contains("SEEDS") || n.contains("PROPAGULE") || n.contains("FUNGUS") || n.equals("PITCHER_POD")) return SAPLINGS;
        if (n.contains("APPLE") || n.contains("BEEF") || n.contains("PORK") || n.contains("MUTTON") || n.contains("CHICKEN") || n.contains("RABBIT") || n.contains("COD") || n.contains("SALMON") || n.contains("BREAD") || n.contains("POTATO") || n.contains("CARROT") || n.contains("SUSPICIOUS_STEW") || n.contains("BEETROOT") || n.contains("COOKIE") || n.contains("MELON") || n.contains("PUMPKIN_PIE") || n.contains("CHORUS_FRUIT") || n.contains("HONEY") || n.equals("WHEAT") || n.contains("BERRIES") || n.contains("STEW") || n.contains("SOUP") || n.contains("CAKE") || n.contains("PUFFERFISH") || n.contains("TROPICAL_FISH") || n.contains("KELP") || n.contains("MILK_BUCKET") || n.contains("OMINOUS_BOTTLE") || n.equals("SUGAR")) return FOOD;
        if (n.endsWith("_ORE") || n.contains("INGOT") || n.contains("NUGGET") || n.contains("RAW_")) return ORES;
        if (n.contains("SAPLING") || n.contains("SEEDS") || n.contains("FLOWER") || n.contains("GRASS") || n.contains("MUSHROOM") || n.contains("VINE") || n.contains("WART") || n.contains("CROP") || n.contains("CORAL")) return PLANTS;
        if (n.contains("REDSTONE") || n.contains("REPEATER") || n.contains("COMPARATOR") || n.contains("PISTON") || n.contains("OBSERVER") || n.contains("HOPPER") || n.contains("DISPENSER") || n.contains("DROPPER") || n.contains("LEVER") || n.contains("BUTTON") || n.contains("PRESSURE_PLATE") || n.contains("DAYLIGHT") || n.contains("TARGET") || n.contains("RAIL") || n.contains("MINECART") || n.equals("TNT") || n.contains("SCULK_SENSOR") || n.contains("SCULK_SHRIEKER") || n.contains("TRIPWIRE_HOOK") || n.contains("CRAFTER") || n.equals("SLIME_BLOCK") || n.equals("HONEY_BLOCK")) return REDSTONE;
        // Miscellaneous items
        if (n.contains("SHULKER")) return MISC;
        // Building blocks heuristic
        if (n.contains("BRICK") || n.contains("PLANKS") || n.contains("STAIRS") || n.contains("SLAB") || n.contains("STONE") || n.contains("SANDSTONE") || n.contains("GLASS") || n.contains("PANE") || n.contains("CONCRETE") || n.contains("TERRACOTTA") || n.contains("LOG") || n.contains("WOOD") || n.contains("DEEPSLATE") || n.contains("COBBLESTONE") || n.contains("MUD") || n.contains("BRICKS") || n.contains("PRISMARINE") || n.contains("WOOL") || n.contains("CARPET") || n.contains("NETHERRACK") || n.contains("BLACKSTONE") || n.contains("END_STONE") || n.contains("QUARTZ") || n.contains("TUFF") || n.contains("PURPUR") || n.contains("BASALT") || n.contains("CALCITE") || n.contains("DRIPSTONE") || n.contains("AMETHYST") || n.contains("OBSIDIAN") || n.contains("INFESTED") || n.contains("WALL") || n.contains("FENCE") || n.contains("CHISELED") || n.contains("POLISHED") || n.contains("SMOOTH") || n.contains("COPPER") || n.contains("DIRT") || n.contains("GRASS_BLOCK") || n.contains("GRAVEL") || n.contains("SAND") || n.contains("CLAY") || n.contains("ICE") || n.contains("SNOW_BLOCK") || n.contains("PODZOL") || n.contains("MYCELIUM") || n.contains("MUSHROOM_BLOCK") || n.contains("BULB") || n.contains("GRATE") || n.contains("CHAIN") || n.contains("BARS") || n.contains("WAXED") || n.contains("EXPOSED") || n.contains("WEATHERED") || n.contains("OXIDIZED") || n.contains("MOSS") || n.contains("LEAVES")) return BUILDING_BLOCKS;
        return MISC;
    }
}
