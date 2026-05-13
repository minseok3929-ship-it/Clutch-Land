package com.clutch.land.manager;

import com.clutch.land.ClutchLandPlugin;
import com.clutch.land.db.LandDatabase;
import com.clutch.land.model.Land;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class LandManager {
    private final ClutchLandPlugin plugin;
    private final LandDatabase db;
    private final List<Land> lands = new ArrayList<>();
    private final Map<String, List<Land>> landsByChunk = new HashMap<>();
    private final Map<Integer, Set<UUID>> membersByLandId = new HashMap<>();

    public LandManager(ClutchLandPlugin plugin, LandDatabase db) {
        this.plugin = plugin;
        this.db = db;
        loadLands();
    }

    public void loadLands() {
        lands.clear();
        landsByChunk.clear();
        membersByLandId.clear();

        for (Land land : db.getAllLands()) {
            Land normalized = normalizeLandHeight(land);
            lands.add(normalized);
            indexLand(normalized);
        }

        membersByLandId.putAll(db.getAllMembers());
        plugin.getLogger().info("Loaded " + lands.size() + " lands into memory cache.");
    }

    public Optional<Land> getLandAt(Location location) {
        if (location == null || location.getWorld() == null) {
            return Optional.empty();
        }

        List<Land> chunkLands = landsByChunk.get(chunkKey(location.getWorld().getName(), location.getBlockX() >> 4, location.getBlockZ() >> 4));
        if (chunkLands == null || chunkLands.isEmpty()) {
            return Optional.empty();
        }

        return chunkLands.stream().filter(land -> land.contains(location)).findFirst();
    }

    public Optional<Land> getOwnedLand(UUID uuid) {
        if (uuid == null) {
            return Optional.empty();
        }
        return lands.stream().filter(land -> uuid.equals(land.getOwnerUuid())).findFirst();
    }

    public int getOwnedLandCount(UUID uuid) {
        return db.countOwnedLands(uuid);
    }

    public boolean createLand(String world, int x1, int y1, int z1, int x2, int y2, int z2) {
        World bukkitWorld = Bukkit.getWorld(world);
        if (bukkitWorld == null) {
            return false;
        }

        int minY = bukkitWorld.getMinHeight();
        int maxY = bukkitWorld.getMaxHeight() - 1;

        Land candidate = new Land(-1, world, x1, minY, z1, x2, maxY, z2, null, null);
        for (Land land : lands) {
            if (candidate.overlaps(land)) {
                return false;
            }
        }
        int id = db.createLand(
            world,
            Math.min(x1, x2),
            minY,
            Math.min(z1, z2),
            Math.max(x1, x2),
            maxY,
            Math.max(z1, z2)
        );
        if (id > 0) {
            loadLands();
            return true;
        }
        return false;
    }

    public int deleteLandRegion(String world, int x1, int y1, int z1, int x2, int y2, int z2) {
        World bukkitWorld = Bukkit.getWorld(world);
        int minY = bukkitWorld == null ? Math.min(y1, y2) : bukkitWorld.getMinHeight();
        int maxY = bukkitWorld == null ? Math.max(y1, y2) : bukkitWorld.getMaxHeight() - 1;

        int minX = Math.min(x1, x2);
        int minZ = Math.min(z1, z2);
        int maxX = Math.max(x1, x2);
        int maxZ = Math.max(z1, z2);
        int deleted = db.deleteLandsOverlapping(world, minX, minY, minZ, maxX, maxY, maxZ);
        if (deleted > 0) {
            loadLands();
        }
        return deleted;
    }

    public boolean claimLand(Player player, Land land) {
        if (land.hasOwner()) {
            return false;
        }
        boolean updated = db.updateOwner(land.getId(), player.getUniqueId(), player.getName());
        if (updated) {
            loadLands();
        }
        return updated;
    }

    public boolean unclaimLand(Land land) {
        boolean updated = db.updateOwner(land.getId(), null, null);
        db.clearMembers(land.getId());
        if (updated) {
            loadLands();
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
            loadLands();
        }
        return updated;
    }

    public boolean addMember(Land land, OfflinePlayer target) {
        if (target.getUniqueId() == null) {
            return false;
        }
        boolean updated = db.addMember(land.getId(), target.getUniqueId(), target.getName() == null ? "Unknown" : target.getName());
        if (updated) {
            membersByLandId.computeIfAbsent(land.getId(), ignored -> new HashSet<>()).add(target.getUniqueId());
        }
        return updated;
    }

    public boolean removeMember(Land land, OfflinePlayer target) {
        if (target.getUniqueId() == null) {
            return false;
        }
        boolean updated = db.removeMember(land.getId(), target.getUniqueId());
        if (updated) {
            Set<UUID> members = membersByLandId.get(land.getId());
            if (members != null) {
                members.remove(target.getUniqueId());
            }
        }
        return updated;
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
        return membersByLandId.getOrDefault(land.getId(), Set.of()).contains(player.getUniqueId());
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

    private Land normalizeLandHeight(Land land) {
        World world = Bukkit.getWorld(land.getWorld());
        if (world == null) {
            return land;
        }
        return new Land(
            land.getId(),
            land.getWorld(),
            land.getMinX(),
            world.getMinHeight(),
            land.getMinZ(),
            land.getMaxX(),
            world.getMaxHeight() - 1,
            land.getMaxZ(),
            land.getOwnerUuid(),
            land.getOwnerName()
        );
    }

    private void indexLand(Land land) {
        int minChunkX = land.getMinX() >> 4;
        int maxChunkX = land.getMaxX() >> 4;
        int minChunkZ = land.getMinZ() >> 4;
        int maxChunkZ = land.getMaxZ() >> 4;

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                landsByChunk.computeIfAbsent(chunkKey(land.getWorld(), chunkX, chunkZ), ignored -> new ArrayList<>()).add(land);
            }
        }
    }

    private String chunkKey(String world, int chunkX, int chunkZ) {
        return world + ":" + chunkX + ":" + chunkZ;
    }
}
