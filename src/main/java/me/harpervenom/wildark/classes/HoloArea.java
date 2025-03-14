package me.harpervenom.wildark.classes;

import me.harpervenom.wildark.WILDARK;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Objects;

public class HoloArea {

    private final BukkitRunnable particleTask;

    public HoloArea(Player p, Location loc1, Location loc2, String color){
        Location xMinLoc = loc1.getX() < loc2.getX() ? loc1 : loc2;
        Location zMinLoc = loc1.getZ() < loc2.getZ() ? loc1 : loc2;
        Location xMaxLoc = loc1.getX() > loc2.getX() ? loc1 : loc2;
        Location zMaxLoc = loc1.getZ() > loc2.getZ() ? loc1 : loc2;

        int xMin = (int) xMinLoc.getX();
        int zMin = (int) zMinLoc.getZ();

        int xMax = (int) xMaxLoc.getX();
        int zMax = (int) zMaxLoc.getZ();

        int xDiff = Math.abs(xMin - xMax);
        int zDiff = Math.abs(zMin - zMax);

        int xSpace = xDiff / 10;
        int zSpace = zDiff / 10;

        Particle particle = Objects.equals(color, "white") ? Particle.WAX_OFF : Particle.SCRAPE;

        int count = 5;
        int offsetY = 2;

            particleTask = new BukkitRunnable() {
                @Override
                public void run() {
                    //X sides
                    p.spawnParticle(particle, xMinLoc.clone().add(0,0, xMaxLoc == zMinLoc ? 1 : 0), count, 0, offsetY, 0, 0);
                    for (int i = xMin; i < xMax+1; i++) {
                        int finalI = i - xMin + 1;
                        if (xSpace != 0 && xSpace != 1 && finalI % xSpace != 0) continue;
                        p.spawnParticle(particle, xMinLoc.clone().add(finalI,0, xMaxLoc == zMinLoc ? 1 : 0), count, 0, offsetY, 0, 0);
                    }

                    int z = (xMaxLoc == zMinLoc ? 0 : 1)
                            + (xMaxLoc == zMinLoc ? -zDiff : zDiff);

                    p.spawnParticle(particle, xMinLoc.clone().add(0,0, z), count, 0, offsetY, 0, 0);
                    for (int i = xMin; i < xMax+1; i++) {
                        int finalI = i - xMin + 1;
                        if (xSpace != 0 && xSpace != 1 && finalI % xSpace != 0) continue;
                        p.spawnParticle(particle, xMinLoc.clone().add(finalI,0, z), count, 0, offsetY, 0, 0);
                    }

                    //Z sides
                    for (int i = zMin; i < zMax+1; i++) {
                        int finalI = i - zMin + 1;
                        if (zSpace != 0 && zSpace != 1 && finalI % zSpace != 0) continue;
                        p.spawnParticle(particle, zMinLoc.clone().add(zMaxLoc == xMinLoc ? 1 : 0,0, finalI), count, 0, offsetY, 0, 0);
                    }

                    int x = (zMaxLoc == xMinLoc ? 0 : 1)
                            + (zMaxLoc == xMinLoc ? -xDiff : xDiff);

                    for (int i = zMin; i < zMax+1; i++) {
                        int finalI = i - zMin + 1;
                        if (zSpace != 0 && zSpace != 1 && finalI % zSpace != 0) continue;
                        p.spawnParticle(particle, zMinLoc.clone().add(x,0, finalI), count, 0, offsetY, 0, 0);
                    }
                }
            };
            particleTask.runTaskTimer(WILDARK.getPlugin(), 0L, 20L);
    }

    public void delete() {
        if (particleTask != null && !particleTask.isCancelled()) {
            particleTask.cancel();
        }
    }
}
