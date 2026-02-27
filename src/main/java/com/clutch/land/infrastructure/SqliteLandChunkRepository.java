package com.clutch.land.infrastructure;

import com.clutch.land.domain.ChunkKey;
import com.clutch.land.repository.LandChunkRepository;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class SqliteLandChunkRepository implements LandChunkRepository {
    private final Database database;

    public SqliteLandChunkRepository(Database database) {
        this.database = database;
    }

    @Override
    public List<LandChunkRow> loadAll() {
        String sql = "SELECT world_id, chunk_x, chunk_z, land_id FROM land_chunks";
        try (PreparedStatement ps = database.getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<LandChunkRow> rows = new ArrayList<>();
            while (rs.next()) {
                ChunkKey chunkKey = new ChunkKey(
                        rs.getString("world_id"),
                        rs.getInt("chunk_x"),
                        rs.getInt("chunk_z")
                );
                rows.add(new LandChunkRow(chunkKey, rs.getInt("land_id")));
            }
            return rows;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load land chunks", e);
        }
    }

    @Override
    public void replaceLandChunks(int landId, Set<ChunkKey> chunks) {
        removeLandChunks(landId);

        String sql = "INSERT INTO land_chunks(world_id, chunk_x, chunk_z, land_id) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = database.getConnection().prepareStatement(sql)) {
            for (ChunkKey chunk : chunks) {
                ps.setString(1, chunk.worldId());
                ps.setInt(2, chunk.chunkX());
                ps.setInt(3, chunk.chunkZ());
                ps.setInt(4, landId);
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to replace land chunks", e);
        }
    }

    @Override
    public void removeLandChunks(int landId) {
        try (PreparedStatement ps = database.getConnection().prepareStatement("DELETE FROM land_chunks WHERE land_id = ?")) {
            ps.setInt(1, landId);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to remove land chunks", e);
        }
    }
}
