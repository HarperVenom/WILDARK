package me.harpervenom.wildark.classes;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

public class Region {

    private Location firstCorner;
    private Location secondCorner;

    public Region(Location firstCorner) {
        this.firstCorner = firstCorner;
    }

    public void setFirstCorner(Location b){
        firstCorner = b;
    }

    public void setSecondCorner(Location b){
        secondCorner = b;
    }

    public Location getFirstCorner() {
        return firstCorner;
    }

    public Location getSecondCorner() {
        return secondCorner;
    }

    public boolean areaSelected() {
        return firstCorner != null && secondCorner != null;
    }

    public String getGrid() {
        int length = (int) Math.abs(firstCorner.getX() - secondCorner.getX()) + 1;
        int width = (int) Math.abs(firstCorner.getZ() - secondCorner.getZ()) + 1;
        return length + "x" + width;
    }

    public int getArea(){
        int xDifference = (int) Math.abs(firstCorner.getX() - secondCorner.getX()) + 1;
        int zDifference = (int) Math.abs(firstCorner.getZ() - secondCorner.getZ()) + 1;
        return xDifference * zDifference;
    }
}
