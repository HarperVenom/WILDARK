package me.harpervenom.wildark.commands;

import me.harpervenom.wildark.classes.WildPlayer;
import me.harpervenom.wildark.database.managers.PlayersManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

import static me.harpervenom.wildark.WILDARK.db;
import static me.harpervenom.wildark.WILDARK.getPlugin;
import static me.harpervenom.wildark.listeners.PlayerListener.getWildPlayer;
import static me.harpervenom.wildark.listeners.PlayerListener.wildPlayers;

public class Mute implements CommandExecutor {


    public Mute() {
        Bukkit.getScheduler().runTaskTimer(getPlugin(), () -> {
            for (Map.Entry<UUID, WildPlayer> entry : wildPlayers.entrySet()) {
                WildPlayer wp = entry.getValue();
                if (wp.getMuted() > 0) wp.addMuted(-1);
            }

            db.players.reduceMuteDuration();
        }, 20, 20);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender.isOp())) {
            sender.sendMessage("Нет прав.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("Использование: /mute <player> <duration>");
            return true;
        }

        String targetName = args[0];
        String targetPlayer = String.valueOf(Bukkit.getOfflinePlayer(targetName).getUniqueId());


        int duration;

        try {
            duration = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage("Длительность должна быть числом.");
            return true;
        }

        Player p = Bukkit.getPlayer(targetName);
        if (p != null) {
            WildPlayer wp = getWildPlayer(p);
            wp.addMuted(duration);
            sender.sendMessage("Игрок " + targetName + " замъючен на " + duration + " секунд.");
            return true;
        }

        db.players.updateMuted(targetPlayer, duration).thenAccept((success) -> {
            if (success) {
                sender.sendMessage("Игрок " + targetName + " замъючен на " + duration + " секунд.");
            } else {
                sender.sendMessage("Не удалось замутить игрока " + targetPlayer + ".");
            }
        });
        return true;
    }


}
