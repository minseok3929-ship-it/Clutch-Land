package com.clutch.land.repository;

import com.clutch.land.domain.ChunkKey;
import java.util.List;
import java.util.Set;

public interface LandChunkRepository {
    List<LandChunkRow> loadAll();

    void replaceLandChunks(int landId, Set<ChunkKey> chunks);

    void removeLandChunks(int landId);

    record LandChunkRow(ChunkKey chunkKey, int landId) {
    }
}
