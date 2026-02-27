package com.clutch.land.infrastructure;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class Database {
    private final Path sqlitePath;
    private Connection connection;

    public Database(Path sqlitePath) {
        this.sqlitePath = Objects.requireNonNull(sqlitePath, "sqlitePath");
    }

    public synchronized void connect() {
        try {
            Files.createDirectories(sqlitePath.getParent());
            String jdbcUrl = "jdbc:sqlite:" + sqlitePath;
            this.connection = DriverManager.getConnection(jdbcUrl);
            try (Statement pragma = this.connection.createStatement()) {
                pragma.execute("PRAGMA foreign_keys = ON");
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to connect sqlite", e);
        }
    }

    public synchronized void migrate() {
        ensureConnected();

        try {
            createMigrationsTable();
            int currentVersion = currentVersion();

            List<MigrationScript> scripts = discoverScripts();
            for (MigrationScript script : scripts) {
                if (script.version() <= currentVersion) {
                    continue;
                }
                applyMigration(script);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Migration failed", e);
        }
    }

    public synchronized Connection getConnection() {
        ensureConnected();
        return connection;
    }

    public synchronized void close() {
        if (connection == null) {
            return;
        }
        try {
            connection.close();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to close sqlite", e);
        } finally {
            connection = null;
        }
    }

    private void ensureConnected() {
        if (connection == null) {
            throw new IllegalStateException("Database connection is not initialized.");
        }
    }

    private void createMigrationsTable() throws SQLException {
        try (Statement st = connection.createStatement()) {
            st.execute("""
                    CREATE TABLE IF NOT EXISTS migrations (
                      version INTEGER PRIMARY KEY,
                      name TEXT NOT NULL,
                      applied_at INTEGER NOT NULL
                    )
                    """);
        }
    }

    private int currentVersion() throws SQLException {
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("SELECT COALESCE(MAX(version), 0) AS version FROM migrations")) {
            return rs.next() ? rs.getInt("version") : 0;
        }
    }

    private List<MigrationScript> discoverScripts() {
        List<MigrationScript> scripts = new ArrayList<>();
        scripts.add(new MigrationScript(1, "V1__init.sql", loadClasspathSql("db/migration/V1__init.sql")));
        scripts.sort(Comparator.comparingInt(MigrationScript::version));
        return scripts;
    }

    private void applyMigration(MigrationScript script) throws SQLException {
        connection.setAutoCommit(false);
        try (Statement statement = connection.createStatement()) {
            statement.execute(script.sql());
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO migrations(version, name, applied_at) VALUES (?, ?, ?)")
            ) {
                ps.setInt(1, script.version());
                ps.setString(2, script.name());
                ps.setLong(3, System.currentTimeMillis());
                ps.executeUpdate();
            }
            connection.commit();
        } catch (Exception e) {
            connection.rollback();
            throw new IllegalStateException("Failed to apply migration: " + script.name(), e);
        } finally {
            connection.setAutoCommit(true);
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

    private record MigrationScript(int version, String name, String sql) {
    }
}
