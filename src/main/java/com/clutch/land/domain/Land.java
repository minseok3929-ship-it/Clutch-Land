package com.clutch.land.domain;

public record Land(
        int id,
        String worldUuid,
        int minX,
        int maxX,
        int minZ,
        int maxZ,
        String ownerUuid
) {
    public boolean contains(int x, int z) {
        return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
    }

    public boolean isOwned() {
        return ownerUuid != null && !ownerUuid.isBlank();
    }
}
