package com.clutch.land.manager;

import com.clutch.land.ClutchLandPlugin;
import com.clutch.land.db.LandDatabase;
import com.clutch.land.model.Land;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.*;

public class LandManager {
    private final ClutchLandPlugin plugin;
    private final LandDatabase db;

    public LandManager(ClutchLandPlugin plugin, LandDatabase db) {
        this.plugin = plugin;
        this.db = db;
    }

    public Optional<Land> getLandAt(Location location) {
        if (location == null || location.getWorld() == null) {
            return Optional.empty();
        }
        return db.getAllLands().stream().filter(land -> land.contains(location)).findFirst();
    }

    public Optional<Land> getOwnedLand(UUID uuid) {
        return db.getOwnedLand(uuid);
    }

    public boolean createLand(String world, int x1, int y1, int z1, int x2, int y2, int z2) {
        Land candidate = new Land(-1, world, x1, y1, z1, x2, y2, z2, null, null);
        for (Land land : db.getAllLands()) {
            if (candidate.overlaps(land)) {
                return false;
            }
        }
        int id = db.createLand(world, x1, y1, z1, x2, y2, z2);
        return id > 0;
    }

    public int deleteLandRegion(String world, int x1, int y1, int z1, int x2, int y2, int z2) {
        int minX = Math.min(x1, x2);
        int minY = Math.min(y1, y2);
        int minZ = Math.min(z1, z2);
        int maxX = Math.max(x1, x2);
        int maxY = Math.max(y1, y2);
        int maxZ = Math.max(z1, z2);
        return db.deleteLandsOverlapping(world, minX, minY, minZ, maxX, maxY, maxZ);
    }

    public boolean claimLand(Player player, Land land) {
        if (getOwnedLand(player.getUniqueId()).isPresent()) {
            return false;
        }
        if (land.hasOwner()) {
            return false;
        }
        boolean updated = db.updateOwner(land.getId(), player.getUniqueId(), player.getName());
        if (updated) {
            land.setOwner(player.getUniqueId(), player.getName());
        }
        return updated;
    }

    public boolean unclaimLand(Land land) {
        boolean updated = db.updateOwner(land.getId(), null, null);
        db.clearMembers(land.getId());
        if (updated) {
            land.setOwner(null, null);
        }
        return updated;
    }

    public boolean transferLand(Land land, OfflinePlayer target) {
        if (target.getUniqueId() == null || getOwnedLand(target.getUniqueId()).isPresent()) {
            return false;
        }
        boolean updated = db.updateOwner(land.getId(), target.getUniqueId(), target.getName() == null ? "Unknown" : target.getName());
        if (updated) {
            db.clearMembers(land.getId());
            land.setOwner(target.getUniqueId(), target.getName() == null ? "Unknown" : target.getName());
        }
        return updated;
    }

    public boolean addMember(Land land, OfflinePlayer target) {
        if (target.getUniqueId() == null) {
            return false;
        }
        return db.addMember(land.getId(), target.getUniqueId(), target.getName() == null ? "Unknown" : target.getName());
    }

    public boolean removeMember(Land land, OfflinePlayer target) {
        if (target.getUniqueId() == null) {
            return false;
        }
        return db.removeMember(land.getId(), target.getUniqueId());
    }

    public boolean hasPermission(Player player, Land land) {
        if (player.isOp() || player.hasPermission(ClutchLandPlugin.ADMIN_PERMISSION)) {
            return true;
        }
        if (land == null) {
            return false;
        }
        if (land.getOwnerUuid() == null) {
            return false;
        }
        if (player.getUniqueId().equals(land.getOwnerUuid())) {
            return true;
        }
        return db.getMembers(land.getId()).contains(player.getUniqueId());
    }

    public boolean dismissOwnedLandByPlayer(OfflinePlayer owner) {
        if (owner.getUniqueId() == null) {
            return false;
        }
        Optional<Land> owned = getOwnedLand(owner.getUniqueId());
        if (owned.isEmpty()) {
            return false;
        }
        return unclaimLand(owned.get());
    }
}
