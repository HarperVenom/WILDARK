package me.harpervenom.wildark.listeners;

import me.harpervenom.wildark.WILDARK;
import me.harpervenom.wildark.classes.Region;
import me.harpervenom.wildark.classes.WildBlock;
import me.harpervenom.wildark.database.Database;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.stream.Collectors;


import static me.harpervenom.wildark.Materials.*;
import static me.harpervenom.wildark.listeners.WildChunksListener.*;

public class BlockListener implements Listener {

    private final Database db;

    public BlockListener(Database db) {
        this.db = db;
    }

    @EventHandler
    public void BlockPlaceEvent(BlockPlaceEvent e) {
        Block b = e.getBlock();
        Player p = e.getPlayer();

        if (!isTrueBlock(p, b)) {
            return;
        }

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
            //In case of error
            if (!result) {
                p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Ошибка! Блок не сохранен!"));

                //Remove block from the map
                List<WildBlock> wildBlockList = new ArrayList<>(wildBlocks.get(chunk));
                wildBlocks.put(chunk, wildBlockList.stream().filter(wildBlock -> wildBlock.getLoc().equals(b.getLocation())).collect(Collectors.toList()));
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
            //only owner cant break his blocks. For testing
            if (region == null || !region.getOwner().getUniqueId().toString().equals(p.getUniqueId().toString())) {
                wildBlocks.put(chunk, wildBlocks.get(chunk).stream().filter(currentWildBlock -> !currentWildBlock.equals(wildBlock)).toList());
                db.blocks.deleteBlockRecord(b);
            } else {
                boolean destroyed = hitBlock(p, b);
                if (!destroyed) {
                    e.setCancelled(true);
                } else {
                    wildBlocks.put(chunk, wildBlocks.get(chunk).stream().filter(currentWildBlock -> !currentWildBlock.equals(wildBlock)).toList());
                    db.blocks.deleteBlockRecord(b);
                }
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

    public static HashMap<Block, Integer> damagedBlocks = new HashMap<>();
    private HashMap<Block, BukkitTask> restoreHealthTasks = new HashMap<>();

    public boolean hitBlock(Player p, Block b) {
        boolean hasProperTool = false;
        ItemStack tool = p.getInventory().getItemInMainHand();
            Material itemType = tool.getType();

        for (Material toolType : getTools()) {
            if (itemType == toolType && b.isPreferredTool(tool)) {
                hasProperTool = true;
                break;
            }
        }

        if (!hasProperTool) {
            p.damage(0.5);
//            p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Защищено"));
            return false;
        }

        int toolDamage = getToolDamage(b, tool);
        ItemMeta meta = tool.getItemMeta();
        if (meta instanceof Damageable damageable){

            if (tool.getType().getMaxDurability()-damageable.getDamage() <= toolDamage){
                p.getInventory().removeItem(tool);
                p.getWorld().playSound(p,Sound.ENTITY_ITEM_BREAK,1,1);
            } else {
                damageable.setDamage(toolDamage + damageable.getDamage());
                tool.setItemMeta(meta);
                p.getWorld().playSound(p,Sound.ENTITY_ITEM_BREAK,0.1f,1.4f);
            }
        }

        BlockData blockData = b.getBlockData();
        Sound breakingSound = blockData.getSoundGroup().getBreakSound();

        int blockMaxHealth = getMaxBlockHealth(b);

        if (!damagedBlocks.containsKey(b)) {
            damagedBlocks.put(b, blockMaxHealth -1);
            b.getWorld().playSound(b.getLocation(), breakingSound, 0.7f, 1.5f);
            p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "" + (blockMaxHealth-1) + "/" + blockMaxHealth));
            scheduleRestoreHealth(b);
        } else {
            damagedBlocks.put(b, damagedBlocks.getOrDefault(b, 0) - 1);

            if (damagedBlocks.get(b) == 0) {
                damagedBlocks.remove(b);
                p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(""));
                return true;
            }

            b.getWorld().playSound(b.getLocation(), breakingSound, 0.7f, 1.5f);
            p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "" + damagedBlocks.get(b) + "/" + blockMaxHealth));
            scheduleRestoreHealth(b);
        }

        return false;
    }

    private void scheduleRestoreHealth(Block b) {
        int UNLOAD_DELAY = 180;

        if (restoreHealthTasks.containsKey(b)) {
            restoreHealthTasks.get(b).cancel();
        }

        BukkitTask unloadTask = new BukkitRunnable() {
            @Override
            public void run() {
                    damagedBlocks.put(b, getMaxBlockHealth(b));
                    restoreHealthTasks.remove(b);
            }
        }.runTaskLater(WILDARK.getPlugin(), UNLOAD_DELAY * 20L);

        restoreHealthTasks.put(b, unloadTask);
    }
}
