package me.harpervenom.wildark.commands;

import me.harpervenom.wildark.classes.Region;
import me.harpervenom.wildark.classes.WildPlayer;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.UUID;

import static me.harpervenom.wildark.classes.Region.returnCoefficient;
import static me.harpervenom.wildark.listeners.PlayerListener.getWildPlayer;

public class DeleteRegion implements CommandExecutor {

    private HashMap<UUID, Region> repeatCommand = new HashMap<>();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) return false;

        if (args.length != 1) {
            sender.sendMessage("Использование: /delete <region name>");
            return false;
        }

        String regionName = args[0];

        WildPlayer wp = getWildPlayer(p);
        if (wp == null) return false;

        Region region = wp.getRegions().stream().filter(currentRegion -> currentRegion.getName().equals(regionName)).findFirst().orElse(null);

        if (region == null) {
            sender.sendMessage(ChatColor.RED + "Участок не найден");
            return false;
        }

        if (!repeatCommand.containsKey(p.getUniqueId()) || !repeatCommand.get(p.getUniqueId()).equals(region)) {
            repeatCommand.put(p.getUniqueId(), region);
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BANJO, 1, 0.5f);
            sender.sendMessage(ChatColor.RED + "Вы собираетесь удалить участок: " + regionName);
            sender.sendMessage(ChatColor.RED + "Вам будет возвращено блоков: " + (returnCoefficient * region.getArea()));
            sender.sendMessage(ChatColor.RED + "Чтобы подтвердить операцию, повторите команду.");
            return false;
        }

        wp.deleteRegion(region);
        sender.sendMessage(ChatColor.RED + "Вы удалили участок: " + regionName);

        repeatCommand.remove(p.getUniqueId());

        return false;
    }
}
