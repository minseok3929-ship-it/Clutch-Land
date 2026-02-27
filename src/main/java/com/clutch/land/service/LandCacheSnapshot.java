package com.clutch.land.service;

import com.clutch.land.domain.ChunkKey;
import com.clutch.land.domain.Land;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public record LandCacheSnapshot(
        Map<Integer, Land> landsById,
        Map<UUID, Set<Integer>> landsByOwner,
        Map<ChunkKey, Set<Integer>> chunkIndex,
        Map<Integer, Set<UUID>> membersByLand
) {
}
