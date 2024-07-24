package me.harpervenom.wildark.database.managers;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class PlayersManager {

    private Connection connection;

    public PlayersManager(Connection connection) throws SQLException {
        this.connection = connection;

            try (Statement statement = connection.createStatement()) {
                String sql = "CREATE TABLE IF NOT EXISTS players (" +
                        "id TEXT NOT NULL, " +
                        "free_blocks INTEGER NOT NULL, " +
                        "minutes_played INTEGER NOT NULL, " +
                        "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP)";
                statement.executeUpdate(sql);
            }

    }
}
