package com.clutch.land.listener;

import com.clutch.land.service.LandCacheService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public final class LandProtectionListener implements Listener {
    private final LandCacheService landCacheService;

    public LandProtectionListener(LandCacheService landCacheService) {
        this.landCacheService = landCacheService;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        String worldUuid = event.getBlock().getWorld().getUID().toString();
        int x = event.getBlock().getX();
        int z = event.getBlock().getZ();

        boolean inRegisteredLand = landCacheService.findLandAt(worldUuid, x, z).isPresent();
        if (!inRegisteredLand && !player.isOp()) {
            event.setCancelled(true);
            player.sendMessage("§c등록된 토지에서만 건축할 수 있습니다.");
        }
    }
}
