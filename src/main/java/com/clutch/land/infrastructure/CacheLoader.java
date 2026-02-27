package com.clutch.land.infrastructure;

import com.clutch.land.domain.ChunkKey;
import com.clutch.land.domain.Land;
import com.clutch.land.domain.LandMember;
import com.clutch.land.repository.LandChunkRepository;
import com.clutch.land.repository.LandMemberRepository;
import com.clutch.land.repository.LandRepository;
import com.clutch.land.service.LandCacheSnapshot;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class CacheLoader {
    private final LandRepository landRepository;
    private final LandMemberRepository landMemberRepository;
    private final LandChunkRepository landChunkRepository;

    public CacheLoader(
            LandRepository landRepository,
            LandMemberRepository landMemberRepository,
            LandChunkRepository landChunkRepository
    ) {
        this.landRepository = landRepository;
        this.landMemberRepository = landMemberRepository;
        this.landChunkRepository = landChunkRepository;
    }

    public LandCacheSnapshot loadAll() {
        Map<Integer, Land> landsById = new HashMap<>();
        Map<UUID, Set<Integer>> landsByOwner = new HashMap<>();
        Map<ChunkKey, Set<Integer>> chunkIndex = new HashMap<>();
        Map<Integer, Set<UUID>> membersByLand = new HashMap<>();

        for (Land land : landRepository.loadAll()) {
            landsById.put(land.id(), land);
            if (land.ownerUuid() != null && !land.ownerUuid().isBlank()) {
                try {
                    UUID ownerUuid = UUID.fromString(land.ownerUuid());
                    landsByOwner.computeIfAbsent(ownerUuid, ignored -> new HashSet<>()).add(land.id());
                } catch (IllegalArgumentException ignored) {
                    // invalid owner UUID in DB row is skipped for owner index build
                }
            }
        }

        for (LandChunkRepository.LandChunkRow row : landChunkRepository.loadAll()) {
            chunkIndex.computeIfAbsent(row.chunkKey(), ignored -> new HashSet<>()).add(row.landId());
        }

        for (LandMember member : landMemberRepository.loadAll()) {
            membersByLand.computeIfAbsent(member.landId(), ignored -> new HashSet<>()).add(member.memberUuid());
        }

        return new LandCacheSnapshot(landsById, landsByOwner, chunkIndex, membersByLand);
    }
}
