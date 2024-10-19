package me.harpervenom.wildark.listeners.stick;

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

import static me.harpervenom.wildark.WILDARK.db;

public class StickAreaListener implements Listener {

    @EventHandler
    public void checkArea(PlayerInteractEvent e) {
        Player p = e.getPlayer();

        if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) return;
        if (e.getHand() == EquipmentSlot.OFF_HAND) return;
        ItemStack tool = e.getItem();
        if (tool == null || tool.getType() != Material.STICK) return;

        if (!StickModeSwitch.regionStickMap.containsKey(p.getUniqueId())) return;
        RegionStick stick = StickModeSwitch.regionStickMap.get(p.getUniqueId());
        if (!stick.getMode().equals("Area")) return;

        Block b = e.getClickedBlock();
        if (b == null) return;
        Location bLoc = b.getLocation();

        scanArea(p, bLoc,3);
    }

    public void scanArea(Player p ,Location loc, int radius) {
        db.regions.scanArea(loc.getWorld().getName(), loc.getBlockX(), loc.getBlockZ(), radius).thenAccept(regions -> {
            int minX = loc.getBlockX() - radius;
            int maxX = loc.getBlockX() + radius;
            int minZ = loc.getBlockZ() - radius;
            int maxZ = loc.getBlockZ() + radius;

            for (int i = minX; i <= maxX; i++) {
                for (int j = minZ; j <= maxZ; j++) {
                    Color color = Color.GRAY;

                    for (Region region : regions) {
                        if (region.contains(i,j)) {
                            if (region.getOwnerId().equals(p.getUniqueId())) color = Color.LIME;
                            else color = Color.RED;
                        }
                    }

                    showBlock(new Location(loc.getWorld(),i,loc.getY(),j).add(0.5,0.5,0.5), 10, color);
                }
            }
        });
    }

    public void showBlock(Location center, int count, Color color) {
        float offset = 0.5f;

        for (int i = 0; i < count; i++) {
            double x = center.getX() + (Math.random() * 2 - 1) * offset;
            double y = center.getY() + (Math.random() * 2 - 1) * 2*offset;
            double z = center.getZ() + (Math.random() * 2 - 1) * offset;

            Particle.DustOptions dustOptions = new Particle.DustOptions(color,1f);
            center.getWorld().spawnParticle(Particle.DUST, x, y, z, 1, dustOptions);
        }
    }
}
