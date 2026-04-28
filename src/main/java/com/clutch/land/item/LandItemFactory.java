package com.clutch.land.item;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public class LandItemFactory {
    private final NamespacedKey itemKey;

    private final Material toolMaterial;
    private final String toolName;
    private final int toolCustomModelData;

    private final Material claimTicketMaterial;
    private final String claimTicketName;
    private final int claimTicketCustomModelData;

    public LandItemFactory(JavaPlugin plugin) {
        this.itemKey = new NamespacedKey(plugin, "clutchland_item_type");

        FileConfiguration config = plugin.getConfig();

        this.toolMaterial = parseMaterial(
            pick(config, "items.land_tool.material", "items.tool.material"),
            Material.GOLDEN_AXE
        );
        this.toolName = color(pick(config, "items.land_tool.name", "items.tool.name", "&6토지 도구"));
        this.toolCustomModelData = pickInt(config, "items.land_tool.custom-model-data", "items.tool.custom-model-data", 10001);

        this.claimTicketMaterial = parseMaterial(config.getString("items.claim-ticket.material"), Material.PAPER);
        this.claimTicketName = color(config.getString("items.claim-ticket.name", "&b땅 구매권"));
        this.claimTicketCustomModelData = config.getInt("items.claim-ticket.custom-model-data", 10002);
    }

    public ItemStack createTool() {
        ItemStack item = new ItemStack(toolMaterial);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.setDisplayName(toolName);
        meta.setCustomModelData(toolCustomModelData);
        meta.getPersistentDataContainer().set(itemKey, PersistentDataType.STRING, "tool");
        item.setItemMeta(meta);
        return item;
    }

    public void giveLandTool(Player player) {
        if (player == null) {
            return;
        }
        player.getInventory().addItem(createTool());
    }

    public ItemStack createClaimTicket() {
        ItemStack item = new ItemStack(claimTicketMaterial);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.setDisplayName(claimTicketName);
        meta.setCustomModelData(claimTicketCustomModelData);
        meta.getPersistentDataContainer().set(itemKey, PersistentDataType.STRING, "ticket");
        item.setItemMeta(meta);
        return item;
    }

    public boolean isTool(ItemStack item) {
        return isType(item, "tool", toolMaterial, toolCustomModelData);
    }

    public boolean isLandTool(ItemStack item) {
        return isTool(item);
    }

    public boolean isClaimTicket(ItemStack item) {
        return isType(item, "ticket", claimTicketMaterial, claimTicketCustomModelData);
    }

    private boolean isType(ItemStack item, String type, Material expectedMaterial, int expectedModelData) {
        if (item == null || item.getType() == Material.AIR || item.getType() != expectedMaterial) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasCustomModelData() || meta.getCustomModelData() != expectedModelData) {
            return false;
        }
        String value = meta.getPersistentDataContainer().get(itemKey, PersistentDataType.STRING);
        return type.equals(value);
    }

    private Material parseMaterial(String value, Material fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        Material material = Material.matchMaterial(value.trim().toUpperCase());
        return material == null ? fallback : material;
    }

    private String color(String raw) {
        return ChatColor.translateAlternateColorCodes('&', raw == null ? "" : raw);
    }

    private String pick(FileConfiguration config, String primaryPath, String legacyPath) {
        return pick(config, primaryPath, legacyPath, null);
    }

    private String pick(FileConfiguration config, String primaryPath, String legacyPath, String fallback) {
        if (config.isString(primaryPath)) {
            return config.getString(primaryPath);
        }
        if (config.isString(legacyPath)) {
            return config.getString(legacyPath);
        }
        return fallback;
    }

    private int pickInt(FileConfiguration config, String primaryPath, String legacyPath, int fallback) {
        if (config.isInt(primaryPath)) {
            return config.getInt(primaryPath);
        }
        if (config.isInt(legacyPath)) {
            return config.getInt(legacyPath);
        }
        return fallback;
    }
}
