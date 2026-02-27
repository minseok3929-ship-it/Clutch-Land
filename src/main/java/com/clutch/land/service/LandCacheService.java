package com.clutch.land.service;

import com.clutch.land.domain.Land;
import java.util.List;
import java.util.Optional;

public final class LandCacheService {
    private final List<Land> lands;

    public LandCacheService(List<Land> lands) {
        this.lands = List.copyOf(lands);
    }

    public Optional<Land> findLandAt(String worldUuid, int x, int z) {
        return lands.stream()
                .filter(land -> land.worldUuid().equals(worldUuid))
                .filter(land -> land.contains(x, z))
                .findFirst();
    }
}
