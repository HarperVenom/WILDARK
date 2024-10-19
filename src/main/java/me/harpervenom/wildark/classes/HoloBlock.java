package me.harpervenom.wildark.classes;

import me.harpervenom.wildark.WILDARK;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class HoloBlock {

    private final Player p;
    private final Location loc;
    private BukkitRunnable particleTask;

    public HoloBlock(Player p, Location loc, String color) {
        this.p = p;
        this.loc = loc;
        createHologram(color);
    }

    private void createHologram(String color) {
        Particle particle = switch (color) {
            case "blue" -> Particle.SCRAPE;
            case "yellow" -> Particle.WAX_ON;
            default -> Particle.WAX_OFF;
        };
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
        if (particleTask != null && !particleTask.isCancelled()) {
            particleTask.cancel();
        }
    }
}
