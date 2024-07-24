package me.harpervenom.wildark;

import me.harpervenom.wildark.database.Database;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

public class BlockManager implements Listener {

    private Database db;

    public BlockManager(Database db) {
        this.db = db;
    }

    @EventHandler
    public void BlockPlaceEvent(BlockPlaceEvent e) {
        Block b = e.getBlock();
        Player p = e.getPlayer();

        String playerUIID = p.getUniqueId().toString();
        int x = b.getX();
        int y = b.getY();
        int z = b.getZ();
        String world = e.getBlock().getWorld().getName();


        Bukkit.getScheduler().runTaskAsynchronously(WILDARK.getPlugin(), () -> {
            boolean res = db.blocks.logBlock(playerUIID,x,y,z,world);

            Bukkit.getScheduler().runTask(WILDARK.getPlugin(), () -> {
                if (!res) {
                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT,1,0.5f);
                    p.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                            new TextComponent(ChatColor.RED + "Не удалось записать блок."));
                }
            });
        });

    }

    @EventHandler
    public void BlockBreakEvent(BlockBreakEvent e) {
        Block b = e.getBlock();
        Player p = e.getPlayer();

        String ownerUUID = db.blocks.getOwner(b);
        if (ownerUUID == null) return;
        if (p.getUniqueId().toString().equals(ownerUUID)) {
            db.blocks.deleteBlockRecord(b);
        }
    }

}
