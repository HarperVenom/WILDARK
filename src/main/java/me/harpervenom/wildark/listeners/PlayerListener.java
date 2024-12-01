package me.harpervenom.wildark.listeners;

import me.harpervenom.wildark.classes.Region;
import me.harpervenom.wildark.classes.WildPlayer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;

import static me.harpervenom.wildark.WILDARK.db;
import static me.harpervenom.wildark.WILDARK.getPlugin;
import static me.harpervenom.wildark.listeners.WildChunksListener.wildRegions;

import java.util.*;

public class PlayerListener implements Listener {

    public static HashMap<UUID, WildPlayer> wildPlayers = new HashMap<>();

    @EventHandler
    public void PlayerJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();

        e.setJoinMessage(ChatColor.YELLOW + p.getName() + " в игре.");
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
        } else {
            WildPlayer wp = getWildPlayer(p);
            wp.setOffline(false);
        }
    }

    @EventHandler
    public void PlayerQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        e.setQuitMessage(ChatColor.YELLOW + p.getName() + " больше не в игре.");
        WildPlayer wp = getWildPlayer(p);
        if (wp == null) return;
        wp.setOffline(true);
    }

    @EventHandler
    public void PlayerRespawn(PlayerRespawnEvent e) {
        Player p = e.getPlayer();
        Bukkit.getScheduler().runTaskLater(getPlugin(), () -> {
            p.setFoodLevel(2);
        },1);

    }

    private static final int LOCAL_RADIUS = 30;

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        // Cancel the default chat behavior
        event.setCancelled(true);

        Player sender = event.getPlayer();
        String message = event.getMessage();
        Location senderLocation = sender.getLocation();
        boolean isGlobal = message.startsWith("!");

        // Define a flag to track if anyone other than the sender hears the message
        boolean messageReceivedByOthers = false;

        for (Player recipient : Bukkit.getOnlinePlayers()) {
            if (!recipient.getWorld().equals(sender.getWorld())) continue;

            double distance = recipient.getLocation().distance(senderLocation);

            if (isGlobal) {
                // Global Chat
                if (distance <= LOCAL_RADIUS) {
                    recipient.sendMessage(
                            ChatColor.DARK_RED + "[W] " +
                                    ChatColor.WHITE + sender.getName() +
                                    ChatColor.GRAY + ": " + ChatColor.AQUA + message.substring(1)
                    );
                } else {
                    recipient.sendMessage(
                            ChatColor.DARK_RED + "[W] " +
                                    ChatColor.WHITE + sender.getName() +
                                    ChatColor.GRAY + ": " + message.substring(1)
                    );
                }
                System.out.println("[Global] " + sender.getName() + ": " + message.substring(1));
                if (!recipient.equals(sender)) {
                    messageReceivedByOthers = true;
                }
            } else {
                // Local Chat
                if (distance <= LOCAL_RADIUS) {
                    recipient.sendMessage(
                            ChatColor.DARK_RED + "[W] " +
                                    ChatColor.WHITE + sender.getName() +
                                    ChatColor.GRAY + ": " + ChatColor.WHITE + message
                    );
                    if (!recipient.equals(sender)) {
                        messageReceivedByOthers = true;
                    }
                }

                System.out.println("[Local] " + sender.getName() + ": " + message);
            }
        }

        // Notify the sender if no one else received the message
        if (!messageReceivedByOthers) {
            sender.sendMessage(ChatColor.RED + "Вас никто не услышал...");
        }
    }

    public static WildPlayer getWildPlayer(Player p){
        return wildPlayers.get(p.getUniqueId());
    }
}
