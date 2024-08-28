package me.harpervenom.wildark.classes;

import me.harpervenom.wildark.WILDARK;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class HoloArea {

    private Player p;
    private Location loc1;
    private Location loc2;

    private BukkitRunnable particleTask;

    public HoloArea(Player p, Location loc1, Location loc2){
        this.loc1 = loc1;
        this.loc2 = loc2;

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

//        Particle.DustOptions dustOptions = new Particle.DustOptions(Color.WHITE,0.5f);

            particleTask = new BukkitRunnable() {
                @Override
                public void run() {
                    //X sides
                    p.spawnParticle(Particle.WAX_OFF, xMinLoc.clone().add(0,0, xMaxLoc == zMinLoc ? 1 : 0), 70, 0, 5, 0, 0);
                    for (int i = xMin; i < xMax+1; i++) {
                        int finalI = i - xMin + 1;
                        if (xSpace != 0 && xSpace != 1 && finalI % xSpace != 0) continue;
                        p.spawnParticle(Particle.WAX_OFF, xMinLoc.clone().add(finalI,0, xMaxLoc == zMinLoc ? 1 : 0), 70, 0, 5, 0, 0);
                    }

                    int z = (xMaxLoc == zMinLoc ? 0 : 1)
                            + (xMaxLoc == zMinLoc ? -zDiff : zDiff);

                    p.spawnParticle(Particle.WAX_OFF, xMinLoc.clone().add(0,0, z), 70, 0, 5, 0, 0);
                    for (int i = xMin; i < xMax+1; i++) {
                        int finalI = i - xMin + 1;
                        if (xSpace != 0 && xSpace != 1 && finalI % xSpace != 0) continue;
                        p.spawnParticle(Particle.WAX_OFF, xMinLoc.clone().add(finalI,0, z), 70, 0, 5, 0, 0);
                    }

                    //Z sides
                    for (int i = zMin; i < zMax+1; i++) {
                        int finalI = i - zMin + 1;
                        if (zSpace != 0 && zSpace != 1 && finalI % zSpace != 0) continue;
                        p.spawnParticle(Particle.WAX_OFF, zMinLoc.clone().add(zMaxLoc == xMinLoc ? 1 : 0,0, finalI), 70, 0, 5, 0, 0);
                    }

                    int x = (zMaxLoc == xMinLoc ? 0 : 1)
                            + (zMaxLoc == xMinLoc ? -xDiff : xDiff);

                    for (int i = zMin; i < zMax+1; i++) {
                        int finalI = i - zMin + 1;
                        if (zSpace != 0 && zSpace != 1 && finalI % zSpace != 0) continue;
                        p.spawnParticle(Particle.WAX_OFF, zMinLoc.clone().add(x,0, finalI), 70, 0, 5, 0, 0);
                    }
                }
            };
            particleTask.runTaskTimer(WILDARK.getPlugin(), 0L, 20L);
    }

    public void delete() {
        // Cancel the particle task to stop the particles
        if (particleTask != null && !particleTask.isCancelled()) {
            particleTask.cancel();
        }

        // Optionally, nullify references to allow garbage collection
        this.p = null;
        this.loc1 = null;
        this.loc2 = null;
        this.particleTask = null;
    }

}
