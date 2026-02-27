package com.clutch.land.infrastructure;

import com.clutch.land.domain.LandMember;
import com.clutch.land.domain.LandRole;
import com.clutch.land.repository.LandMemberRepository;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class SqliteLandMemberRepository implements LandMemberRepository {
    private final Database database;

    public SqliteLandMemberRepository(Database database) {
        this.database = database;
    }

    @Override
    public List<LandMember> loadAll() {
        String sql = "SELECT land_id, member_uuid, role FROM land_members";
        try (PreparedStatement ps = database.getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<LandMember> rows = new ArrayList<>();
            while (rs.next()) {
                String memberRaw = rs.getString("member_uuid");
                String roleRaw = rs.getString("role");
                UUID memberUuid = UUID.fromString(memberRaw);
                LandRole role = parseRole(roleRaw);
                rows.add(new LandMember(rs.getInt("land_id"), memberUuid, role));
            }
            return rows;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load land members", e);
        }
    }

    @Override
    public void addMember(int landId, UUID memberUuid, String role) {
        String sql = "INSERT OR REPLACE INTO land_members(land_id, member_uuid, role, added_at) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = database.getConnection().prepareStatement(sql)) {
            ps.setInt(1, landId);
            ps.setString(2, memberUuid.toString());
            ps.setString(3, role);
            ps.setLong(4, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to add land member", e);
        }
    }

    @Override
    public void removeMember(int landId, UUID memberUuid) {
        String sql = "DELETE FROM land_members WHERE land_id = ? AND member_uuid = ?";
        try (PreparedStatement ps = database.getConnection().prepareStatement(sql)) {
            ps.setInt(1, landId);
            ps.setString(2, memberUuid.toString());
            ps.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to remove land member", e);
        }
    }

    @Override
    public void removeAllByLand(int landId) {
        try (PreparedStatement ps = database.getConnection().prepareStatement("DELETE FROM land_members WHERE land_id = ?")) {
            ps.setInt(1, landId);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to remove land members", e);
        }
    }

    private LandRole parseRole(String roleRaw) {
        if (roleRaw == null || roleRaw.isBlank()) {
            return LandRole.MEMBER;
        }
        try {
            return LandRole.valueOf(roleRaw.toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return LandRole.MEMBER;
        }
    }
}
