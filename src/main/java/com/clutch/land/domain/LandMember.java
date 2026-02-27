package com.clutch.land.domain;

import java.util.UUID;

public record LandMember(int landId, UUID memberUuid, LandRole role) {
}
