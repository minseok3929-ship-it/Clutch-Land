package com.clutch.land.service;

import com.clutch.land.domain.Land;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public final class LandQueryService {
    private final LandCacheService landCacheService;
    private final Map<UUID, Integer> playerCurrentLandId = new ConcurrentHashMap<>();

    public LandQueryService(LandCacheService landCacheService) {
        this.landCacheService = landCacheService;
    }

    public Optional<Land> findAt(Location location) {
        if (location == null || location.getWorld() == null) {
            return Optional.empty();
        }
        return landCacheService.findLandAt(
                location.getWorld().getUID().toString(),
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ()
        );
    }

    public Optional<Land> findAt(Player player) {
        return findAt(player.getLocation());
    }

    public Integer getCurrentLandId(UUID playerId) {
        return playerCurrentLandId.get(playerId);
    }

    public void setCurrentLandId(UUID playerId, Integer landId) {
        if (landId == null) {
            playerCurrentLandId.remove(playerId);
            return;
        }
        playerCurrentLandId.put(playerId, landId);
    }
}
