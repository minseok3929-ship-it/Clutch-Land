package com.clutch.land.api;

import com.clutch.land.domain.Land;
import java.util.Optional;

public interface ClutchLandApi {
    Optional<Land> findLandAt(String worldUuid, int x, int z);
}
