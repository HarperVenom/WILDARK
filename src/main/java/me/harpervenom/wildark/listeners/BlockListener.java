package me.harpervenom.wildark.listeners;

import me.harpervenom.wildark.WILDARK;
import me.harpervenom.wildark.classes.Region;
import me.harpervenom.wildark.classes.Relation;
import me.harpervenom.wildark.classes.WildBlock;
import me.harpervenom.wildark.database.Database;
import me.harpervenom.wildark.keys.classes.Lock;
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
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.sql.Timestamp;
import java.util.*;


import static me.harpervenom.wildark.Materials.*;
import static me.harpervenom.wildark.WILDARK.getPlugin;
import static me.harpervenom.wildark.keys.classes.Lock.getLock;
import static me.harpervenom.wildark.keys.classes.listeners.LockListener.isUnderDoorBlock;
import static me.harpervenom.wildark.listeners.WildChunksListener.*;
import static org.bukkit.Bukkit.getLogger;

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

        Timestamp timestamp = new java.sql.Timestamp(System.currentTimeMillis());

        WildBlock wildBlock = new WildBlock(b.getLocation(), p.getUniqueId().toString(), timestamp);
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
                if (b.getType() == Material.NETHERITE_BLOCK || b.getType() == Material.DIAMOND_BLOCK) {
                    e.setDropItems(false);
                }
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

        if (!(e.getEntity() instanceof FallingBlock falling)) return;

        loadChunkSync(b.getChunk());

        WildBlock wildBlock = getWildBlock(b);
        if (wildBlock == null) return;

        fallingBlocks.put(falling, wildBlock);
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

    public static boolean blockCanBreak(String playerId, Block b) {
        loadChunkSync(b.getChunk());
        WildBlock wb = getWildBlock(b);
        if (wb == null) return true;

        Region region = getBlockRegion(b);
        if (region == null) return true;
        if (playerId == null) return false;

        String blockOwnerId = wb.getOwnerId();
        String regionOwnerId = region.getOwnerId().toString();

        if (regionOwnerId.equals(playerId)) return true;

        Relation blockOwnerToRegionRelation = region.getRelation(blockOwnerId);

        if (!regionOwnerId.equals(blockOwnerId) && blockOwnerToRegionRelation == null) return true;

        Relation playerToRegionRelation = region.getRelation(playerId);
        if (playerToRegionRelation != null && playerToRegionRelation.relation().equals("authority")) return true;

        //If block owner is region owner and block relation to region is null, it means this block belongs to the region owner
        if (blockOwnerToRegionRelation == null) return false;

        if (blockOwnerToRegionRelation.relation().equals("member")) {
            return playerId.equals(blockOwnerId);
        }

        if (blockOwnerToRegionRelation.relation().equals("claim")) {
            return blockOwnerToRegionRelation.time().before(wb.getTimestamp());
        }

        return !blockOwnerToRegionRelation.relation().equals("authority");
    }

    public static boolean isBlockProtected(Block b) {
        WildBlock wb = getWildBlock(b);
        if (wb == null) return false;

        Region region = getBlockRegion(b);
        if (region == null) return false;

        if (region.getOwnerId().toString().equals(wb.getOwnerId())) return true;

        Relation blockToRegionRelation = region.getRelation(wb.getOwnerId());
        if (blockToRegionRelation == null) return false;
        if (blockToRegionRelation.relation().equals("claim")) {
            return wb.getTimestamp().before(blockToRegionRelation.time());
        }
        return true;
    }

    public static WildBlock getWildBlock(Block b) {
        Chunk chunk = b.getChunk();

        if (wildBlocks.get(chunk) == null) {
            loadChunkSync(chunk);
        }

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
    private static final HashMap<Block, BukkitTask> restoreHealthTasks = new HashMap<>();

    public static boolean hitBlock(Player p, Block b) {
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

    private static void scheduleRestoreHealth(Block b) {
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

    @EventHandler
    public void onCreeperExplosion(EntityExplodeEvent e) {
        List<Block> blocksToRemove = new ArrayList<>();

        Entity entity = e.getEntity();
        if (!(entity instanceof Creeper)) return;

        Player p = null;
        for (Entity nearbyEntity : entity.getNearbyEntities(10, 10, 10)) {
            if (nearbyEntity instanceof Player) {
                p = (Player) nearbyEntity;
                // Log or perform any other actions you need here
                getPlugin().getLogger().info("Player " + p.getName() + " was near the creeper explosion. "
                        + e.getLocation().getX() + " " + e.getLocation().getY() + " " + e.getLocation().getZ());
            }
        }

        for (Block block : e.blockList()) {
            Block mainBlock = getMainBlock(block);

            if (!blockCanBreak(p == null ? null : p.getUniqueId().toString(), mainBlock) || isUnderDoorBlock(mainBlock)) {
                blocksToRemove.add(block);
            }
        }

        e.blockList().removeAll(blocksToRemove);
    }

    @EventHandler
    public void BlockExplodeEvent(EntityExplodeEvent e){
        if (e.getEntity() instanceof Creeper) return;

        List<Block> blocksToRemove = new ArrayList<>();
        for (Block block : e.blockList()) {
            if (block != null) {
                Block mainBlock = getMainBlock(block);
                if ((!blockCanBreak(null, mainBlock) || isUnderDoorBlock(block)) && !block.getType().toString().contains("GLASS")) {
                    blocksToRemove.add(block);
                }
            }
        }
        e.blockList().removeAll(blocksToRemove);
    }

    @EventHandler
    public void BlockBurnEvent(BlockBurnEvent e){
        Block fire = e.getIgnitingBlock();
        Block b = e.getBlock();
        if (blockCanBreak(null, b)) return;
        if (fire == null) return;
        fire.setType(Material.AIR);
        e.setCancelled(true);
    }

    @EventHandler
    public void FireSpread(BlockSpreadEvent e) {
        if (e.getSource().getType() != Material.FIRE) return;

        boolean isCanceled = checkAllAttachedBlocks(e.getSource());
        if (isCanceled) e.setCancelled(true);
    }

    private boolean checkAllAttachedBlocks(Block fireBlock) {
        // Check below
        Block below = fireBlock.getRelative(BlockFace.DOWN);
        if (!blockCanBreak(null, below)) {
            return true; // If any block fails, return false
        }

        // Check sides
        for (BlockFace face : new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST}) {
            Block side = fireBlock.getRelative(face);
            if (!blockCanBreak(null, side)) {
                return true; // If any block fails, return false
            }
        }

        // Check above
        Block above = fireBlock.getRelative(BlockFace.UP);
        return !blockCanBreak(null, above); // If any block fails, return false
    }
}
