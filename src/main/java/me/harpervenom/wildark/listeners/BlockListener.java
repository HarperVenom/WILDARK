package me.harpervenom.wildark.listeners;

import me.harpervenom.wildark.WILDARK;
import me.harpervenom.wildark.classes.Region;
import me.harpervenom.wildark.classes.WildBlock;
import me.harpervenom.wildark.database.Database;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static me.harpervenom.wildark.listeners.WildChunksListener.*;

public class BlockListener implements Listener {

    private static Database db;

    public BlockListener(Database db) {
        this.db = db;
    }

    @EventHandler
    public void BlockPlaceEvent(BlockPlaceEvent e) {
        Block b = e.getBlock();
        Player p = e.getPlayer();
        Chunk chunk = b.getChunk();

        String playerUIID = p.getUniqueId().toString();
        int x = b.getX();
        int y = b.getY();
        int z = b.getZ();
        String world = e.getBlock().getWorld().getName();

        if (wildBlocks.containsKey(chunk)) {
            List<WildBlock> wildBlockList = new ArrayList<>(wildBlocks.get(chunk));
            wildBlockList.add(new WildBlock(b.getLocation(), p.getUniqueId().toString()));
            wildBlocks.put(chunk, wildBlockList);
        }

        db.blocks.logBlock(playerUIID,x,y,z,world).thenAccept(result -> {
            if (!result) {
                p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Ошибка! Блок не сохранен!"));
            }
        });
    }

    @EventHandler
    public void BlockBreakEvent(BlockBreakEvent e) {
        Block b = e.getBlock();
        Player p = e.getPlayer();

        Chunk chunk = b.getChunk();

        if (!wildBlocks.containsKey(chunk) || !wildRegions.containsKey(chunk)){
            e.setCancelled(true);
            p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Не прогружено"));
            loadWildChunks(getActiveChunks(chunk));
        } else {
            Region region = getBlockRegion(b);
            WildBlock wildBlock = getWildBlock(b);

            if (wildBlock == null) {
                return;
            }
            if (region == null || region.getOwner().getUniqueId().toString().equals(p.getUniqueId().toString())) {
                wildBlocks.put(chunk, wildBlocks.get(chunk).stream().filter(currentWildBlock -> !currentWildBlock.equals(wildBlock)).toList());
                db.blocks.deleteBlockRecord(b);
                return;
            }

            e.setCancelled(true);
            p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Защищено"));
        }
    }

    public static WildBlock getWildBlock(Block b) {
        Chunk chunk = b.getChunk();

        for (WildBlock block : wildBlocks.get(chunk)) {
            if (block.getLoc().equals(b.getLocation())) return block;
        }

        return null;
    }

    public static Region getBlockRegion(Block b) {
        Chunk chunk = b.getChunk();

        for (Region region : wildRegions.get(chunk)) {
            if (region.contains(b.getX(), b.getZ())) return region;
        }

        return null;
    }
}
