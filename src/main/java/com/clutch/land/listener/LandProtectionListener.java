package com.clutch.land.listener;

import com.clutch.land.service.LandCacheService;
import com.clutch.land.ui.MessageFacade;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

public final class LandProtectionListener implements Listener {
    private final LandCacheService landCacheService;
    private final MessageFacade messageFacade;

    public LandProtectionListener(LandCacheService landCacheService, MessageFacade messageFacade) {
        this.landCacheService = landCacheService;
        this.messageFacade = messageFacade;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (!canModify(event.getPlayer(), event.getBlock().getWorld().getUID().toString(), event.getBlock().getX(), event.getBlock().getY(), event.getBlock().getZ())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (!canModify(event.getPlayer(), event.getBlock().getWorld().getUID().toString(), event.getBlock().getX(), event.getBlock().getY(), event.getBlock().getZ())) {
            event.setCancelled(true);
        }
    }

    private boolean canModify(Player player, String worldId, int x, int y, int z) {
        var landOpt = landCacheService.findLandAt(worldId, x, y, z);
        if (landOpt.isEmpty()) {
            if (!player.isOp()) {
                messageFacade.error(player, "등록된 토지에서만 건축할 수 있습니다.");
                return false;
            }
            return true;
        }

        if (landCacheService.canBuild(player, landOpt.get())) {
            return true;
        }

        if (!landOpt.get().isOwned()) {
            messageFacade.error(player, "빈 토지는 OP만 건축할 수 있습니다.");
        } else {
            messageFacade.error(player, "이 토지에서 건축 권한이 없습니다.");
        }
        return false;
    }
}
