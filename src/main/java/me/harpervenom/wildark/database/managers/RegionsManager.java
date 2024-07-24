package me.harpervenom.wildark.database.managers;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class RegionsManager {

    private Connection connection;

    public RegionsManager(Connection connection) throws SQLException {
        this.connection = connection;

        try (Statement statement = connection.createStatement()) {
            String sql = "CREATE TABLE IF NOT EXISTS regions (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "x1 INTEGER NOT NULL, " +
                    "z1 INTEGER NOT NULL, " +
                    "x2 INTEGER NOT NULL, " +
                    "z2 INTEGER NOT NULL, " +
                    "world TEXT NOT NULL, " +
                    "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP)";
            statement.executeUpdate(sql);

//            PLAYERS_REGIONS
            sql = "CREATE TABLE IF NOT EXISTS players_regions (" +
                    "player_id INTEGER NOY NULL, " +
                    "region_id INTEGER NOT NULL, " +
                    "relation TEXT NOT NULL, " +
                    "FOREIGN KEY (player_id) REFERENCES players(id), " +
                    "FOREIGN KEY (regions_id) REFERENCES regions(id), " +
                    "PRIMARY KEY (player_id, region_id)";
            statement.executeUpdate(sql);
        }
    }
}
