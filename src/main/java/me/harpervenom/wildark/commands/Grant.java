package me.harpervenom.wildark.commands;

import me.harpervenom.wildark.classes.Region;
import me.harpervenom.wildark.classes.WildPlayer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static me.harpervenom.wildark.listeners.PlayerListener.getWildPlayer;

public class Grant implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check if the sender is a player
        if (!(sender instanceof Player p)) {
            sender.sendMessage("This command can only be run by a player.");
            return false;
        }

        // Check if the correct number of arguments is passed
        if (args.length != 3) {
            sender.sendMessage("Использование: /grant <player> <region name> <relation>");
            return false;
        }

        String targetPlayerName = args[0];  // The player to grant
        String regionName = args[1];        // The region name
        String relation = args[2];          // The relation

        // Try to find the target player (could also be done via UUID lookup, etc.)
        Player targetPlayer = Bukkit.getPlayer(targetPlayerName);
        if (targetPlayer == null) {
            p.sendMessage("Игрок " + targetPlayerName + " не найден.");
            return false;
        }

        WildPlayer wp = getWildPlayer(p);
        if (wp == null) return false;
        Region selectedRegion = null;
        for (Region region : wp.getRegions()) {
            if (region.getName().equals(regionName)) {
                selectedRegion = region;
            }
        }

        if (selectedRegion == null) {
            p.sendMessage("У вас нет региона с названием: " + regionName);
            return false;
        }

        if (relation.equals("member") || relation.equals("authority") || relation.equals("claimed")) {
            selectedRegion.addRelation(targetPlayer.getUniqueId(), relation);
        } else {
            p.sendMessage("Такого права не существует.");
            return false;
        }

        return true;
    }
}

