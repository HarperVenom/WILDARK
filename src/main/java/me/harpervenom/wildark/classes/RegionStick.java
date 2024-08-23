package me.harpervenom.wildark.classes;

public class RegionStick {

    private final String[] modes = {"Block", "Area", "Region"};

    private int index;

    public RegionStick(){
        this.index = 0;
    }

    public void switchMode() {
        index++;
        if (index > modes.length-1){
            index = 0;
        }
    }

    public String getMode() {
        return modes[index];
    }

}
