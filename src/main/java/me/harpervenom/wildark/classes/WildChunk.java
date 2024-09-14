package me.harpervenom.wildark.classes;

import java.util.ArrayList;
import java.util.List;

public class WildChunk {

    private List<WildBlock> wildBlocks = new ArrayList<>();

    public void setBlocks(List<WildBlock> wildBlocks){
        this.wildBlocks = wildBlocks;
    }

}
