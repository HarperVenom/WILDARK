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

public class BlockListener implements Listener {

    private static Database db;

    public BlockListener(Database db) {
        this.db = db;
    }

    public static HashMap<Chunk, List<Region>> wildRegions = new HashMap<>();
    public static HashMap<Chunk, List<WildBlock>> wildBlocks = new HashMap<>();

    HashMap<UUID, Chunk> lastChunk = new HashMap<>();

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
            loadWildChunks(chunk);
        } else {
            Region region = getBlockRegion(b);
            WildBlock wildBlock = getWildBlock(b);

            if (wildBlock == null) {
                return;
            }
            if (region == null || region.getOwner().getUniqueId().toString().equals(p.getUniqueId().toString())) {
                BlockListener.wildBlocks.put(chunk, wildBlocks.get(chunk).stream().filter(currentWildBlock -> !currentWildBlock.equals(wildBlock)).toList());
                db.blocks.deleteBlockRecord(b);
                return;
            }

            e.setCancelled(true);
            p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Защищено"));
        }
    }

    @EventHandler
    public void PlayerJoin(PlayerJoinEvent e){
        Player p = e.getPlayer();
        Chunk chunk = p.getLocation().getChunk();

        loadWildChunks(chunk);
    }

    @EventHandler
    public void PlayerMoveEvent(PlayerMoveEvent e){
        Player p = e.getPlayer();
        UUID id = p.getUniqueId();
        Chunk chunk = p.getLocation().getChunk();

        if (!lastChunk.containsKey(id)) {
            lastChunk.put(id, chunk);
        } else {
            if (!lastChunk.get(id).equals(chunk)) {

                Bukkit.getServer().getScheduler().runTaskLater(WILDARK.getPlugin(), () -> {
                    Chunk newChunk = p.getLocation().getChunk();
                    if (newChunk.equals(chunk)){
                        loadWildChunks(chunk);
                    }
                }, 20);

                lastChunk.put(id, chunk);
            }
        }
    }

    public static void loadWildChunks(Chunk chunk){
        List<Chunk> chunks = new ArrayList<>();

        int x = chunk.getX();
        int z = chunk.getZ();
        World world = chunk.getWorld();

        chunks.add(chunk);
        chunks.add(world.getChunkAt(x+1,z));
        chunks.add(world.getChunkAt(x,z+1));
        chunks.add(world.getChunkAt(x+1,z+1));
        chunks.add(world.getChunkAt(x-1,z));
        chunks.add(world.getChunkAt(x,z-1));
        chunks.add(world.getChunkAt(x-1,z-1));
        chunks.add(world.getChunkAt(x+1,z-1));
        chunks.add(world.getChunkAt(x-1,z+1));

        for (Chunk currentChunk : chunks) {
            int chunkX = currentChunk.getX();
            int chunkZ = currentChunk.getZ();

            int minX = chunkX * 16;
            int maxX = minX + 15;
            int minZ = chunkZ * 16;
            int maxZ = minZ + 15;

            if (!wildBlocks.containsKey(currentChunk)) {
                db.blocks.getWildBlocks(minX, minZ, maxX, maxZ, world.getName())
                        .thenAccept(blocks -> wildBlocks.put(currentChunk, blocks));
            }

            if (!wildRegions.containsKey(currentChunk)) {
                db.regions.scanArea(world.getName(), minX, minZ, maxX, maxZ)
                        .thenAccept(regions -> wildRegions.put(currentChunk, regions));
            }
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
