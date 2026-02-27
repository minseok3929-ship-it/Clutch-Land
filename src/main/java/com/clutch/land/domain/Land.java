package com.clutch.land.domain;

public record Land(
        int id,
        String worldId,
        int minX,
        int maxX,
        int minY,
        int maxY,
        int minZ,
        int maxZ,
        String ownerUuid,
        int grade,
        String flagsJson
) {
    public boolean contains(int x, int y, int z) {
        return x >= minX && x <= maxX
                && y >= minY && y <= maxY
                && z >= minZ && z <= maxZ;
    }

    public boolean containsXZ(int x, int z) {
        return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
    }

    public boolean isOwned() {
        return ownerUuid != null && !ownerUuid.isBlank();
    }
}
