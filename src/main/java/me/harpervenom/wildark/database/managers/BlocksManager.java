package me.harpervenom.wildark.database.managers;

import me.harpervenom.wildark.classes.WildBlock;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class BlocksManager {

    private final Connection connection;

    public BlocksManager(Connection connection) throws SQLException {
        this.connection = connection;

        try (Statement statement = connection.createStatement()) {
            String sql = "CREATE TABLE IF NOT EXISTS blocks (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "owner_id TEXT NOT NULL, " +
                    "x INTEGER NOT NULL, " +
                    "y INTEGER NOT NULL, " +
                    "z INTEGER NOT NULL, " +
                    "world TEXT NOT NULL, " +
                    "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                    "FOREIGN KEY (owner_id) REFERENCES players(id))";
            statement.executeUpdate(sql);
        }

    }

    public CompletableFuture<Integer> logBlock(String playerUUID, int x, int y, int z, String world) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "INSERT INTO blocks (owner_id, x, y, z, world) VALUES " +
                    "(?, ?, ?, ?, ?)";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, playerUUID);
                ps.setInt(2,x);
                ps.setInt(3,y);
                ps.setInt(4,z);
                ps.setString(5, world);

                int affectedRows = ps.executeUpdate();

                if (affectedRows > 0) {
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next()) {
                            return rs.getInt(1);
                        }
                    }
                }

                return null;
            }catch (SQLException e) {
                e.printStackTrace();
                return null;
            }
        });
    }

    public CompletableFuture<Boolean> updateBlockLoc(int id, Location newLocation) {
        return CompletableFuture.supplyAsync(() -> {
            String updateQuery = "UPDATE blocks SET x = ?, y = ?, z = ? WHERE id = ?";

            try (PreparedStatement ps = connection.prepareStatement(updateQuery)) {
                // Set new location
                ps.setInt(1, newLocation.getBlockX());
                ps.setInt(2, newLocation.getBlockY());
                ps.setInt(3, newLocation.getBlockZ());

                // Set id
                ps.setInt(4, id);

                ps.executeUpdate();
                return true;
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        });
    }

    public CompletableFuture<List<WildBlock>> getWildBlocks(int x1, int z1, int x2, int z2, String world) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM blocks WHERE x BETWEEN ? AND ? AND z BETWEEN ? AND ? AND world = ?";
            List<WildBlock> blocks = new ArrayList<>();

            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setInt(1, Math.min(x1, x2));
                ps.setInt(2, Math.max(x1, x2));
                ps.setInt(3, Math.min(z1, z2));
                ps.setInt(4, Math.max(z1, z2));
                ps.setString(5, world);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        WildBlock block = new WildBlock(rs.getInt("id"), new Location(Bukkit.getWorld(rs.getString("world")), rs.getInt("x"),
                                rs.getInt("y"),
                                rs.getInt("z")), rs.getString("owner_id"));

                        blocks.add(block);
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return blocks;
        });
    }

    public CompletableFuture<Boolean> deleteBlockRecord(int id) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "DELETE FROM blocks WHERE id = ?";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setInt(1, id);
                ps.executeUpdate();
                return true;
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        });
    }
}
