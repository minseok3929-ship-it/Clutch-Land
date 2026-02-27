package com.clutch.land.repository;

import com.clutch.land.domain.LandMember;
import java.util.List;
import java.util.UUID;

public interface LandMemberRepository {
    List<LandMember> loadAll();

    void addMember(int landId, UUID memberUuid, String role);

    void removeMember(int landId, UUID memberUuid);

    void removeAllByLand(int landId);
}
