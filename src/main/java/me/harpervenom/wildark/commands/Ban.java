package me.harpervenom.wildark.commands;

import me.harpervenom.wildark.database.managers.PlayersManager;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.profile.PlayerProfile;

import java.net.InetAddress;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static me.harpervenom.wildark.WILDARK.getPlugin;

public class Ban implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender.isOp())) {
            sender.sendMessage("Нет прав.");
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage("Использование: /ban <player> <duration> <reason>");
            return true;
        }

        String targetPlayer = args[0];
        int duration;

        try {
            duration = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage("Длительность должна быть числом.");
            return true;
        }

        String reason = args[2];

        banPlayer(sender, targetPlayer, reason, duration);
        return true;
    }

    public void banPlayer(CommandSender sender, String playerName, String reason, long durationInSeconds) {
        // Calculate the expiration date for the ban
        Date expirationDate = durationInSeconds > 0 ? new Date(System.currentTimeMillis() + (durationInSeconds * 1000)) : null;

        UUID playerUUID = null;

        List<OfflinePlayer> offlinePlayers = List.of(Bukkit.getOfflinePlayers());
        for (OfflinePlayer player : offlinePlayers) {
            if (player.getName() == null) continue;
            if (player.getName().equals(playerName)) {
                playerUUID = player.getUniqueId();
            }
        }

        if (playerUUID == null) {
            sender.sendMessage("Игрок не найден.");
            return;
        }

        Bukkit.broadcastMessage(playerUUID + "");

        // Access the ban list for players
        BanList<PlayerProfile> banList = Bukkit.getBanList(BanList.Type.PROFILE);

        PlayerProfile playerProfile = Bukkit.createPlayerProfile(playerUUID, playerName);

        // Add the player to the ban list with the expiration date and reason
        banList.addBan(playerProfile, reason, expirationDate, "Plugin");

        // If the player is currently online, kick them with the reason and expiration date
        Player player = Bukkit.getPlayer(playerName);
        if (player != null) {
            String expirationMessage = expirationDate != null ? expirationDate.toString() : "неопределенный срок";
            player.kickPlayer("Вы забанены до " + expirationMessage + ". Причина: " + reason);
        }

        // Ban the player by IP
        if (player != null) {
            InetAddress ipAddress = player.getAddress().getAddress();
            Bukkit.getBanList(BanList.Type.IP).addBan(ipAddress.getHostAddress(), reason, expirationDate, "Plugin");
        }
    }
}
