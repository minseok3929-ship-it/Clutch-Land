package com.clutch.land.api;

import com.clutch.land.domain.Land;
import java.util.Optional;

public interface ClutchLandApi {
    Optional<Land> findLandAt(String worldId, int x, int y, int z);
}
