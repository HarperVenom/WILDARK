package me.harpervenom.wildark.commands;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
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
        return true;
    }

    public static void showGeneralInfo(Player p) {
        p.sendMessage("Добро пожаловать!");
        p.sendMessage("На сервере присутствуют уникальные системы.");

        TextComponent message = new TextComponent("Ознакомиться - ");

        // Create the clickable "Подробнее" part
        TextComponent moreInfo = new TextComponent("*Подробнее*");
        moreInfo.setColor(net.md_5.bungee.api.ChatColor.GRAY);
        moreInfo.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://harpervenom.github.io/wildark_website/")); // Replace with your URL

        // Add "Подробнее" to the base message
        message.addExtra(moreInfo);
        p.spigot().sendMessage(message);
    }
}
