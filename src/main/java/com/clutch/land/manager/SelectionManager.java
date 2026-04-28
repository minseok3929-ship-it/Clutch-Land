package com.clutch.land.manager;

import org.bukkit.Location;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SelectionManager {
    private final Map<UUID, Location> first = new HashMap<>();
    private final Map<UUID, Location> second = new HashMap<>();

    public void setFirst(UUID playerUuid, Location location) {
        first.put(playerUuid, location.clone());
    }

    public void setSecond(UUID playerUuid, Location location) {
        second.put(playerUuid, location.clone());
    }

    public Location getFirst(UUID playerUuid) {
        return first.get(playerUuid);
    }

    public Location getSecond(UUID playerUuid) {
        return second.get(playerUuid);
    }
}
