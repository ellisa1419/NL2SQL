package com.acme.nl2sql.db;

import org.sqlite.SQLiteDataSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Owns the SQLite connection settings and hands out connections.
 *
 * <p>Connections are opened per unit of work and closed by the caller. SQLite is a local
 * file, so opening is cheap and concurrent readers do not block each other.
 */
public final class Database {

    private static final String FILE_URL_PREFIX = "jdbc:sqlite:";

    private final SQLiteDataSource dataSource;

    /**
     * @param jdbcUrl a SQLite JDBC URL such as {@code jdbc:sqlite:data/analytics.db}
     */
    public Database(String jdbcUrl) {
        ensureParentDirectory(jdbcUrl);
        SQLiteDataSource ds = new SQLiteDataSource();
        ds.setUrl(jdbcUrl);
        this.dataSource = ds;
    }

    /**
     * Opens a new connection in autocommit mode.
     *
     * @return an open connection the caller must close
     * @throws SQLException if the database file cannot be opened
     */
    public Connection open() throws SQLException {
        return dataSource.getConnection();
    }

    /**
     * Creates the directory holding the database file when the URL points at a path,
     * so that a first run on a clean checkout does not fail.
     *
     * @param jdbcUrl the configured JDBC URL
     */
    private void ensureParentDirectory(String jdbcUrl) {
        if (!jdbcUrl.startsWith(FILE_URL_PREFIX)) {
            return;
        }
        String path = jdbcUrl.substring(FILE_URL_PREFIX.length());
        if (path.isBlank() || path.startsWith(":") || path.contains("mode=memory")) {
            return;
        }
        Path parent = Path.of(path).toAbsolutePath().getParent();
        if (parent == null) {
            return;
        }
        try {
            Files.createDirectories(parent);
        } catch (Exception e) {
            throw new IllegalStateException("could not create database directory " + parent, e);
        }
    }
}
