package com.clutch.land.db;

import com.clutch.land.model.Land;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.*;
import java.time.Instant;
import java.util.*;

public class LandDatabase {
    private final JavaPlugin plugin;
    private Connection connection;

    public LandDatabase(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        try {
            File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists() && !dataFolder.mkdirs()) {
                throw new IllegalStateException("Could not create plugin data folder");
            }
            File dbFile = new File(dataFolder, "lands.db");
            this.connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            createTables();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to initialize SQLite database", e);
        }
    }

    private void createTables() throws SQLException {
        String lands = """
            CREATE TABLE IF NOT EXISTS lands (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                world TEXT NOT NULL,
                min_x INTEGER NOT NULL,
                min_y INTEGER NOT NULL,
                min_z INTEGER NOT NULL,
                max_x INTEGER NOT NULL,
                max_y INTEGER NOT NULL,
                max_z INTEGER NOT NULL,
                owner_uuid TEXT NULL,
                owner_name TEXT NULL,
                created_at INTEGER NOT NULL
            )
            """;

        String members = """
            CREATE TABLE IF NOT EXISTS land_members (
                land_id INTEGER NOT NULL,
                member_uuid TEXT NOT NULL,
                member_name TEXT NOT NULL,
                PRIMARY KEY (land_id, member_uuid),
                FOREIGN KEY (land_id) REFERENCES lands(id) ON DELETE CASCADE
            )
            """;

        try (Statement statement = connection.createStatement()) {
            statement.execute(lands);
            statement.execute(members);
        }
    }

    public List<Land> getAllLands() {
        String sql = "SELECT * FROM lands";
        List<Land> result = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                result.add(mapLand(rs));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to load lands: " + e.getMessage());
        }
        return result;
    }

    public Optional<Land> getOwnedLand(UUID ownerUuid) {
        String sql = "SELECT * FROM lands WHERE owner_uuid = ? LIMIT 1";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, ownerUuid.toString());
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapLand(rs));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to find owned land: " + e.getMessage());
        }
        return Optional.empty();
    }

    public int countOwnedLands(UUID ownerUuid) {
        if (ownerUuid == null) {
            return 0;
        }
        String sql = "SELECT COUNT(*) FROM lands WHERE owner_uuid = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, ownerUuid.toString());
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to count owned lands: " + e.getMessage());
        }
        return 0;
    }

    public int createLand(String world, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        String sql = "INSERT INTO lands(world,min_x,min_y,min_z,max_x,max_y,max_z,owner_uuid,owner_name,created_at) VALUES (?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, world);
            statement.setInt(2, minX);
            statement.setInt(3, minY);
            statement.setInt(4, minZ);
            statement.setInt(5, maxX);
            statement.setInt(6, maxY);
            statement.setInt(7, maxZ);
            statement.setNull(8, Types.VARCHAR);
            statement.setNull(9, Types.VARCHAR);
            statement.setLong(10, Instant.now().getEpochSecond());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to create land: " + e.getMessage());
        }
        return -1;
    }

    public int deleteLandsOverlapping(String world, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        List<Integer> ids = new ArrayList<>();
        String findSql = "SELECT id FROM lands WHERE world = ? "
            + "AND MIN(min_x, max_x) <= ? AND MAX(min_x, max_x) >= ? "
            + "AND MIN(min_y, max_y) <= ? AND MAX(min_y, max_y) >= ? "
            + "AND MIN(min_z, max_z) <= ? AND MAX(min_z, max_z) >= ?";
        try (PreparedStatement find = connection.prepareStatement(findSql)) {
            find.setString(1, world);
            find.setInt(2, maxX);
            find.setInt(3, minX);
            find.setInt(4, maxY);
            find.setInt(5, minY);
            find.setInt(6, maxZ);
            find.setInt(7, minZ);
            try (ResultSet rs = find.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getInt("id"));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to query overlapping lands: " + e.getMessage());
            return 0;
        }

        if (ids.isEmpty()) {
            return 0;
        }

        int deleted = 0;
        String memberSql = "DELETE FROM land_members WHERE land_id = ?";
        String landSql = "DELETE FROM lands WHERE id = ?";
        try (PreparedStatement member = connection.prepareStatement(memberSql);
             PreparedStatement land = connection.prepareStatement(landSql)) {
            for (int id : ids) {
                member.setInt(1, id);
                member.executeUpdate();
                land.setInt(1, id);
                deleted += land.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to delete lands: " + e.getMessage());
        }

        return deleted;
    }

    public boolean updateOwner(int landId, UUID ownerUuid, String ownerName) {
        String sql = "UPDATE lands SET owner_uuid = ?, owner_name = ? WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            if (ownerUuid == null) {
                statement.setNull(1, Types.VARCHAR);
                statement.setNull(2, Types.VARCHAR);
            } else {
                statement.setString(1, ownerUuid.toString());
                statement.setString(2, ownerName);
            }
            statement.setInt(3, landId);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to update owner: " + e.getMessage());
        }
        return false;
    }

    public boolean clearMembers(int landId) {
        String sql = "DELETE FROM land_members WHERE land_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, landId);
            statement.executeUpdate();
            return true;
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to clear members: " + e.getMessage());
        }
        return false;
    }

    public boolean addMember(int landId, UUID memberUuid, String memberName) {
        String sql = "INSERT INTO land_members(land_id,member_uuid,member_name) VALUES (?,?,?) ON CONFLICT(land_id,member_uuid) DO UPDATE SET member_name=excluded.member_name";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, landId);
            statement.setString(2, memberUuid.toString());
            statement.setString(3, memberName);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to add member: " + e.getMessage());
        }
        return false;
    }

    public boolean removeMember(int landId, UUID memberUuid) {
        String sql = "DELETE FROM land_members WHERE land_id = ? AND member_uuid = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, landId);
            statement.setString(2, memberUuid.toString());
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to remove member: " + e.getMessage());
        }
        return false;
    }

    public Map<Integer, Set<UUID>> getAllMembers() {
        String sql = "SELECT land_id, member_uuid FROM land_members";
        Map<Integer, Set<UUID>> members = new HashMap<>();
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                int landId = rs.getInt("land_id");
                UUID memberUuid = UUID.fromString(rs.getString("member_uuid"));
                members.computeIfAbsent(landId, ignored -> new HashSet<>()).add(memberUuid);
            }
        } catch (SQLException | IllegalArgumentException e) {
            plugin.getLogger().severe("Failed to load all land members: " + e.getMessage());
        }
        return members;
    }

    public Set<UUID> getMembers(int landId) {
        String sql = "SELECT member_uuid FROM land_members WHERE land_id = ?";
        Set<UUID> members = new HashSet<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, landId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    members.add(UUID.fromString(rs.getString("member_uuid")));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get members: " + e.getMessage());
        }
        return members;
    }

    private Land mapLand(ResultSet rs) throws SQLException {
        String ownerUuidRaw = rs.getString("owner_uuid");
        UUID ownerUuid = ownerUuidRaw == null ? null : UUID.fromString(ownerUuidRaw);
        return new Land(
            rs.getInt("id"),
            rs.getString("world"),
            rs.getInt("min_x"),
            rs.getInt("min_y"),
            rs.getInt("min_z"),
            rs.getInt("max_x"),
            rs.getInt("max_y"),
            rs.getInt("max_z"),
            ownerUuid,
            rs.getString("owner_name")
        );
    }

    public void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ignored) {
            }
        }
    }
}
