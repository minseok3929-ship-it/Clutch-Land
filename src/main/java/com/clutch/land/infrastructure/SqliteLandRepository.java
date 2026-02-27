package com.clutch.land.infrastructure;

import com.clutch.land.domain.Land;
import com.clutch.land.repository.LandRepository;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public final class SqliteLandRepository implements LandRepository {
    private final Database database;

    public SqliteLandRepository(Database database) {
        this.database = database;
    }

    @Override
    public List<Land> loadAll() {
        String sql = """
                SELECT id, world_id, min_x, max_x, min_y, max_y, min_z, max_z,
                       owner_uuid, grade, flags
                FROM lands
                """;
        try (PreparedStatement ps = database.getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Land> lands = new ArrayList<>();
            while (rs.next()) {
                lands.add(new Land(
                        rs.getInt("id"),
                        rs.getString("world_id"),
                        rs.getInt("min_x"),
                        rs.getInt("max_x"),
                        rs.getInt("min_y"),
                        rs.getInt("max_y"),
                        rs.getInt("min_z"),
                        rs.getInt("max_z"),
                        rs.getString("owner_uuid"),
                        rs.getInt("grade"),
                        rs.getString("flags")
                ));
            }
            return lands;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load lands", e);
        }
    }

    @Override
    public int insert(Land land) {
        String sql = """
                INSERT INTO lands(world_id, min_x, max_x, min_y, max_y, min_z, max_z,
                                  owner_uuid, created_at, updated_at, grade, flags)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        long now = System.currentTimeMillis();
        try (PreparedStatement ps = database.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, land.worldId());
            ps.setInt(2, land.minX());
            ps.setInt(3, land.maxX());
            ps.setInt(4, land.minY());
            ps.setInt(5, land.maxY());
            ps.setInt(6, land.minZ());
            ps.setInt(7, land.maxZ());
            ps.setString(8, land.ownerUuid());
            ps.setLong(9, now);
            ps.setLong(10, now);
            ps.setInt(11, land.grade());
            ps.setString(12, land.flagsJson());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
            throw new IllegalStateException("Insert land succeeded but no generated key returned");
        } catch (Exception e) {
            throw new IllegalStateException("Failed to insert land", e);
        }
    }

    @Override
    public void delete(int landId) {
        try (PreparedStatement ps = database.getConnection().prepareStatement("DELETE FROM lands WHERE id = ?")) {
            ps.setInt(1, landId);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to delete land", e);
        }
    }

    @Override
    public void updateOwner(int landId, String ownerUuid) {
        String sql = "UPDATE lands SET owner_uuid = ?, updated_at = ? WHERE id = ?";
        try (PreparedStatement ps = database.getConnection().prepareStatement(sql)) {
            ps.setString(1, ownerUuid);
            ps.setLong(2, System.currentTimeMillis());
            ps.setInt(3, landId);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to update land owner", e);
        }
    }

    @Override
    public void updateBounds(int landId, int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
        String sql = """
                UPDATE lands
                SET min_x = ?, max_x = ?, min_y = ?, max_y = ?, min_z = ?, max_z = ?, updated_at = ?
                WHERE id = ?
                """;
        try (PreparedStatement ps = database.getConnection().prepareStatement(sql)) {
            ps.setInt(1, minX);
            ps.setInt(2, maxX);
            ps.setInt(3, minY);
            ps.setInt(4, maxY);
            ps.setInt(5, minZ);
            ps.setInt(6, maxZ);
            ps.setLong(7, System.currentTimeMillis());
            ps.setInt(8, landId);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to update land bounds", e);
        }
    }
}
