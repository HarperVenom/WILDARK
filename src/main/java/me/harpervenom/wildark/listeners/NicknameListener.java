package me.harpervenom.wildark.listeners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public class NicknameListener implements Listener {

    @EventHandler
    public void PlayerJoinEvent(PlayerJoinEvent e){
        Player p = e.getPlayer();
        setInvisibleNickname(p);
    }

    @EventHandler
    public void InteractEvent(PlayerInteractEntityEvent e){
        if (!(e.getRightClicked() instanceof Player interacted)) return;
        Player p = e.getPlayer();
        ItemStack helmet = interacted.getInventory().getHelmet();
        if (helmet != null && helmet.getType() == Material.CARVED_PUMPKIN){
            p.playSound(p.getLocation(), Sound.BLOCK_POWDER_SNOW_BREAK,1.3f,0.3f);
            return;
        }

        p.sendActionBar(Component.text()
                .color(NamedTextColor.GOLD)
                .append(interacted.displayName())
                .build());
    }


    public void setInvisibleNickname(Player player) {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();

        Team invisibleTeam = scoreboard.getTeam(player.getName());
        if (invisibleTeam == null) {
            invisibleTeam = scoreboard.registerNewTeam(player.getName());
        }

        invisibleTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);

        invisibleTeam.addEntry(player.getName());

        player.setScoreboard(scoreboard);
    }
}
