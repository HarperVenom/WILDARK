package me.harpervenom.wildark.classes;

public class WildPlayer {
    private final String id;
    private final int availableBlocks;
    private final int availableRegions;
    private final int minutesPlayed;

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
}
