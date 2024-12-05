package me.harpervenom.wildark.classes;

import org.bukkit.Bukkit;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static me.harpervenom.wildark.WILDARK.db;
import static me.harpervenom.wildark.WILDARK.getPlugin;
import static me.harpervenom.wildark.classes.Region.returnCoefficient;

public class WildPlayer {
    private final String id;
    private int availableBlocks;
    private int availableRegions;
    private int accumulator;

    private BukkitTask timer;

    private List<Region> regions = new ArrayList<>();

    public WildPlayer(String id, int availableBlocks, int availableRegions, int accumulator) {
        this.id = id;
        this.availableBlocks = availableBlocks;
        this.availableRegions = availableRegions;
        this.accumulator = accumulator;

        runTimer();
    }

    public String getId() {
        return id;
    }

    public int getAvailableBlocks() {
        return availableBlocks;
    }

    public int getAvailableRegions() {
        return availableRegions;
    }

    public int getAccumulator() {
        return accumulator;
    }

    public void addRegion(Region region) {
        regions.add(region);
    }

    public void deleteRegion(Region region) {
        regions.remove(region);
        updateBalance((int) (returnCoefficient * region.getArea()), 1);
        region.delete();
    }

    public void setRegions(List<Region> regions) {
        this.regions = regions;
    }
    public List<Region> getRegions() {
        return regions;
    }

    private void runTimer() {
        timer = Bukkit.getScheduler().runTaskTimer(getPlugin(), () -> {
            accumulator++;
            if (accumulator >= 60) {
                updateBalance();
                accumulator = 0;
            }
            db.players.updateAccumulator(id, accumulator);
        }, 1200, 1200);
    }

    public void setOffline(boolean isOffline) {
        if (isOffline) timer.cancel();
        else runTimer();
    }

    public void updateBalance(int blocks, int regions) {
        if (regions != 0) {
            db.players.updateAvailableRegions(id, regions);
            getPlayer().sendMessage("Участки: " + (regions > 0 ? "+" + regions : regions));
            availableRegions += regions;
        }
        if (blocks != 0) {
            db.players.updateAvailableBlock(id, blocks);
            getPlayer().sendMessage("Блоки: " + (blocks > 0 ? "+" + blocks : blocks));
            availableBlocks += blocks;
        }
    }

    private void updateBalance() {
        int blocksChange = 12;
        int regionsChange = 0;

        int[] thresholds = {1, 10, 100, 1000};
        int playedHours = (int) Math.ceil(getPlayer().getStatistic(Statistic.PLAY_ONE_MINUTE) / (double) (20 * 60 * 60));

        int highestThresholdReached = regions.size() + availableRegions;

        for (int i = highestThresholdReached; i < thresholds.length; i++) {
            if (playedHours >= thresholds[i]) {
                regionsChange++;
            } else {
                break;
            }
        }

        updateBalance(blocksChange, regionsChange);
    }

    private Player getPlayer() {
        return Bukkit.getPlayer(UUID.fromString(id));
    }
}
