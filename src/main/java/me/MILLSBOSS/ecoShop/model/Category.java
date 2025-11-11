package me.MILLSBOSS.ecoShop.model;

import org.bukkit.Material;

public enum Category {
    BUILDING_BLOCKS("Building Blocks", Material.BRICKS),
    TOOLS("Tools", Material.IRON_PICKAXE),
    WEAPONS("Weapons", Material.IRON_SWORD),
    ARMOR("Armor", Material.IRON_CHESTPLATE),
    FOOD("Food", Material.COOKED_BEEF),
    ORES("Ores", Material.IRON_INGOT),
    PLANTS("Plants", Material.OAK_SAPLING),
    REDSTONE("Redstone", Material.REDSTONE),
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
        // Simple heuristic to categorize based on material name
        String n = mat.name();
        if (n.contains("SWORD") || n.contains("BOW") || n.contains("CROSSBOW") || n.contains("TRIDENT")) return WEAPONS;
        if (n.contains("HELMET") || n.contains("CHESTPLATE") || n.contains("LEGGINGS") || n.contains("BOOTS") || n.contains("SHIELD")) return ARMOR;
        if (n.contains("PICKAXE") || n.contains("AXE") || n.contains("SHOVEL") || n.contains("HOE") || n.contains("SHEARS") || n.contains("FISHING_ROD") || n.contains("FLINT_AND_STEEL")) return TOOLS;
        if (n.contains("APPLE") || n.contains("BEEF") || n.contains("PORK") || n.contains("MUTTON") || n.contains("CHICKEN") || n.contains("RABBIT") || n.contains("COD") || n.contains("SALMON") || n.contains("BREAD") || n.contains("POTATO") || n.contains("CARROT") || n.contains("SUSPICIOUS_STEW") || n.contains("BEETROOT") || n.contains("COOKIE") || n.contains("MELON") || n.contains("PUMPKIN_PIE") || n.contains("CHORUS_FRUIT") || n.contains("HONEY")) return FOOD;
        if (n.endsWith("_ORE") || n.contains("INGOT") || n.contains("NUGGET") || n.contains("RAW_")) return ORES;
        if (n.contains("SAPLING") || n.contains("SEEDS") || n.contains("FLOWER") || n.contains("LEAVES") || n.contains("GRASS") || n.contains("MUSHROOM") || n.contains("VINE") || n.contains("WART") || n.contains("CROP")) return PLANTS;
        if (n.contains("REDSTONE") || n.contains("REPEATER") || n.contains("COMPARATOR") || n.contains("PISTON") || n.contains("OBSERVER") || n.contains("HOPPER") || n.contains("DISPENSER") || n.contains("DROPPER") || n.contains("LEVER") || n.contains("BUTTON") || n.contains("PRESSURE_PLATE") || n.contains("DAYLIGHT") || n.contains("TARGET")) return REDSTONE;
        // Building blocks heuristic
        if (n.contains("BRICK") || n.contains("PLANKS") || n.contains("STAIRS") || n.contains("SLAB") || n.contains("STONE") || n.contains("SANDSTONE") || n.contains("GLASS") || n.contains("CONCRETE") || n.contains("TERRACOTTA") || n.contains("LOG") || n.contains("WOOD") || n.contains("DEEPSLATE") || n.contains("COBBLESTONE") || n.contains("MUD") || n.contains("BRICKS") || n.contains("PRISMARINE") || n.contains("WOOL") || n.contains("NETHERRACK") || n.contains("BLACKSTONE") || n.contains("END_STONE") || n.contains("QUARTZ")) return BUILDING_BLOCKS;
        return MISC;
    }
}
