package com.clutch.land.service;

import com.clutch.land.domain.ChunkKey;
import com.clutch.land.domain.Land;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class LandCacheService {
    private final Map<Integer, Land> landsById;
    private final Map<UUID, Set<Integer>> landsByOwner;
    private final Map<ChunkKey, Set<Integer>> chunkIndex;
    private final Map<Integer, Set<UUID>> membersByLand;

    public LandCacheService(LandCacheSnapshot snapshot) {
        this.landsById = new ConcurrentHashMap<>(snapshot.landsById());
        this.landsByOwner = new ConcurrentHashMap<>(snapshot.landsByOwner());
        this.chunkIndex = new ConcurrentHashMap<>(snapshot.chunkIndex());
        this.membersByLand = new ConcurrentHashMap<>(snapshot.membersByLand());
    }

    public Optional<Land> findLandAt(String worldId, int x, int y, int z) {
        ChunkKey key = new ChunkKey(worldId, x >> 4, z >> 4);
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

    public Set<Integer> collectCandidateLandIds(String worldId, int minX, int maxX, int minZ, int maxZ) {
        Set<Integer> result = new HashSet<>();
        int minChunkX = minX >> 4;
        int maxChunkX = maxX >> 4;
        int minChunkZ = minZ >> 4;
        int maxChunkZ = maxZ >> 4;

        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                Set<Integer> ids = chunkIndex.get(new ChunkKey(worldId, cx, cz));
                if (ids != null) {
                    result.addAll(ids);
                }
            }
        }
        return result;
    }

    public Optional<Land> getLandById(int landId) {
        return Optional.ofNullable(landsById.get(landId));
    }

    public void addLand(Land land, Set<ChunkKey> chunks) {
        landsById.put(land.id(), land);

        if (land.ownerUuid() != null && !land.ownerUuid().isBlank()) {
            try {
                UUID owner = UUID.fromString(land.ownerUuid());
                landsByOwner.computeIfAbsent(owner, ignored -> ConcurrentHashMap.newKeySet()).add(land.id());
            } catch (IllegalArgumentException ignored) {
                // ignore invalid owner UUID
            }
        }

        for (ChunkKey chunk : chunks) {
            chunkIndex.computeIfAbsent(chunk, ignored -> ConcurrentHashMap.newKeySet()).add(land.id());
        }
    }

    public Map<Integer, Set<UUID>> membersByLandView() {
        return Map.copyOf(membersByLand);
    }
}
