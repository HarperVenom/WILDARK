package me.harpervenom.wildark.database.managers;

import me.harpervenom.wildark.classes.WildPlayer;

import java.sql.*;
import java.util.UUID;

public class PlayersManager {

    private final Connection connection;

    public PlayersManager(Connection connection) throws SQLException {
        this.connection = connection;

            try (Statement statement = connection.createStatement()) {
                String sql = "CREATE TABLE IF NOT EXISTS players (" +
                        "id TEXT NOT NULL, " +
                        "available_blocks INTEGER NOT NULL, " +
                        "available_regions INTEGER NOT NULL, " +
                        "minutes_played INTEGER NOT NULL, " +
                        "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP)";
                statement.executeUpdate(sql);
            }

    }

    public void create(UUID id) {
        String sql = "INSERT INTO players (id, available_blocks, available_regions, minutes_played) VALUES " +
                "(?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, id.toString());
            ps.setInt(2,16);
            ps.setInt(3,1);
            ps.setInt(4,0);
            ps.executeUpdate();
        }catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public WildPlayer getPlayer(String id) {
        String sql = "SELECT * FROM players WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new WildPlayer(rs.getString("id"),
                            rs.getInt("available_blocks"),
                            rs.getInt("available_regions"),
                            rs.getInt("minutes_played"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}

