package me.harpervenom.wildark.classes;

import me.harpervenom.wildark.WILDARK;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class HoloBlock {

    private Player p;
    private Location loc;
    private BukkitRunnable particleTask;

    public HoloBlock(Player p, Location loc, String color) {
        this.p = p;
        this.loc = loc;
        createHologram(color);
    }

    private void createHologram(String color) {
        Particle particle;
        switch (color){
            case "blue": particle = Particle.SCRAPE; break;
            case "yellow": particle = Particle.WAX_ON; break;
            default : particle = Particle.WAX_OFF; break;
        }
        int count = color.equals("yellow") ? 30 : 20;
        int offsetY = 2;
        particleTask = new BukkitRunnable() {
            @Override
            public void run() {
                p.spawnParticle(particle, loc.clone(), count, 0, offsetY, 0, 0);
                p.spawnParticle(particle, loc.clone().add(1, 0, 0), count, 0, offsetY, 0, 0);
                p.spawnParticle(particle, loc.clone().add(1, 0, 1), count, 0, offsetY, 0, 0);
                p.spawnParticle(particle, loc.clone().add(0, 0, 1), count, 0, offsetY, 0, 0);
            }
        };
        particleTask.runTaskTimer(WILDARK.getPlugin(), 0L, 10L);
    }

    public void delete() {
        // Cancel the particle task to stop the particles
        if (particleTask != null && !particleTask.isCancelled()) {
            particleTask.cancel();
        }

        // Optionally, nullify references to allow garbage collection
        this.p = null;
        this.loc = null;
        this.particleTask = null;
    }
}
