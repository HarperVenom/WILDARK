package me.harpervenom.wildark.commands;

import com.destroystokyo.paper.profile.PlayerProfile;
import io.papermc.paper.ban.BanListType;
import me.harpervenom.wildark.database.managers.PlayersManager;
import me.harpervenom.wildark.listeners.PlayerListener;
import net.kyori.adventure.text.Component;
import org.apache.maven.model.Profile;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.ban.IpBanList;
import org.bukkit.ban.ProfileBanList;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.Instant;
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
            sender.sendMessage("Использование: /ban <player> <minutes> <reason>");
            return true;
        }

        String targetPlayer = args[0];
        int minutes;

        try {
            minutes = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage("Длительность должна быть числом.");
            return true;
        }

        String reason = args[2];

        banPlayer(sender, targetPlayer, reason, minutes);
        return true;
    }

    public void banPlayer(CommandSender sender, String playerName, String reason, long durationInMinutes) {
        Instant expirationInstant = durationInMinutes > 0 ? Instant.now().plusSeconds(durationInMinutes * 60) : null;
        Date expirationDate = expirationInstant != null ? Date.from(expirationInstant) : null;

        UUID playerUUID;
        OfflinePlayer targetOfflinePlayer = Bukkit.getOfflinePlayerIfCached(playerName);

        if (targetOfflinePlayer == null) {
            sender.sendMessage("Игрок не найден в кеше. Попробуйте позже.");
            return;
        }

        playerUUID = targetOfflinePlayer.getUniqueId();
        Player p = Bukkit.getPlayer(playerUUID);

        //IP
        if (p != null) {
            InetSocketAddress socketAddress = p.getAddress();
            if (socketAddress != null) {
                InetAddress inetAddress = socketAddress.getAddress();

                IpBanList ipBanList = Bukkit.getBanList(BanListType.IP);

                ipBanList.addBan(inetAddress, reason, expirationDate, "Wildark");

                sender.sendMessage("IP-адрес игрока " + playerName + " успешно забанен.");
                Component kickMessage = Component.text("Вы забанены. Причина: " + reason + ". До: " + expirationDate);
                p.kick(kickMessage);
            } else {
                sender.sendMessage("Не удалось получить IP-адрес игрока.");
            }
        }

        //PROFILE
        PlayerProfile playerProfile = Bukkit.createProfile(playerUUID, playerName);

        ProfileBanList banList = Bukkit.getBanList(BanListType.PROFILE);
        banList.addBan(playerProfile, reason, expirationDate, "Wildark");


        String message = "Игрок " + playerName + " успешно забанен. Причина: " + reason + ". До: " + expirationDate;
        getPlugin().getLogger().info(message);
        if (!(sender instanceof ConsoleCommandSender)) {
            sender.sendMessage(message);
        }
    }
}
