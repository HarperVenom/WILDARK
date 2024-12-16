package me.harpervenom.wildark.listeners;

import com.destroystokyo.paper.event.player.PlayerSetSpawnEvent;
import com.destroystokyo.paper.profile.PlayerProfile;
import io.papermc.paper.ban.BanListType;
import io.papermc.paper.event.player.AsyncChatEvent;
import me.harpervenom.wildark.classes.Region;
import me.harpervenom.wildark.classes.WildBlock;
import me.harpervenom.wildark.classes.WildPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.ban.IpBanList;
import org.bukkit.ban.ProfileBanList;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;
import org.bukkit.event.world.SpawnChangeEvent;

import static me.harpervenom.wildark.WILDARK.db;
import static me.harpervenom.wildark.WILDARK.getPlugin;
import static me.harpervenom.wildark.commands.Help.showGeneralInfo;
import static me.harpervenom.wildark.listeners.BlockListener.*;
import static me.harpervenom.wildark.listeners.WildChunksListener.wildRegions;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.*;

public class PlayerListener implements Listener {

    public static HashMap<UUID, WildPlayer> wildPlayers = new HashMap<>();

    List<TextComponent> messages = new ArrayList<>();
    private int messageIndex = 0;

    public PlayerListener() {
        TextComponent message1 = new TextComponent(ChatColor.GOLD + "Инструкция по серверу - " + ChatColor.GRAY + "*Ссылка*");
        message1.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://harpervenom.github.io/wildark_website/"));

        TextComponent message2 = new TextComponent(ChatColor.GOLD + "Присоединяйтесь к серверу Дискорд - " + ChatColor.GRAY + "*Ссылка*");
        message2.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://discord.gg/XPQUZuXmqs"));

        TextComponent message3 = new TextComponent(ChatColor.GOLD + "Присоединяйтесь к чату Телеграм - " + ChatColor.GRAY + "*Ссылка*");
        message3.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://t.me/+uncOhNHx6zY5ZWFi"));

        messages.add(message1);
        messages.add(message2);
        messages.add(message3);

        Bukkit.getScheduler().runTaskTimer(getPlugin(), () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.spigot().sendMessage(messages.get(messageIndex));
            }
            messageIndex = (messageIndex + 1) % messages.size();
        },0, 1800 * 20);
    }

    @EventHandler
    public void PlayerJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();

        e.setJoinMessage(ChatColor.YELLOW + p.getName() + " в игре.");
        UUID id = p.getUniqueId();

        Bukkit.getScheduler().runTaskLater(getPlugin(),() -> showGeneralInfo(p),1);

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

    private static final int LOCAL_RADIUS = 50;

    @EventHandler
    public void onPlayerChat(AsyncChatEvent e) {
        // Cancel the default chat behavior
        e.setCancelled(true);

        Player sender = e.getPlayer();

        WildPlayer wp = getWildPlayer(sender);
        if (wp.getMuted() > 0) {
            sender.sendMessage(ChatColor.RED + "Вы сможете писать в чат через: " + wp.getMuted() + " секунд.");
            return;
        }

        String message = PlainTextComponentSerializer.plainText().serialize(e.message());
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
                    recipient.sendMessage("<" +
                                    ChatColor.WHITE + sender.getName() +
                                    "> " + ChatColor.AQUA + message.substring(1)
                    );
                } else {
                    recipient.sendMessage("<" +
                                    ChatColor.WHITE + sender.getName() +
                                    "> " + ChatColor.GRAY + message.substring(1)
                    );
                }

                if (!recipient.equals(sender)) {
                    messageReceivedByOthers = true;
                }
            } else {
                // Local Chat
                if (distance <= LOCAL_RADIUS) {
                    recipient.sendMessage("<" +
                                    ChatColor.WHITE + sender.getName() + "> " + message
                    );
                    if (!recipient.equals(sender)) {
                        messageReceivedByOthers = true;
                    }
                }


            }
        }
        getPlugin().getLogger().info(sender.getName() + " > " + message);

        // Notify the sender if no one else received the message
        if (!messageReceivedByOthers) {
            sender.sendMessage(ChatColor.GRAY + "Вас никто не услышал...");
        }
    }

    @EventHandler
    public void RespawnChange(PlayerSetSpawnEvent e) {
        Location newLoc = e.getLocation();
        if (newLoc == null) return;

        Player p = e.getPlayer();
        Block b = getMainBlock(newLoc.getBlock());

        WildBlock wb = getWildBlock(b);
        if (wb == null) return;
        if (!blockCanBreak(p.getUniqueId().toString(), b)) {
            e.setCancelled(true);
        }
    }

    public static WildPlayer getWildPlayer(Player p){
        return wildPlayers.get(p.getUniqueId());
    }

    @EventHandler
    public void BannedJoin(AsyncPlayerPreLoginEvent e) {
        ProfileBanList banList = Bukkit.getBanList(BanListType.PROFILE);
        PlayerProfile playerProfile = e.getPlayerProfile();

        if (banList.isBanned(playerProfile)) {
            BanEntry<PlayerProfile> profileBanEntry = banList.getBanEntry(playerProfile);
            String reason = profileBanEntry.getReason();
            Date expirationDate = profileBanEntry.getExpiration();

            InetAddress inetAddress = e.getAddress();

            IpBanList ipBanList = Bukkit.getBanList(BanListType.IP);

            if (!ipBanList.isBanned(inetAddress)) {
                ipBanList.addBan(inetAddress, reason, expirationDate, "Wildark");
                getPlugin().getLogger().info("IP-адрес игрока " + e.getName() + " успешно забанен.");
            }
        }
    }
}
