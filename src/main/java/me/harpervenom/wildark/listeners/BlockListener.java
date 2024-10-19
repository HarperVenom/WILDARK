package me.harpervenom.wildark.listeners;

import me.harpervenom.wildark.WILDARK;
import me.harpervenom.wildark.classes.Region;
import me.harpervenom.wildark.classes.WildBlock;
import me.harpervenom.wildark.database.Database;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Bed;
import org.bukkit.block.data.type.Door;
import org.bukkit.block.data.type.PistonHead;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.*;


import static me.harpervenom.wildark.Materials.*;
import static me.harpervenom.wildark.listeners.WildChunksListener.*;

public class BlockListener implements Listener {

    @EventHandler
    public void BlockPlaceEvent(BlockPlaceEvent e) {
        Block b = e.getBlock();
        Player p = e.getPlayer();
        Chunk chunk = b.getChunk();

        if (chunkNotLoaded(p, chunk)){
            e.setCancelled(true);
            return;
        }

        if (!isTrueBlock(p, b)) return;

        WildBlock wildBlock = new WildBlock(b.getLocation(), p.getUniqueId().toString());
        wildBlock.save();
    }

    @EventHandler
    public void BlockBreakEvent(BlockBreakEvent e) {
        Block b = getMainBlock(e.getBlock());
        Player p = e.getPlayer();

        Chunk chunk = b.getChunk();

        if (chunkNotLoaded(p, chunk)){
            e.setCancelled(true);
            return;
        }

        WildBlock wildBlock = getWildBlock(b);

        if (wildBlock == null) {
            return;
        }

        if (blockCanBreak(p.getUniqueId().toString(), b) || p.getGameMode() == GameMode.CREATIVE) {
            wildBlock.remove();
        } else {
            boolean destroyed = hitBlock(p, b);
            if (!destroyed) {
                e.setCancelled(true);
            } else {
                wildBlock.remove();
            }
        }
    }

    @EventHandler
    public void onPistonExtend(BlockPistonExtendEvent e) {
        boolean shifted = handlePistonMoveEvent(e.getBlock(), e.getBlocks(), e.getDirection().getDirection());
        if (!shifted) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onPistonRetract(BlockPistonRetractEvent e) {
        boolean shifted = handlePistonMoveEvent(e.getBlock(), e.getBlocks(), e.getDirection().getDirection());
        if (!shifted) {
            e.setCancelled(true);
        }
    }

    public boolean handlePistonMoveEvent(Block piston, List<Block> blocks, Vector direction) {

        if (blocks.isEmpty()) return true;

        boolean canShift = true;

        // First, check if all blocks can be shifted
        for (Block block : blocks) {
            if (chunkNotLoaded(null, block.getChunk())) {
                canShift = false;
                break;
            }

            WildBlock wildBlock = getWildBlock(block);
            if (wildBlock == null) continue;

            WildBlock wildPiston = getWildBlock(piston);
            if (wildPiston == null) return true;

            if (!blockCanBreak(wildPiston.getOwnerId(), block)) {
                canShift = false;
                break;
            }
        }

        if (canShift) {
            Map<WildBlock, Location> blockLocations = new HashMap<>();

            for (Block block : blocks) {
                WildBlock wildBlock = getWildBlock(block);
                if (wildBlock == null) continue;

                Location oldLocation = block.getLocation();
                Location newLocation = oldLocation.clone().add(direction);
                blockLocations.put(wildBlock, newLocation);
            }

            blockLocations.forEach(WildBlock::move);

            return true;
        } else {
            return false;
        }
    }

    public static HashMap<FallingBlock, WildBlock> fallingBlocks = new HashMap<>();

    @EventHandler
    public void onBlockBecomeFalling(EntityChangeBlockEvent e) {
        if (e.getTo() != Material.AIR) return;
        Block b = e.getBlock();

        loadChunkSync(b.getChunk());

        WildBlock wildBlock = getWildBlock(b);
        if (wildBlock == null) return;

        if (e.getEntity() instanceof FallingBlock falling) {
            fallingBlocks.put(falling, wildBlock);
        }
    }

    @EventHandler
    public void onBlockLand(EntityChangeBlockEvent e) {
        if (e.getTo() == Material.AIR) return;
        if (e.getEntity() instanceof FallingBlock falling) {
            Location newLocation = e.getBlock().getLocation();

            if (fallingBlocks.containsKey(falling)) {
                fallingBlocks.get(falling).move(newLocation);
            }
        }
    }

    public boolean blockCanBreak(String playerId, Block b) {
        Region region = getBlockRegion(b);
        //owner cant break his blocks. For testing
        return region == null || !region.getOwnerId().toString().equals(playerId);
    }

    public static WildBlock getWildBlock(Block b) {
        Chunk chunk = b.getChunk();

        for (WildBlock block : wildBlocks.get(chunk)) {
            if (block.getLoc().equals(b.getLocation())) return block;
        }

        return null;
    }

    public static Region getBlockRegion(Block b) {
        Region region = null;

        for (Region currenRegion : wildRegions) {
            if (currenRegion.contains(b)) {
                region = currenRegion;
            }
        }

        return region;
    }

    public static HashMap<Block, Integer> damagedBlocks = new HashMap<>();
    private final HashMap<Block, BukkitTask> restoreHealthTasks = new HashMap<>();

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



    public static Block getMainBlock(Block b) {
        BlockData blockData = b.getBlockData();
        //If Bed
        if (blockData instanceof Bed bed && bed.getPart() == Bed.Part.HEAD) {
            BlockFace facing = bed.getFacing();
            return b.getRelative(facing.getOppositeFace());
        }

        //If Piston
        if (blockData instanceof PistonHead pistonHead) {
            BlockFace facing = pistonHead.getFacing();

            return b.getRelative(facing.getOppositeFace());
        }

        //If Door
        if (blockData instanceof Door door) {
            if (door.getHalf() == Bisected.Half.TOP) {
                return b.getRelative(BlockFace.DOWN);
            }
        }

        return b;
    }
}
