package com.clutch.land.service;

import com.clutch.land.domain.ChunkKey;
import com.clutch.land.domain.Land;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.Player;

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

    public boolean canBuild(Player player, Land land) {
        if (player.isOp()) {
            return true;
        }

        if (!land.isOwned()) {
            return false;
        }

        UUID playerId = player.getUniqueId();
        if (playerId.toString().equals(land.ownerUuid())) {
            return true;
        }

        Set<UUID> members = membersByLand.get(land.id());
        return members != null && members.contains(playerId);
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
        indexOwner(land.id(), land.ownerUuid());
        for (ChunkKey chunk : chunks) {
            chunkIndex.computeIfAbsent(chunk, ignored -> ConcurrentHashMap.newKeySet()).add(land.id());
        }
    }

    public void updateOwner(int landId, String newOwnerUuid) {
        Land old = landsById.get(landId);
        if (old == null) {
            return;
        }

        unindexOwner(landId, old.ownerUuid());
        Land updated = new Land(
                old.id(), old.worldId(), old.minX(), old.maxX(), old.minY(), old.maxY(), old.minZ(), old.maxZ(),
                newOwnerUuid, old.grade(), old.flagsJson()
        );
        landsById.put(landId, updated);
        indexOwner(landId, newOwnerUuid);
    }

    public void addMember(int landId, UUID memberUuid) {
        membersByLand.computeIfAbsent(landId, ignored -> ConcurrentHashMap.newKeySet()).add(memberUuid);
    }

    public void removeMember(int landId, UUID memberUuid) {
        Set<UUID> set = membersByLand.get(landId);
        if (set != null) {
            set.remove(memberUuid);
        }
    }

    public void clearMembers(int landId) {
        membersByLand.remove(landId);
    }

    public boolean isOwner(int landId, UUID playerUuid) {
        Land land = landsById.get(landId);
        return land != null && land.isOwned() && playerUuid.toString().equals(land.ownerUuid());
    }

    private void indexOwner(int landId, String ownerUuid) {
        if (ownerUuid == null || ownerUuid.isBlank()) {
            return;
        }
        try {
            UUID owner = UUID.fromString(ownerUuid);
            landsByOwner.computeIfAbsent(owner, ignored -> ConcurrentHashMap.newKeySet()).add(landId);
        } catch (IllegalArgumentException ignored) {
            // ignore invalid owner uuid
        }
    }

    private void unindexOwner(int landId, String ownerUuid) {
        if (ownerUuid == null || ownerUuid.isBlank()) {
            return;
        }
        try {
            UUID owner = UUID.fromString(ownerUuid);
            Set<Integer> set = landsByOwner.get(owner);
            if (set != null) {
                set.remove(landId);
            }
        } catch (IllegalArgumentException ignored) {
            // ignore invalid owner uuid
        }
    }
}
