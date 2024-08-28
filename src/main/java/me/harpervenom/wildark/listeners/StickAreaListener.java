package me.harpervenom.wildark.listeners;

import me.harpervenom.wildark.classes.Region;
import me.harpervenom.wildark.classes.RegionStick;
import me.harpervenom.wildark.classes.WildPlayer;
import me.harpervenom.wildark.database.Database;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class StickAreaListener implements Listener {

    private final Database db;

    public StickAreaListener(Database db) {
        this.db = db;
    }

    @EventHandler
    public void checkArea(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        WildPlayer wildPlayer = db.players.getPlayer(p.getUniqueId().toString());

        if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) return;
        if (e.getHand() == EquipmentSlot.OFF_HAND) return;
        ItemStack tool = e.getItem();
        if (tool == null || tool.getType() != Material.STICK) return;

        if (!GeneralListener.regionStickMap.containsKey(p.getUniqueId())) return;
        RegionStick stick = GeneralListener.regionStickMap.get(p.getUniqueId());
        if (!stick.getMode().equals("Area")) return;

        Block b = e.getClickedBlock();
        if (b == null) return;
        Location bLoc = b.getLocation();

        List<Region> regions = db.regions.scanArea(bLoc.getWorld().getName(), (int)bLoc.getX(), (int)bLoc.getZ(),4);
//        p.sendMessage(String.valueOf(regions.size()));
        displayGridInChat(p,bLoc,regions);
    }

    public void displayGridInChat(Player player, Location clickedLocation, List<Region> regions) {
        // Get player's current location
        Location playerLocation = player.getLocation();
        int playerX = playerLocation.getBlockX();
        int playerZ = playerLocation.getBlockZ();
        String playerWorld = playerLocation.getWorld().getName();

        // Calculate grid boundaries centered around player's location
        int gridMinX = playerX - 4;
        int gridMaxX = playerX + 4;
        int gridMinZ = playerZ - 4;
        int gridMaxZ = playerZ + 4;

        // Iterate through the 9x9 grid and build the grid string
        for (int j = 0; j < 9; j++) { // j represents rows (top to bottom)
            StringBuilder row = new StringBuilder();
            for (int i = 9; i >= 0; i--) { // i represents columns (left to right)
                int x = gridMinX + i; // Calculate x coordinate
                int z = gridMinZ + (8 - j); // Reverse the z coordinate to match text display

                // Determine if this block is the clicked location
                boolean isClickedBlock = (x == clickedLocation.getBlockX()) && (z == clickedLocation.getBlockZ()) && playerWorld.equals(clickedLocation.getWorld().getName());

                // Determine if this block is the player's location
                boolean isPlayerLocation = (x == playerX) && (z == playerZ) && playerWorld.equals(playerWorld);

                // Check if this block is in any region
                boolean isInRegion = false;
                for (Region region : regions) {
                    if (region.contains(x, z)) {
                        isInRegion = true;
                        break;
                    }
                }

                // Assign color based on conditions
                if (isPlayerLocation) {
                    row.append(ChatColor.BLUE).append("# "); // Player location
                } else if (isClickedBlock) {
                    row.append(ChatColor.GOLD).append("# "); // Central block (clicked block)
                } else if (isInRegion) {
                    row.append(ChatColor.RED).append("# "); // Block belongs to a region
                } else {
                    row.append(ChatColor.GRAY).append("# "); // Block does not belong to any region
                }
            }
            player.sendMessage(row.toString());
        }

        // Adding an empty line after the grid for better readability
        player.sendMessage("");
    }


}
