package me.harpervenom.wildark.listeners;

import me.harpervenom.wildark.classes.Region;
import me.harpervenom.wildark.classes.WildPlayer;
import me.harpervenom.wildark.database.Database;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import static me.harpervenom.wildark.WILDARK.db;
import static me.harpervenom.wildark.WILDARK.getPlugin;
import static me.harpervenom.wildark.listeners.WildChunksListener.wildRegions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class PlayerListener implements Listener {

    public static HashMap<UUID, WildPlayer> wildPlayers = new HashMap<>();

    @EventHandler
    public void PlayerJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        UUID id = p.getUniqueId();

        if (!wildPlayers.containsKey(id)) {
            db.players.getPlayer(id.toString()).thenAccept((wildPlayer) -> {
                if (wildPlayer == null) {
                    db.players.create(id).thenAccept((newWildPlayer) -> {
                        if (newWildPlayer == null) {
                            p.sendMessage(ChatColor.RED + "Не удалось создать профиль.");
                            return;
                        }
                        wildPlayers.put(id, newWildPlayer);
                    });
                    return;
                }

                List<Region> playerRegions = new ArrayList<>();
                for (Region region : wildRegions) {
                    if (region.getOwnerId().equals(id)) {
                        playerRegions.add(region);
                    }
                }
                wildPlayer.setRegions(playerRegions);

                wildPlayers.put(id, wildPlayer);
            });
        }
    }

    @EventHandler
    public void PlayerQuit(PlayerQuitEvent e) {
        WildPlayer wp = getWildPlayer(e.getPlayer());
        if (wp == null) return;
        wp.setOffline();
    }

    public static WildPlayer getWildPlayer(Player p){
        return wildPlayers.get(p.getUniqueId());
    }
}
