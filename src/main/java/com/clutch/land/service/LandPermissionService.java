package com.clutch.land.service;

import com.clutch.land.domain.Land;
import org.bukkit.entity.Player;

public final class LandPermissionService {
    private final LandCacheService landCacheService;

    public LandPermissionService(LandCacheService landCacheService) {
        this.landCacheService = landCacheService;
    }

    public boolean canAccess(Player player, Land land) {
        if (player.isOp()) {
            return true;
        }

        if (!land.isOwned()) {
            return false;
        }

        if (landCacheService.isOwner(land.id(), player.getUniqueId())) {
            return true;
        }

        return landCacheService.isMember(land.id(), player.getUniqueId());
    }
}
