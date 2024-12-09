package me.harpervenom.wildark.database.managers;

import me.harpervenom.wildark.classes.WildPlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.*;
import java.util.HashSet;
import java.util.Map;
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
                        "accumulator INTEGER NOT NULL, " +
                        "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP)";
                statement.executeUpdate(sql);

                Map<String, String> newFields = Map.of(
                        "muted", "INTEGER DEFAULT 0" // Time left being muted
//                        ", banned", "INTEGER DEFAULT 0" // Time left being banned
                );

                // Check existing columns
                String checkColumnSql = "PRAGMA table_info(players);"; // For SQLite
                HashSet<String> existingColumns = new HashSet<>();
                try (ResultSet rs = statement.executeQuery(checkColumnSql)) {
                    while (rs.next()) {
                        existingColumns.add(rs.getString("name").toLowerCase());
                    }
                }

                // Add fields if they don't exist
                for (Map.Entry<String, String> field : newFields.entrySet()) {
                    if (!existingColumns.contains(field.getKey().toLowerCase())) {
                        String alterSql = "ALTER TABLE players ADD COLUMN " + field.getKey() + " " + field.getValue() + ";";
                        statement.executeUpdate(alterSql);
                    }
                }
            }
    }

    public CompletableFuture<WildPlayer> create(UUID id) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "INSERT INTO players (id, available_blocks, available_regions, accumulator, muted) VALUES " +
                    "(?, ?, ?, ?)";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                int availableBlocks = 0;
                int availableRegions = 0;
                int minutesPlayed = 0;
                int muted = 0;

                ps.setString(1, id.toString());
                ps.setInt(2, availableBlocks);
                ps.setInt(3, availableRegions);
                ps.setInt(4, minutesPlayed);
                ps.setInt(5, muted);
                ps.executeUpdate();

                return new WildPlayer(id.toString(), availableBlocks, availableRegions, minutesPlayed, muted);
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
                                rs.getInt("accumulator"),
                                rs.getInt("muted"));
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return null;
        });
    }

    public CompletableFuture<Boolean> updateAvailableBlock(String playedId, int number) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "UPDATE players SET available_blocks = available_blocks + ? WHERE id = ?";

            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setInt(1, number);
                pstmt.setString(2, playedId);

                int rowsAffected = pstmt.executeUpdate();
                return rowsAffected > 0;
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        });
    }

    public CompletableFuture<Boolean> updateAvailableRegions(String playedId, int number) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "UPDATE players SET available_regions = available_regions + ? WHERE id = ?";

            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setInt(1, number);
                pstmt.setString(2, playedId);

                int rowsAffected = pstmt.executeUpdate();
                return rowsAffected > 0;
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        });
    }

    public CompletableFuture<Boolean> updateAccumulator(String playedId, int number) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "UPDATE players SET accumulator = ? WHERE id = ?";

            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setInt(1, number);
                pstmt.setString(2, playedId);

                int rowsAffected = pstmt.executeUpdate();
                return rowsAffected > 0;
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        });
    }

    public CompletableFuture<Boolean> updateMuted(String playerId, int duration) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "UPDATE players SET muted = ? WHERE id = ?";

            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setInt(1, duration);
                pstmt.setString(2, playerId);

                int rowsAffected = pstmt.executeUpdate();
                return rowsAffected > 0;
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        });
    }

    public void reduceMuteDuration() {
        String sql = "SELECT id, muted FROM players WHERE muted > 0";
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String playerId = rs.getString("id");
                int mutedSeconds = rs.getInt("muted");

                // Check if the player is offline
                if (Bukkit.getPlayer(playerId) == null) { // Player is offline
                    int newMutedSeconds = mutedSeconds - 1;

                    // Update the muted duration in the database
                    updateMuted(playerId, newMutedSeconds);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

//    public CompletableFuture<Boolean> updateBanned(String playerId, int durationChange) {
//        return CompletableFuture.supplyAsync(() -> {
//            String sql = "UPDATE players SET banned = banned + ? WHERE id = ?";
//
//            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
//                pstmt.setInt(1, durationChange);
//                pstmt.setString(2, playerId);
//
//                int rowsAffected = pstmt.executeUpdate();
//                return rowsAffected > 0;
//            } catch (SQLException e) {
//                e.printStackTrace();
//                return false;
//            }
//        });
//    }
}

