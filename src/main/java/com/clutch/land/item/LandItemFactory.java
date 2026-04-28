package com.clutch.land.item;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public class LandItemFactory {
    private static final String TOOL_NAME = ChatColor.GOLD + "토지 도구";
    private static final String CLAIM_TICKET_NAME = ChatColor.AQUA + "땅 구매권";

    private final NamespacedKey itemKey;

    public LandItemFactory(JavaPlugin plugin) {
        this.itemKey = new NamespacedKey(plugin, "clutchland_item_type");
    }

    public ItemStack createTool() {
        ItemStack item = new ItemStack(Material.COMPASS);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.setDisplayName(TOOL_NAME);
        meta.setCustomModelData(10001);
        meta.getPersistentDataContainer().set(itemKey, PersistentDataType.STRING, "tool");
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack createClaimTicket() {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.setDisplayName(CLAIM_TICKET_NAME);
        meta.setCustomModelData(10002);
        meta.getPersistentDataContainer().set(itemKey, PersistentDataType.STRING, "ticket");
        item.setItemMeta(meta);
        return item;
    }

    public boolean isTool(ItemStack item) {
        return isType(item, "tool", 10001);
    }

    public boolean isClaimTicket(ItemStack item) {
        return isType(item, "ticket", 10002);
    }

    private boolean isType(ItemStack item, String type, int cmd) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasCustomModelData() || meta.getCustomModelData() != cmd) {
            return false;
        }
        String value = meta.getPersistentDataContainer().get(itemKey, PersistentDataType.STRING);
        return type.equals(value);
    }
}
