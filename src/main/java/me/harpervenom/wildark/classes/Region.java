package me.harpervenom.wildark.classes;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class Region {

    private String name;
    private int id;
//    private Region oldRegion;
    private Integer selectedX;
    private Integer selectedZ;

    String color = "white";

    private Player p;
    private String worldName;

    private boolean firstCornerSet;
    //Lower values
    private int x1;
    private int z1;

    private boolean secondCornerSet;
    //Higher values
    private int x2;
    private int z2;

    private int x3;
    private int z3;

    private int x4;
    private int z4;

    private HoloBlock firstHoloBlock;
    private HoloBlock secondHoloBlock;
    private HoloBlock thirdHoloBlock;
    private HoloBlock fourthHoloBlock;

    private HoloArea holoArea;

    public Region(Player p, String worldName, int x1, int z1) {
        this.p = p;
        this.worldName = worldName;
        this.x1 = x1;
        this.z1 = z1;
        firstCornerSet = true;
        showHolo();
    }

    public Region(int id, Player p, String name, String worldName, int x1, int z1, int x2, int z2) {
        this.id = id;
        this.p = p;
        this.worldName = worldName;
        this.name = name;

        this.x1 = x1;
        this.z1 = z1;
        firstCornerSet = true;

        this.x2 = x2;
        this.z2 = z2;
        secondCornerSet = true;

        this.x3 = x1;
        this.z3 = z2;

        this.x4 = x2;
        this.z4 = z1;

        this.color = color;
    }

    public Player getOwner() {
        return p;
    }
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public void setId(int id){
        this.id = id;
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

    public int getX3() {
        return x3;
    }
    public int getZ3() {
        return z3;
    }

    public int getX4() {
        return x4;
    }
    public int getZ4() {
        return z4;
    }

    public int getWidth() {
        return Math.abs(x1 - x2) + 1;
    }
    public int getLength() {
        return Math.abs(z1 - z2) + 1;
    }

    public String getWorldName() {
        return worldName;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public void setFirstCorner(int x, int z){
        x1 = x;
        z1 = z;
        firstCornerSet = true;

        this.x3 = x1;
        this.z3 = z2;

        this.x4 = x2;
        this.z4 = z1;

        showHolo();
    }

    public void setSecondCorner(int x, int z){
        x2 = x;
        z2 = z;
        secondCornerSet = true;

        this.x3 = x1;
        this.z3 = z2;

        this.x4 = x2;
        this.z4 = z1;

        showHolo();
    }

    public boolean selectCorner(int x, int z) {
        if ((x != x1 && z != z1) && (x != x2 && z != z2) && (x != x3 && z != z3) && (x != x4 && z != z4)) return false;
        selectedX = x;
        selectedZ = z;
        showHolo();
        return true;
    }

    public void removeSelectedCorner() {
        selectedX = null;
        selectedZ = null;
    }

    public int getSelectedCorner() {
        if (selectedX == null || selectedZ == null) return 0;

        if (selectedX == x1 && selectedZ == z1) return 1;
        if (selectedX == x2 && selectedZ == z2) return 2;
        if (selectedX == x3 && selectedZ == z3) return 3;
        if (selectedX == x4 && selectedZ == z4) return 4;

        return 0;
    }

    public void showHolo() {
        if (firstHoloBlock != null) firstHoloBlock.delete();
        if (secondHoloBlock != null) secondHoloBlock.delete();
        if (thirdHoloBlock != null) thirdHoloBlock.delete();
        if (fourthHoloBlock != null) fourthHoloBlock.delete();
        if (holoArea != null) holoArea.delete();

        if (firstCornerSet) {
            firstHoloBlock = new HoloBlock(p,new Location(Bukkit.getWorld(worldName),x1,p.getLocation().getY(),z1), getSelectedCorner() == 1 ? "yellow" : color);
        }
        if (secondCornerSet) {
            secondHoloBlock = new HoloBlock(p,new Location(Bukkit.getWorld(worldName),x2,p.getLocation().getY(),z2), getSelectedCorner() == 2 ? "yellow" : color);
        }

        if (firstCornerSet && secondCornerSet) {
            holoArea = new HoloArea(p,new Location(Bukkit.getWorld(worldName),x1,p.getLocation().getY(),z1),
                    new Location(Bukkit.getWorld(worldName),x2,p.getLocation().getY(),z2), color);

            thirdHoloBlock = new HoloBlock(p,new Location(Bukkit.getWorld(worldName),x1,p.getLocation().getY(),z2), getSelectedCorner() == 3 ? "yellow" : color);
            fourthHoloBlock = new HoloBlock(p,new Location(Bukkit.getWorld(worldName),x2,p.getLocation().getY(),z1), getSelectedCorner() == 4 ? "yellow" : color);
        }
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

    public List<Chunk> getChunks() {
        List<Chunk> chunks = new ArrayList<>();
        World world = Bukkit.getWorld(worldName);

        // Calculate the chunk boundaries based on the region's coordinates
        int minChunkX = Math.min(x1, x2) / 16;
        int maxChunkX = Math.max(x1, x2) / 16;
        int minChunkZ = Math.min(z1, z2) / 16;
        int maxChunkZ = Math.max(z1, z2) / 16;

        // Iterate over all chunks that the region spans and add them to the list
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                chunks.add(world.getChunkAt(chunkX, chunkZ));
            }
        }

        return chunks;
    }

    public void removeHolo() {

        if (firstHoloBlock != null) firstHoloBlock.delete();
        if (secondHoloBlock != null) secondHoloBlock.delete();
        if (thirdHoloBlock != null) thirdHoloBlock.delete();
        if (fourthHoloBlock != null) fourthHoloBlock.delete();
        if (holoArea != null) holoArea.delete();
    }
}
