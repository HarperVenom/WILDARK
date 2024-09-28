package me.harpervenom.wildark.database.managers;

import me.harpervenom.wildark.classes.Region;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class RegionsManager {

    private Connection connection;

    public RegionsManager(Connection connection) throws SQLException {
        this.connection = connection;

        try (Statement statement = connection.createStatement()) {
            String sql = "CREATE TABLE IF NOT EXISTS regions (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "name TEXT NOT NULL, " +
                    "x1 INTEGER NOT NULL, " +
                    "z1 INTEGER NOT NULL, " +
                    "x2 INTEGER NOT NULL, " +
                    "z2 INTEGER NOT NULL, " +
                    "world TEXT NOT NULL, " +
                    "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP)";
            statement.executeUpdate(sql);

//            PLAYERS_REGIONS
            sql = "CREATE TABLE IF NOT EXISTS players_regions (" +
                    "player_id TEXT NOT NULL, " +
                    "region_id INTEGER NOT NULL, " +
                    "relation TEXT NOT NULL, " +
                    "FOREIGN KEY (player_id) REFERENCES players(id), " +
                    "FOREIGN KEY (region_id) REFERENCES regions(id), " +
                    "PRIMARY KEY (player_id, region_id))";
            statement.executeUpdate(sql);
        }
    }

    public CompletableFuture<Region> createRegion(Region region) {
        Player p = region.getOwner();
        return getPlayerRegions(p).thenCompose(ownRegions -> CompletableFuture.supplyAsync(() -> {
            String regionName = p.getName() + (!ownRegions.isEmpty() ? "(" + ownRegions.size() + ")" : "");

            String regionSql = "INSERT INTO regions (name, x1, z1, x2, z2, world) VALUES " +
                    "(?, ?, ?, ?, ?, ?)";
            String relationSql = "INSERT INTO players_regions (player_id, region_id, relation) VALUES " +
                    "(?, ?, ?)";

            int x1 = Math.min(region.getX1(), region.getX2());
            int z1 = Math.min(region.getZ1(), region.getZ2());
            int x2 = Math.max(region.getX1(), region.getX2());
            int z2 = Math.max(region.getZ1(), region.getZ2());

            try (PreparedStatement psRegion = connection.prepareStatement(regionSql, Statement.RETURN_GENERATED_KEYS)) {
                psRegion.setString(1, regionName);
                psRegion.setInt(2, x1);
                psRegion.setInt(3, z1);
                psRegion.setInt(4, x2);
                psRegion.setInt(5, z2);
                psRegion.setString(6, region.getWorldName());
                psRegion.executeUpdate();

                try (ResultSet rs = psRegion.getGeneratedKeys()) {
                    if (rs.next()) {
                        int regionId = rs.getInt(1);

                        try (PreparedStatement psRelation = connection.prepareStatement(relationSql)) {
                            psRelation.setString(1, p.getUniqueId().toString());
                            psRelation.setInt(2, regionId);
                            psRelation.setString(3, "owner");
                            psRelation.executeUpdate();
                        }

                        return new Region(
                                regionId, p,
                                regionName,
                                region.getWorldName(),
                                region.getX1(),
                                region.getZ1(),
                                region.getX2(),
                                region.getZ2()
                        );
                    } else {
                        throw new SQLException("Creating player_region relation failed, no ID obtained.");
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
                return null;
            }
        }));
    }

    public CompletableFuture<Boolean> updateRegion(Region region){
        return CompletableFuture.supplyAsync(() -> {
            String sql = "UPDATE regions SET x1 = ?, z1 = ?, x2 = ?, z2 = ?, timestamp = CURRENT_TIMESTAMP WHERE id = ?";

            int x1 = Math.min(region.getX1(), region.getX2());
            int z1 = Math.min(region.getZ1(), region.getZ2());
            int x2 = Math.max(region.getX1(), region.getX2());
            int z2 = Math.max(region.getZ1(), region.getZ2());

            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setInt(1, x1);
                pstmt.setInt(2, z1);
                pstmt.setInt(3, x2);
                pstmt.setInt(4, z2);
                pstmt.setInt(5, region.getId());

                int rowsAffected = pstmt.executeUpdate();
                return rowsAffected > 0;
            } catch (SQLException e) {
                System.out.println(e.getMessage());
                return false;
            }
        });
    }


    public CompletableFuture<List<Region>> getPlayerRegions(Player p) {
        return CompletableFuture.supplyAsync(() -> {
            String query = "SELECT * FROM players_regions WHERE player_id = ?";
            String regionsQuery = "SELECT * FROM regions WHERE id = ?";

            List<Region> regions = new ArrayList<>();
            try (PreparedStatement ps = connection.prepareStatement(query)) {
                ps.setString(1,p.getUniqueId().toString());

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        int region_id = rs.getInt("region_id");
                        String playerId = rs.getString("player_id");

                        try (PreparedStatement regionsPs = connection.prepareStatement(regionsQuery)) {
                            regionsPs.setInt(1,region_id);

                            try (ResultSet regionsRs = regionsPs.executeQuery()) {
                                Region region = new Region(
                                        regionsRs.getInt("id"),
                                        Bukkit.getPlayer(UUID.fromString(playerId)),
                                        regionsRs.getString("name"),
                                        regionsRs.getString("world"),
                                        regionsRs.getInt("x1"),
                                        regionsRs.getInt("z1"),
                                        regionsRs.getInt("x2"),
                                        regionsRs.getInt("z2")
                                );
                                regions.add(region);
                            }
                        }
                    }
                }

            } catch (SQLException e) {
                e.printStackTrace();
                return Collections.emptyList();
            }

            return regions;
        });
    }

    public Player getRegionOwner(int regionId) {
        String query = "SELECT player_id FROM players_regions WHERE region_id = ? AND relation = 'owner'";

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, regionId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Bukkit.getPlayer(UUID.fromString(rs.getString("player_id")));
                } else {
                    return null; // No owner found
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

//    public Region getBlockRegion(Block b) {
//        String query = "SELECT * FROM regions WHERE world = ? AND x1 <= ? AND x2 >= ? AND z1 <= ? AND z2 >= ?";
//        try (PreparedStatement ps = connection.prepareStatement(query)){
//            ps.setString(1, b.getWorld().getName());
//            ps.setInt(2, b.getX());
//            ps.setInt(3, b.getX());
//            ps.setInt(4, b.getZ());
//            ps.setInt(5, b.getZ());
//
//            try (ResultSet rs = ps.executeQuery()) {
//                if (rs.next()){
//                    return new Region(
//                            rs.getInt("id"),
//                            getRegionOwner(rs.getInt("id")),
//                            rs.getString("name"),
//                            rs.getString("world"),
//                            rs.getInt("x1"),
//                            rs.getInt("z1"),
//                            rs.getInt("x2"),
//                            rs.getInt("z2"),
//                            "blue");
//                }
//                return null;
//            }
//
//        } catch (SQLException e){
//            e.printStackTrace();
//            return null;
//        }
//
//    }

    public CompletableFuture<String> regionStatus(Region region) {
        return scanArea(region.getWorldName(), region.getX1(), region.getZ1(), region.getX2(), region.getZ2(), 10).thenApply(regions -> {
            regions = regions.stream().filter(currentRegion -> currentRegion.getId() != (region.getId())).collect(Collectors.toList());

            if (regions.isEmpty()) return "ok";

            for (Region value : regions) {
                if (regionsIntersect(region, value)) return "intersect";
            }
            for (Region value : regions) {
                if (value.getArea() > region.getArea()) return "close";
            }

            return "ok";
        });
    }

    public CompletableFuture<List<Region>> scanArea(String worldName, int x1, int z1, int x2, int z2, int radius) {
        int minX = Math.min(x1, x2) - radius;
        int maxX = Math.max(x1, x2) + radius;
        int minZ = Math.min(z1, z2) - radius;
        int maxZ = Math.max(z1, z2) + radius;

        return scanArea(worldName, minX, minZ, maxX, maxZ);
    }

    public CompletableFuture<List<Region>> scanArea(String worldName, int x, int z, int radius) {
        int minX = x - radius;
        int maxX = x + radius;
        int minZ = z - radius;
        int maxZ = z + radius;

        return scanArea(worldName, minX, minZ, maxX, maxZ);
    }

    public CompletableFuture<List<Region>> scanArea(String worldName, int minX, int minZ, int maxX, int maxZ) {
        return CompletableFuture.supplyAsync(() -> {
            String query = "SELECT * FROM regions WHERE world = ? AND " +
                    "NOT (x2 < ? OR ? < x1 OR z2 < ? OR ? < z1)";

            String playerQuery = "SELECT player_id FROM players_regions WHERE region_id = ?";

            try (PreparedStatement ps = connection.prepareStatement(query)) {
                ps.setString(1, worldName);
                ps.setInt(2, minX);
                ps.setInt(3, maxX);
                ps.setInt(4, minZ);
                ps.setInt(5, maxZ);

                try (ResultSet rs = ps.executeQuery()) {
                    List<Region> regions = new ArrayList<>();

                    while (rs.next()) {
                        int regionId = rs.getInt("id");
                        String playerId = null;

                        try (PreparedStatement playerPs = connection.prepareStatement(playerQuery)) {
                            playerPs.setInt(1, regionId);
                            try (ResultSet playerRs = playerPs.executeQuery()) {
                                if (playerRs.next()) {
                                    playerId = playerRs.getString("player_id");
                                }
                            }
                        }

                        if (playerId != null) {
                            Region region = new Region(
                                    rs.getInt("id"),
                                    Bukkit.getPlayer(UUID.fromString(playerId)),
                                    rs.getString("name"),
                                    rs.getString("world"),
                                    rs.getInt("x1"),
                                    rs.getInt("z1"),
                                    rs.getInt("x2"),
                                    rs.getInt("z2")
                            );
                            regions.add(region);
                        }
                    }
                    return regions;
                }
            } catch (SQLException e) {
                e.printStackTrace();
                return Collections.emptyList();
            }
        });

    }


    private boolean regionsIntersect(Region region1, Region region2) {
        // Get coordinates for the first region
        int x1_A = Math.min(region1.getX1(), region1.getX2());
        int x2_A = Math.max(region1.getX1(), region1.getX2());
        int z1_A = Math.min(region1.getZ1(), region1.getZ2());
        int z2_A = Math.max(region1.getZ1(), region1.getZ2());

        // Get coordinates for the second region
        int x1_B = Math.min(region2.getX1(), region2.getX2());
        int x2_B = Math.max(region2.getX1(), region2.getX2());
        int z1_B = Math.min(region2.getZ1(), region2.getZ2());
        int z2_B = Math.max(region2.getZ1(), region2.getZ2());

        // Check if one region is completely to the left of the other
        if (x2_A < x1_B || x2_B < x1_A) {
            return false;
        }

        // Check if one region is completely above the other
        if (z2_A < z1_B || z2_B < z1_A) {
            return false;
        }

        // If neither of the above conditions is true, the regions intersect
        return true;
    }
}
