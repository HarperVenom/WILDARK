package me.harpervenom.wildark.database.managers;

import me.harpervenom.wildark.classes.Region;
import me.harpervenom.wildark.classes.WildPlayer;

import java.sql.*;

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
                    "player_id TEXT NOY NULL, " +
                    "region_id INTEGER NOT NULL, " +
                    "relation TEXT NOT NULL, " +
                    "FOREIGN KEY (player_id) REFERENCES players(id), " +
                    "FOREIGN KEY (region_id) REFERENCES regions(id), " +
                    "PRIMARY KEY (player_id, region_id))";
            statement.executeUpdate(sql);
        }
    }

    public boolean createRegion(WildPlayer p, Region region) {
        String regionSql = "INSERT INTO regions (x1, z1, x2, z2, world) VALUES " +
                "(?, ?, ?, ?, ?)";
        String relationSql = "INSERT INTO players_regions (player_id, region_id, relation) VALUES " +
                "(?, ?, ?)";
        try (PreparedStatement psRegion = connection.prepareStatement(regionSql)) {
            psRegion.setInt(1,(int)region.getFirstCorner().getX());
            psRegion.setInt(2,(int)region.getFirstCorner().getZ());
            psRegion.setInt(3,(int)region.getSecondCorner().getX());
            psRegion.setInt(4, (int)region.getSecondCorner().getZ());
            psRegion.setString(5, region.getFirstCorner().getWorld().getName());
            psRegion.executeUpdate();

            //Make relation between region and owner
            try (ResultSet generatedKeys = psRegion.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int regionId = generatedKeys.getInt(1);

                    try (PreparedStatement psRelation = connection.prepareStatement(relationSql)) {
                        psRelation.setString(1, p.getId());  // Assuming you have a method to get the player's ID
                        psRelation.setInt(2, regionId);
                        psRelation.setString(3, "owner");  // Assuming "owner" as the relation type; adjust as needed
                        psRelation.executeUpdate();
                    }
                } else {
                    throw new SQLException("Creating player_region relation failed, no ID obtained.");
                }
            }

            return true;
        }catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
