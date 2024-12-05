package me.harpervenom.wildark.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Help implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) return false;

        showGeneralInfo(p);
//        p.sendMessage("Помимо этого, в игру введены небольшие изменения: ");
//        p.sendMessage(" - Полный день длится 1 час (30 минут день и 30 минут ночь).");
//        p.sendMessage(" - Ночь невозможно пропустить.");
//        p.sendMessage(" - Координаты скрыты. Ориентироваться придется естественными способами.");
//        p.sendMessage(" - При каждом возраждении у вас остается всего 2 единицы голода.");
        return true;
    }

    public static void showGeneralInfo(Player p) {
        p.sendMessage("Добро пожаловать!");
        p.sendMessage("На сервере присутствуют уникальные.");
        p.sendMessage("Ознакомиться - " + ChatColor.GRAY + "*Подробнее*");
    }
}
