package com.clutch.land.infrastructure;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class MigrationRunner {
    private final Database database;

    public MigrationRunner(Database database) {
        this.database = database;
    }

    public void migrate() {
        try {
            createVersionTable();
            List<MigrationScript> scripts = discoverScripts();
            int currentVersion = currentVersion();

            for (MigrationScript script : scripts) {
                if (script.version() <= currentVersion) {
                    continue;
                }
                apply(script);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Migration failed", e);
        }
    }

    private void createVersionTable() throws Exception {
        try (Statement st = database.getConnection().createStatement()) {
            st.execute("""
                    CREATE TABLE IF NOT EXISTS schema_version (
                      version INTEGER PRIMARY KEY,
                      description TEXT NOT NULL,
                      applied_at INTEGER NOT NULL
                    )
                    """);
        }
    }

    private int currentVersion() throws Exception {
        try (Statement st = database.getConnection().createStatement();
             ResultSet rs = st.executeQuery("SELECT COALESCE(MAX(version), 0) AS version FROM schema_version")) {
            return rs.next() ? rs.getInt("version") : 0;
        }
    }

    private List<MigrationScript> discoverScripts() {
        List<MigrationScript> scripts = new ArrayList<>();
        scripts.add(new MigrationScript(1, "init", loadClasspathSql("db/migration/V1__init.sql")));
        scripts.sort(Comparator.comparingInt(MigrationScript::version));
        return scripts;
    }

    private void apply(MigrationScript script) throws Exception {
        try (Statement st = database.getConnection().createStatement()) {
            st.execute("BEGIN");
            st.execute(script.sql());
            try (PreparedStatement ps = database.getConnection().prepareStatement(
                    "INSERT INTO schema_version(version, description, applied_at) VALUES (?, ?, ?)")
            ) {
                ps.setInt(1, script.version());
                ps.setString(2, script.description());
                ps.setLong(3, System.currentTimeMillis());
                ps.executeUpdate();
            }
            st.execute("COMMIT");
        } catch (Exception e) {
            try (Statement rollback = database.getConnection().createStatement()) {
                rollback.execute("ROLLBACK");
            }
            throw e;
        }
    }

    private String loadClasspathSql(String path) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try (InputStream inputStream = classLoader.getResourceAsStream(path)) {
            if (inputStream == null) {
                throw new IllegalStateException("Migration resource not found: " + path);
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append('\n');
                }
                return sb.toString();
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load migration: " + path, e);
        }
    }

    private record MigrationScript(int version, String description, String sql) {
    }
}
