package me.harpervenom.wildark.listeners;

import me.harpervenom.wildark.classes.Region;
import me.harpervenom.wildark.classes.RegionStick;
import me.harpervenom.wildark.database.Database;
import org.bukkit.*;
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
//        WildPlayer wildPlayer = db.players.getPlayer(p.getUniqueId().toString());

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


//        showBlock(bLoc.clone().add(0.5,0.5,0.5),10);
        scanArea(p, bLoc,3);



    }

    public void scanArea(Player p ,Location loc, int radius) {

        List<Region> regions = db.regions.scanArea(loc.getWorld().getName(), loc.getBlockX(), loc.getBlockZ(),5);



        int minX = loc.getBlockX() - radius;
        int maxX = loc.getBlockX() + radius;
        int minZ = loc.getBlockZ() - radius;
        int maxZ = loc.getBlockZ() + radius;

        for (int i = minX; i <= maxX; i++) {
            for (int j = minZ; j <= maxZ; j++) {
                Color color = Color.GRAY;

                for (Region region : regions) {
                    if (region.contains(i,j)) {
                        if (region.getOwner().equals(p)) color = Color.LIME;
                        else color = Color.RED;
                    }
                }

                showBlock(new Location(loc.getWorld(),i,loc.getY(),j).add(0.5,0.5,0.5), 10, color);
            }
        }
    }

    public void showBlock(Location center, int count, Color color) {
        float offset = 0.5f; // Half-block offset for each axis

        for (int i = 0; i < count; i++) {
            double x = center.getX() + (Math.random() * 2 - 1) * offset;
            double y = center.getY() + (Math.random() * 2 - 1) * 2*offset;
            double z = center.getZ() + (Math.random() * 2 - 1) * offset;

            Particle.DustOptions dustOptions = new Particle.DustOptions(color,1f);
            center.getWorld().spawnParticle(Particle.DUST, x, y, z, 1, dustOptions);
        }
    }
}
