package com.clutch.land.repository;

import com.clutch.land.domain.Land;
import java.util.List;

public interface LandRepository {
    List<Land> loadAll();

    int insert(Land land);

    void delete(int landId);

    void updateOwner(int landId, String ownerUuid);

    void updateBounds(int landId, int minX, int maxX, int minY, int maxY, int minZ, int maxZ);
}
