package me.harpervenom.wildark.listeners;

import me.harpervenom.wildark.database.Database;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerListener implements Listener {

    private Database db;

    public PlayerListener(Database db) {
        this.db = db;
    }

    @EventHandler
    public void PlayerJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        if (db.players.getPlayer(p.getUniqueId().toString()) == null) {
            db.players.create(p.getUniqueId());
        }
    }
}
