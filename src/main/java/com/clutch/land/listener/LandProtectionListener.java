package com.clutch.land.listener;

import com.clutch.land.ClutchLandPlugin;
import com.clutch.land.manager.LandManager;
import com.clutch.land.model.Land;
import org.bukkit.Location;
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

public class LandProtectionListener implements Listener {
    private final ClutchLandPlugin plugin;
    private final LandManager landManager;

    public LandProtectionListener(ClutchLandPlugin plugin, LandManager landManager) {
        this.plugin = plugin;
        this.landManager = landManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        ProtectionCheck check = check(event.getPlayer(), event.getBlock().getLocation());
        logProtectionDebug("BlockBreakEvent", event.getPlayer(), event.getBlock().getLocation(), check);
        cancelIfDenied(event, event.getPlayer(), check);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        ProtectionCheck check = check(event.getPlayer(), event.getBlockPlaced().getLocation());
        logProtectionDebug("BlockPlaceEvent", event.getPlayer(), event.getBlockPlaced().getLocation(), check);
        cancelIfDenied(event, event.getPlayer(), check);
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) {
            return;
        }
        ProtectionCheck check = check(event.getPlayer(), event.getClickedBlock().getLocation());
        if (check.cancel()) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ClutchLandPlugin.PREFIX + "본인의 권한이 없는 땅입니다.");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        ProtectionCheck check = check(event.getPlayer(), event.getBlock().getLocation());
        if (check.cancel()) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ClutchLandPlugin.PREFIX + "본인의 권한이 없는 땅입니다.");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent event) {
        ProtectionCheck check = check(event.getPlayer(), event.getBlock().getLocation());
        if (check.cancel()) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ClutchLandPlugin.PREFIX + "본인의 권한이 없는 땅입니다.");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }
        ProtectionCheck check = check(player, event.getEntity().getLocation());
        if (check.cancel()) {
            event.setCancelled(true);
            player.sendMessage(ClutchLandPlugin.PREFIX + "본인의 권한이 없는 땅입니다.");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onHangingBreak(HangingBreakByEntityEvent event) {
        if (!(event.getRemover() instanceof Player player)) {
            return;
        }
        ProtectionCheck check = check(player, event.getEntity().getLocation());
        if (check.cancel()) {
            event.setCancelled(true);
            player.sendMessage(ClutchLandPlugin.PREFIX + "본인의 권한이 없는 땅입니다.");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onHangingPlace(HangingPlaceEvent event) {
        Player player = event.getPlayer();
        if (player == null) {
            return;
        }
        Entity entity = event.getEntity();
        ProtectionCheck check = check(player, entity.getLocation());
        if (check.cancel()) {
            event.setCancelled(true);
            player.sendMessage(ClutchLandPlugin.PREFIX + "본인의 권한이 없는 땅입니다.");
        }
    }

    private ProtectionCheck check(Player player, Location location) {
        Land land = landManager.getLandAt(location).orElse(null);

        if (land == null) {
            return new ProtectionCheck(null, false);
        }

        if (player.isOp() || player.hasPermission(ClutchLandPlugin.ADMIN_PERMISSION)) {
            return new ProtectionCheck(land, false);
        }

        return new ProtectionCheck(land, !landManager.hasPermission(player, land));
    }

    private void cancelIfDenied(org.bukkit.event.Cancellable event, Player player, ProtectionCheck check) {
        if (!check.cancel()) {
            return;
        }
        event.setCancelled(true);
        player.sendMessage(ClutchLandPlugin.PREFIX + "본인의 권한이 없는 땅입니다.");
    }

    private void logProtectionDebug(String eventName, Player player, Location location, ProtectionCheck check) {
        if (!plugin.isProtectionDebug()) {
            return;
        }
        plugin.getLogger().info("[ProtectionDebug] event=" + eventName
            + " player=" + player.getName()
            + " world=" + location.getWorld().getName()
            + " x=" + location.getBlockX()
            + " y=" + location.getBlockY()
            + " z=" + location.getBlockZ()
            + " landNull=" + (check.land() == null)
            + " landId=" + (check.land() == null ? "null" : check.land().getId())
            + " cancel=" + check.cancel());
    }

    private record ProtectionCheck(Land land, boolean cancel) {
    }
}
