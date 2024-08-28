package me.harpervenom.wildark.classes;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class Region {

    private Player p;
    private String worldName;

    private boolean firstCornerSet;
    private int x1;
    private int z1;

    private boolean secondCornerSet;
    private int x2;
    private int z2;

    private HoloBlock firstHoloBlock;
//    private HoloBlock secondHoloBlock;

    private HoloArea holoArea;

    public Region(Player p, String worldName, int x1, int z1) {
        this.p = p;
        this.worldName = worldName;
        this.x1 = x1;
        this.z1 = z1;
        firstCornerSet = true;
        firstHoloBlock = new HoloBlock(p,new Location(Bukkit.getWorld(worldName),x1,p.getLocation().getY(),z1));
    }

    public Region(Player p, String worldName, int x1, int z1, int x2, int z2) {
        this.p = p;
        this.worldName = worldName;
        this.x1 = x1;
        this.z1 = z1;
        this.x2 = x2;
        this.z2 = z2;
    }

    public String getWorldName() {
        return worldName;
    }

    public void setFirstCorner(int x, int z){
        x1 = x;
        z1 = z;
        firstCornerSet = true;

        if (firstHoloBlock != null){
            firstHoloBlock.delete();
        }


        if (secondCornerSet) {
            if (holoArea != null) {
                holoArea.delete();
            }
            holoArea = new HoloArea(p,new Location(Bukkit.getWorld(worldName),x1,p.getLocation().getY(),z1),
                    new Location(Bukkit.getWorld(worldName),x2,p.getLocation().getY(),z2));
        } else {
            firstHoloBlock = new HoloBlock(p,new Location(Bukkit.getWorld(worldName),x1,p.getLocation().getY(),z1));
        }
    }

    public void setSecondCorner(int x, int z){
        x2 = x;
        z2 = z;
        secondCornerSet = true;

//        if (secondHoloBlock != null) {
//            secondHoloBlock.delete();
//        }
//        secondHoloBlock = new HoloBlock(p,new Location(Bukkit.getWorld(worldName),x2,p.getLocation().getY(),z2));

        if (firstCornerSet) {
            firstHoloBlock.delete();
            if (holoArea != null) {
                holoArea.delete();
            }
            holoArea = new HoloArea(p,new Location(Bukkit.getWorld(worldName),x1,p.getLocation().getY(),z1),
                    new Location(Bukkit.getWorld(worldName),x2,p.getLocation().getY(),z2));
        }
    }

    public int getX1() {
        return x1;
    }
    public int getZ1() {
        return z1;
    }

    public int getX2() {
        return x2;
    }
    public int getZ2() {
        return z2;
    }

    public boolean areaSelected() {
        return firstCornerSet && secondCornerSet;
    }

    public String getGrid() {
        int length = Math.abs(x1 - x2) + 1;
        int width = Math.abs(z1 - z2) + 1;
        return length + "x" + width;
    }

    public int getArea(){
        int xDifference = Math.abs(x1 - x2) + 1;
        int zDifference = Math.abs(z1 - z2) + 1;
        return xDifference * zDifference;
    }

    public boolean contains(int x, int z){
        int minX = Math.min(x1, x2);
        int maxX = Math.max(x1, x2);
        int minZ = Math.min(z1, z2);
        int maxZ = Math.max(z1, z2);

        return (x >= minX && x <= maxX && z >= minZ && z <= maxZ);
    }
}
