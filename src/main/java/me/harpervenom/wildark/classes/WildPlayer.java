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
        if (regions != 0) db.players.updateAvailableRegions(id, regions);
        if (blocks != 0) db.players.updateAvailableBlock(id, blocks);
    }

    private void updateBalance() {
        int blocksChange = 20;
        int regionsChange = 0;
        availableBlocks += blocksChange;

        int[] thresholds = {1, 10, 100, 1000};
        int playedHours = getPlayer().getStatistic(Statistic.PLAY_ONE_MINUTE) / 20 / 60 / 60;

        for (int i = 0; i < thresholds.length; i++) {
            if (playedHours >= thresholds[i] && regions.size() + availableRegions <= i + 1) {
                availableRegions++;
                regionsChange = 1;
                break;
            }
        }

        updateBalance(blocksChange, regionsChange);
    }

    private Player getPlayer() {
        return Bukkit.getPlayer(UUID.fromString(id));
    }
}
