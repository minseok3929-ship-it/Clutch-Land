package com.clutch.land.service;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Location;

public final class SelectionService {
    private final Map<UUID, Selection> selections = new ConcurrentHashMap<>();

    public void setPos1(UUID playerId, Location location) {
        selections.compute(playerId, (ignored, old) -> {
            Selection base = old == null ? new Selection() : old;
            return base.withPos1(location);
        });
    }

    public void setPos2(UUID playerId, Location location) {
        selections.compute(playerId, (ignored, old) -> {
            Selection base = old == null ? new Selection() : old;
            return base.withPos2(location);
        });
    }

    public Optional<Selection> get(UUID playerId) {
        return Optional.ofNullable(selections.get(playerId));
    }

    public record SelectionPoint(String worldId, int x, int y, int z) {
        public static SelectionPoint from(Location location) {
            return new SelectionPoint(
                    location.getWorld().getUID().toString(),
                    location.getBlockX(),
                    location.getBlockY(),
                    location.getBlockZ()
            );
        }
    }

    public record Selection(SelectionPoint pos1, SelectionPoint pos2) {
        public Selection() {
            this(null, null);
        }

        public Selection withPos1(Location location) {
            SelectionPoint point = SelectionPoint.from(location);
            SelectionPoint adjustedPos2 = pos2;
            if (pos2 != null && !pos2.worldId().equals(point.worldId())) {
                adjustedPos2 = null;
            }
            return new Selection(point, adjustedPos2);
        }

        public Selection withPos2(Location location) {
            SelectionPoint point = SelectionPoint.from(location);
            SelectionPoint adjustedPos1 = pos1;
            if (pos1 != null && !pos1.worldId().equals(point.worldId())) {
                adjustedPos1 = null;
            }
            return new Selection(adjustedPos1, point);
        }

        public boolean isComplete() {
            return pos1 != null && pos2 != null && pos1.worldId().equals(pos2.worldId());
        }

        public SelectionBounds toBounds() {
            if (!isComplete()) {
                throw new IllegalStateException("Selection is not complete");
            }
            int minX = Math.min(pos1.x(), pos2.x());
            int maxX = Math.max(pos1.x(), pos2.x());
            int minY = Math.min(pos1.y(), pos2.y());
            int maxY = Math.max(pos1.y(), pos2.y());
            int minZ = Math.min(pos1.z(), pos2.z());
            int maxZ = Math.max(pos1.z(), pos2.z());
            return new SelectionBounds(pos1.worldId(), minX, maxX, minY, maxY, minZ, maxZ);
        }
    }

    public record SelectionBounds(
            String worldId,
            int minX,
            int maxX,
            int minY,
            int maxY,
            int minZ,
            int maxZ
    ) {
    }
}
