package me.harpervenom.wildark.classes;

import java.util.ArrayList;
import java.util.List;

public class WildPlayer {
    private final String id;
    private final int availableBlocks;
    private final int availableRegions;
    private final int minutesPlayed;

    private List<Region> regions = new ArrayList<>();

    public WildPlayer(String id, int availableBlocks, int availableRegions, int minutesPlayed) {
        this.id = id;
        this.availableBlocks = availableBlocks;
        this.availableRegions = availableRegions;
        this.minutesPlayed = minutesPlayed;
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

    public int getMinutesPlayed() {
        return minutesPlayed;
    }

    public void setRegions(List<Region> regions) {
        this.regions = regions;
    }
    public List<Region> getRegions() {
        return regions;
    }
}
