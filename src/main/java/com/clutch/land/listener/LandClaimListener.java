package com.clutch.land.listener;

import com.clutch.land.ClutchLandPlugin;
import com.clutch.land.item.LandItemFactory;
import com.clutch.land.manager.LandManager;
import com.clutch.land.model.Land;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

public class LandClaimListener implements Listener {
    private final ClutchLandPlugin plugin;
    private final LandManager landManager;
    private final LandItemFactory itemFactory;

    public LandClaimListener(ClutchLandPlugin plugin, LandManager landManager, LandItemFactory itemFactory) {
        this.plugin = plugin;
        this.landManager = landManager;
        this.itemFactory = itemFactory;
    }

    @EventHandler(ignoreCancelled = true)
    public void onUseClaimTicket(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        ItemStack item = event.getItem();
        if (!itemFactory.isClaimTicket(item)) {
            return;
        }

        event.setCancelled(true);

        Optional<Land> landOpt = landManager.getLandAt(event.getPlayer().getLocation());

        if (plugin.isClaimDebug()) {
            Land land = landOpt.orElse(null);
            plugin.getLogger().info("[ClaimDebug] player=" + event.getPlayer().getName()
                + " world=" + event.getPlayer().getWorld().getName()
                + " x=" + event.getPlayer().getLocation().getBlockX()
                + " y=" + event.getPlayer().getLocation().getBlockY()
                + " z=" + event.getPlayer().getLocation().getBlockZ()
                + " land=" + (land == null ? "null" : land.getId())
                + " owner=" + (land == null ? "null" : land.getOwnerName()));
        }

        if (landOpt.isEmpty()) {
            event.getPlayer().sendMessage(ClutchLandPlugin.PREFIX + "이 곳은 구매할 수 없는 땅 입니다.");
            return;
        }

        Land land = landOpt.get();
        if (land.hasOwner()) {
            event.getPlayer().sendMessage(ClutchLandPlugin.PREFIX + "이 곳은 구매할 수 없는 땅 입니다.");
            return;
        }

        if (landManager.getOwnedLand(event.getPlayer().getUniqueId()).isPresent()) {
            event.getPlayer().sendMessage(ClutchLandPlugin.PREFIX + "이미 소유한 땅이 있습니다.");
            return;
        }

        if (landManager.claimLand(event.getPlayer(), land)) {
            if (item.getAmount() <= 1) {
                event.getPlayer().getInventory().setItemInMainHand(null);
            } else {
                item.setAmount(item.getAmount() - 1);
            }
            event.getPlayer().sendMessage(ClutchLandPlugin.PREFIX + "땅을 구매했습니다.");
        } else {
            event.getPlayer().sendMessage(ClutchLandPlugin.PREFIX + "이 곳은 구매할 수 없는 땅 입니다.");
        }
    }
}
