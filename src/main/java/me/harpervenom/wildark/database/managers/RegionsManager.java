package me.harpervenom.wildark.database.managers;

import me.harpervenom.wildark.classes.Region;
import me.harpervenom.wildark.classes.WildPlayer;
import org.bukkit.Bukkit;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

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
            psRegion.setInt(1,region.getX1());
            psRegion.setInt(2,region.getZ1());
            psRegion.setInt(3,region.getX2());
            psRegion.setInt(4, region.getZ2());
            psRegion.setString(5, region.getWorldName());
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

    public List<Region> scanArea(String worldName, int x, int z, int radius) {
        String query = "SELECT * FROM regions WHERE world = ? AND " +
                "((x1 <= ? AND x1 >= ?) OR (x2 <= ? AND x2 >= ?)) AND " +
                "((z1 <= ? AND z1 >= ?) OR (z2 <= ? AND z2 >= ?))";

        int scanAreaMinX = x - radius;
        int scanAreaMaxX = x + radius;
        int scanAreaMinZ = z - radius;
        int scanAreaMaxZ = z + radius;

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, worldName);      // world name
            ps.setInt(2, scanAreaMaxX);      // x2 of scan area
            ps.setInt(3, scanAreaMinX);      // x1 of scan area
            ps.setInt(4, scanAreaMaxX);      // x2 of scan area
            ps.setInt(5, scanAreaMinX);      // x1 of scan area
            ps.setInt(6, scanAreaMaxZ);      // z2 of scan area
            ps.setInt(7, scanAreaMinZ);      // z1 of scan area
            ps.setInt(8, scanAreaMaxZ);      // z2 of scan area
            ps.setInt(9, scanAreaMinZ);      // z1 of scan area


            try (ResultSet rs = ps.executeQuery()) {
                List<Region> regions = new ArrayList<>();
                while (rs.next()) {
                    Region region = new Region(
                            Bukkit.getPlayer(UUID.fromString(rs.getString("player_id"))),
                            rs.getString("world"),
                            rs.getInt("x1"),
                            rs.getInt("z1"),
                            rs.getInt("x2"),
                            rs.getInt("z2")
                    );
                    regions.add(region);
                }
                return regions;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }
}
