package me.harpervenom.wildark.listeners;

import me.harpervenom.wildark.classes.WildPlayer;
import me.harpervenom.wildark.database.Database;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import static me.harpervenom.wildark.WILDARK.db;

import java.util.HashMap;
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
                }
               wildPlayers.put(id, wildPlayer);
            });
        }
    }

    public static WildPlayer getWildPlayer(Player p){
        return wildPlayers.get(p.getUniqueId());
    }
}
