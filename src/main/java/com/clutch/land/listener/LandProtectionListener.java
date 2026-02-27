package com.clutch.land.listener;

import com.clutch.land.service.LandPermissionService;
import com.clutch.land.service.LandQueryService;
import com.clutch.land.ui.MessageFacade;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public final class LandProtectionListener implements Listener {
    private static final String DENY_MESSAGE = "본인의 권한이 없는 땅입니다.";

    private final LandQueryService landQueryService;
    private final LandPermissionService permissionService;
    private final MessageFacade messageFacade;

    public LandProtectionListener(
            LandQueryService landQueryService,
            LandPermissionService permissionService,
            MessageFacade messageFacade
    ) {
        this.landQueryService = landQueryService;
        this.permissionService = permissionService;
        this.messageFacade = messageFacade;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (!canModify(event.getPlayer(), event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (!canModify(event.getPlayer(), event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.LEFT_CLICK_BLOCK) {
            return;
        }
        if (event.getClickedBlock() == null) {
            return;
        }

        if (!canModify(event.getPlayer(), event.getClickedBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (!canModify(event.getPlayer(), event.getRightClicked().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onHangingBreak(HangingBreakByEntityEvent event) {
        Entity remover = event.getRemover();
        if (!(remover instanceof Player player)) {
            return;
        }
        if (!canModify(player, event.getEntity().getLocation())) {
            event.setCancelled(true);
        }
    }

    private boolean canModify(Player player, Location location) {
        var landOpt = landQueryService.findAt(location);
        if (landOpt.isEmpty()) {
            return player.isOp();
        }

        if (permissionService.canAccess(player, landOpt.get())) {
            return true;
        }

        messageFacade.error(player, DENY_MESSAGE);
        return false;
    }
}
