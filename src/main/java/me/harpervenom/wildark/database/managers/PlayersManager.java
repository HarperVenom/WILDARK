package me.harpervenom.wildark.database.managers;

import me.harpervenom.wildark.classes.WildPlayer;
import org.bukkit.entity.Player;

import java.sql.*;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

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

    public CompletableFuture<WildPlayer> create(UUID id) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "INSERT INTO players (id, available_blocks, available_regions, minutes_played) VALUES " +
                    "(?, ?, ?, ?)";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                int availableBlocks = 0;
                int availableRegions = 0;
                int minutesPlayed = 0;

                ps.setString(1, id.toString());
                ps.setInt(2, availableBlocks);
                ps.setInt(3, availableRegions);
                ps.setInt(4, minutesPlayed);
                ps.executeUpdate();

                return new WildPlayer(id.toString(), availableBlocks, availableRegions, minutesPlayed);
            }catch (SQLException e) {
                e.printStackTrace();
                return null;
            }
        });
    }

    public CompletableFuture<WildPlayer> getPlayer(String id) {
        return CompletableFuture.supplyAsync(() -> {
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
        });
    }

    public boolean updateAvailableBlock(Player p, int number) {
        String sql = "UPDATE players SET available_blocks = available_blocks + ? WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, number);
            pstmt.setString(2, p.getUniqueId().toString());

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateAvailableRegions(Player p, int number) {
        String sql = "UPDATE players SET available_regions = available_regions + ? WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, number);
            pstmt.setString(2, p.getUniqueId().toString());

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}

