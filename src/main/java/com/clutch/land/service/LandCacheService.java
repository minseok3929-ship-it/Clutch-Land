package com.clutch.land.service;

import com.clutch.land.domain.ChunkKey;
import com.clutch.land.domain.Land;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class LandCacheService {
    private final Map<Integer, Land> landsById;
    private final Map<UUID, Set<Integer>> landsByOwner;
    private final Map<ChunkKey, Set<Integer>> chunkIndex;
    private final Map<Integer, Set<UUID>> membersByLand;

    public LandCacheService(LandCacheSnapshot snapshot) {
        this.landsById = Map.copyOf(snapshot.landsById());
        this.landsByOwner = Map.copyOf(snapshot.landsByOwner());
        this.chunkIndex = Map.copyOf(snapshot.chunkIndex());
        this.membersByLand = Map.copyOf(snapshot.membersByLand());
    }

    public Optional<Land> findLandAt(String worldId, int x, int y, int z) {
        int chunkX = x >> 4;
        int chunkZ = z >> 4;
        ChunkKey key = new ChunkKey(worldId, chunkX, chunkZ);

        Set<Integer> candidates = chunkIndex.get(key);
        if (candidates == null || candidates.isEmpty()) {
            return Optional.empty();
        }

        for (int landId : candidates) {
            Land land = landsById.get(landId);
            if (land != null && land.contains(x, y, z)) {
                return Optional.of(land);
            }
        }

        return Optional.empty();
    }

    public Map<Integer, Land> landsByIdView() {
        return landsById;
    }

    public Map<UUID, Set<Integer>> landsByOwnerView() {
        return landsByOwner;
    }

    public Map<ChunkKey, Set<Integer>> chunkIndexView() {
        return chunkIndex;
    }

    public Map<Integer, Set<UUID>> membersByLandView() {
        return membersByLand;
    }
}
