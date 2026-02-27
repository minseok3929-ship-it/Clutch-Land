package com.clutch.land.repository;

import com.clutch.land.domain.Land;
import java.util.List;

public interface LandRepository {
    List<Land> findAll();
}
