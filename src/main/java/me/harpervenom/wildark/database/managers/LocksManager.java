package me.harpervenom.wildark.database.managers;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class LocksManager {

    private Connection connection;

    public LocksManager(Connection connection) throws SQLException {
        this.connection = connection;

        try (Statement statement = connection.createStatement()) {
            String sql = "CREATE TABLE IF NOT EXISTS locks (" +
                    "owner_id TEXT PRIMARY KEY NOT NULL, " +
                    "x INTEGER NOT NULL, " +
                    "y INTEGER NOT NULL, " +
                    "z INTEGER NOT NULL, " +
                    "world TEXT NOT NULL, " +
                    "locked BOOLEAN NOT NULL, " +
                    "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                    "FOREIGN KEY (owner_id) REFERENCES players(id))";
            statement.executeUpdate(sql);
        }
    }
}
