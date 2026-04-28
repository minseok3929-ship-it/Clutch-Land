package com.clutch.land.listener;

import com.clutch.land.ClutchLandPlugin;
import com.clutch.land.manager.LandManager;
import com.clutch.land.model.Land;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.Optional;

public class LandProtectionListener implements Listener {
    private final ClutchLandPlugin plugin;
    private final LandManager landManager;

    public LandProtectionListener(ClutchLandPlugin plugin, LandManager landManager) {
        this.plugin = plugin;
        this.landManager = landManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (!check(event.getPlayer(), landManager.getLandAt(event.getBlock().getLocation()))) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (!check(event.getPlayer(), landManager.getLandAt(event.getBlock().getLocation()))) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) {
            return;
        }
        if (!check(event.getPlayer(), landManager.getLandAt(event.getClickedBlock().getLocation()))) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        if (!check(event.getPlayer(), landManager.getLandAt(event.getBlock().getLocation()))) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent event) {
        if (!check(event.getPlayer(), landManager.getLandAt(event.getBlock().getLocation()))) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }
        if (!check(player, landManager.getLandAt(event.getEntity().getLocation()))) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onHangingBreak(HangingBreakByEntityEvent event) {
        if (!(event.getRemover() instanceof Player player)) {
            return;
        }
        if (!check(player, landManager.getLandAt(event.getEntity().getLocation()))) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onHangingPlace(HangingPlaceEvent event) {
        Player player = event.getPlayer();
        if (player == null) {
            return;
        }
        Entity entity = event.getEntity();
        if (!check(player, landManager.getLandAt(entity.getLocation()))) {
            event.setCancelled(true);
        }
    }

    private boolean check(Player player, Optional<Land> landOpt) {
        if (player.isOp() || player.hasPermission(ClutchLandPlugin.ADMIN_PERMISSION)) {
            return true;
        }
        if (landOpt.isEmpty()) {
            if (!plugin.isProtectUnregisteredLand()) {
                return true;
            }
            player.sendMessage(ClutchLandPlugin.PREFIX + "본인의 권한이 없는 땅입니다.");
            return false;
        }
        if (!landManager.hasPermission(player, landOpt.get())) {
            player.sendMessage(ClutchLandPlugin.PREFIX + "본인의 권한이 없는 땅입니다.");
            return false;
        }
        return true;
    }
}
