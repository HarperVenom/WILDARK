package me.harpervenom.wildark.classes;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class WildBlock {

    private Location loc;
    private String ownerId;

    public WildBlock(Location loc, String ownerId){
        this.loc = loc;
        this.ownerId = ownerId;
    }

    public Location getLoc(){
        return loc;
    }
    public String getOwnerId(){
        return ownerId;
    }
}
