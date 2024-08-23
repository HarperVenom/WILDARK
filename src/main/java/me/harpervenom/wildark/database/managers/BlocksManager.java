package me.harpervenom.wildark.database.managers;

import org.bukkit.block.Block;

import java.sql.*;

public class BlocksManager {

    private Connection connection;

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

    public boolean logBlock(String playerUUID, int x, int y, int z, String world) {
        String sql = "INSERT INTO blocks (owner_id, x, y, z, world) VALUES " +
                "(?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, playerUUID);
            ps.setInt(2,x);
            ps.setInt(3,y);
            ps.setInt(4,z);
            ps.setString(5, world);
            ps.executeUpdate();
            return true;
        }catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public String getOwner(Block b) {
        int x = b.getX();
        int y = b.getY();
        int z = b.getZ();
        String world = b.getWorld().getName();
        String sql = "SELECT owner_id FROM blocks WHERE x = ? AND y = ? AND z = ? AND world = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, x);
            ps.setInt(2, y);
            ps.setInt(3, z);
            ps.setString(4, world);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("owner_id");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void deleteBlockRecord(Block b) {
        int x = b.getX();
        int y = b.getY();
        int z = b.getZ();
        String world = b.getWorld().getName();
        String sql = "DELETE FROM blocks WHERE x = ? AND y = ? AND z = ? AND world = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, x);
            ps.setInt(2, y);
            ps.setInt(3, z);
            ps.setString(4, world);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
