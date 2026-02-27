package com.clutch.land.infrastructure;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
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
            this.connection.createStatement().execute("PRAGMA foreign_keys = ON");
        } catch (Exception e) {
            throw new IllegalStateException("Failed to connect sqlite", e);
        }
    }

    public synchronized Connection getConnection() {
        if (connection == null) {
            throw new IllegalStateException("Database connection is not initialized.");
        }
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
}
