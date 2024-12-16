package me.harpervenom.wildark.classes;

import me.harpervenom.wildark.WILDARK;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static me.harpervenom.wildark.WILDARK.db;
import static me.harpervenom.wildark.WILDARK.getPlugin;
import static me.harpervenom.wildark.listeners.PlayerListener.getWildPlayer;
import static me.harpervenom.wildark.listeners.stick.StickRegionListener.resetSelection;
import static me.harpervenom.wildark.listeners.WildChunksListener.wildRegions;

public class Region {

    private String name;
    private int id;
    public Region updatedRegion;

    private Integer selectedX;
    private Integer selectedZ;

    String color = "white";



    private final UUID ownerID;
    private List<Relation> relations = new ArrayList<>();


    private final String worldName;

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

    public static double returnCoefficient = 0.5;

    public Region(Player p, String worldName, int x1, int z1) {
        this.ownerID = p.getUniqueId();
        this.worldName = worldName;
        setFirstCorner(x1, z1);
        showHolo();
    }

    public Region(int id, UUID playerID, String name, String worldName, int x1, int z1, int x2, int z2) {
        this.id = id;
        this.ownerID = playerID;
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
    }

    public UUID getOwnerId() {
        return ownerID;
    }
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean exists() {
        return name != null;
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


        getPlayer().spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacy(ChatColor.YELLOW + "Первая точка установлена."));
        checkRegion();
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

        getPlayer().spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacy(ChatColor.YELLOW + "Вторая точка установлена."));
        checkRegion();
    }

    public void select() {
        setColor("blue");
        showHolo();
        getPlayer().spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacy(ChatColor.YELLOW + "Участок '" + name + "' выделен."));
    }

    public void selectCorner(Block b) {
        int x = b.getX();
        int z = b.getZ();
        Player p = getPlayer();
        if (!((x == x1 && z == z1) || (x == x2 && z == z2) || (x == x3 && z == z3) || (x == x4 && z == z4))) return;
        selectedX = x;
        selectedZ = z;
        showHolo();

        p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacy(ChatColor.YELLOW + "Угол выбран."));

        if (updatedRegion != null) {
            int[] opposite = getCornerOppositeToSelected();
            updatedRegion.x1 = opposite[0];
            updatedRegion.z1 = opposite[1];
            updatedRegion.showHolo();
            checkNew();
        }
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
        Player p = getPlayer();

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

    public boolean contains(Block b){
        int x = b.getX();
        int z = b.getZ();

        return contains(x, z);
    }

    public boolean contains(int x, int z){
        int minX = Math.min(x1, x2);
        int maxX = Math.max(x1, x2);
        int minZ = Math.min(z1, z2);
        int maxZ = Math.max(z1, z2);

        return (x >= minX && x <= maxX && z >= minZ && z <= maxZ);
    }

    public void removeHolo() {

        if (firstHoloBlock != null) firstHoloBlock.delete();
        if (secondHoloBlock != null) secondHoloBlock.delete();
        if (thirdHoloBlock != null) thirdHoloBlock.delete();
        if (fourthHoloBlock != null) fourthHoloBlock.delete();
        if (holoArea != null) holoArea.delete();
    }

    public static Region getPlayerRegion(Player p, Block b) {
        return wildRegions.stream().filter(region -> region.contains(b.getX(), b.getZ()) && region.getOwnerId().equals(p.getUniqueId())).findFirst()
                .orElse(null);
    }

    public void checkRegion() {
        Player p = getPlayer();

        WildPlayer wildPlayer = getWildPlayer(p);
        if (areaSelected()) {
            ChatColor color = wildPlayer.getAvailableBlocks() >= getArea() ? ChatColor.GREEN : ChatColor.RED;
            p.sendMessage("Участок: " + getGrid() + ". Необходимо блоков: " + color + getArea());

            if (wildPlayer.getAvailableBlocks() < getArea()) {
                p.sendMessage(ChatColor.RED + "У вас недостаточно блоков.");
            } else {
                p.sendMessage("ШИФТ + ПКМ чтобы создать");
            }
        }
    }

    public void create() {
        Player p = getPlayer();

        WildPlayer wp = getWildPlayer(p);
        if (wp.getAvailableBlocks() < getArea()) {
            p.sendMessage(ChatColor.RED + "У вас недостаточно блоков.");
            return;
        }

        if (wp.getAvailableRegions() < 1){
            p.sendMessage(ChatColor.RED + "У вас максимальное число участков.");
            return;
        }

        db.regions.regionStatus(this).thenAccept(regionStatus -> {
            if (regionStatus.equals("intersect")){
                p.sendMessage(ChatColor.RED + "Участок пересекается с другими.");
                return;
            } else if (regionStatus.equals("close")) {
                p.sendMessage(ChatColor.RED + "Рядом участок больше вашего.");
                return;
            }

            db.regions.createRegion(this).thenAccept((createdRegion) -> {

                wildRegions.add(createdRegion);

                int price = createdRegion.getArea();

                wp.addRegion(createdRegion);
                wp.updateBalance(-price, -1);

                p.sendMessage(ChatColor.GREEN + "Вы создали участок.");
//                p.sendMessage("Потрачено блоков: " + (price) + ".");

                resetSelection(p);
            });
        });

        getPlugin().getLogger().info(getPlayer().getName() + " создал участок: " + name);
    }

    public void update() {
        Player p = getPlayer();
        int price = getUpdatePrice();

        WildPlayer wp = getWildPlayer(p);

        if (price > 0 && wp.getAvailableBlocks() < price) {
            p.sendMessage(ChatColor.RED + "У вас недостаточно блоков.");
            return;
        }

        db.regions.regionStatus(updatedRegion).thenAccept(regionStatus -> Bukkit.getScheduler().runTask(WILDARK.getPlugin(), () -> {
            if (regionStatus.equals("intersect")){
                p.sendMessage(ChatColor.RED + "Участок пересекается с другими.");
                return;
            }
//            else if (regionStatus.equals("close")) {
//                p.sendMessage(ChatColor.WHITE + "[W] " + ChatColor.RED + "Рядом участок больше вашего.");
//                return;
//            }

            db.regions.updateRegion(updatedRegion).thenAccept(updated -> Bukkit.getScheduler().runTask(WILDARK.getPlugin(), () -> {{
                if (!updated) {
                    p.sendMessage(ChatColor.RED + "Не удалось обновить участок.");
                    return;
                }

                wildRegions = wildRegions.stream().filter(region -> region.getId() != id).collect(Collectors.toList());
                wildRegions.add(updatedRegion);

                wp.updateBalance(-price, 0);

                p.sendMessage(ChatColor.GREEN + "Вы обновили участок.");

                updatedRegion.removeSelectedCorner();

//                if (price < 0){
//                    p.sendMessage("Возвращено блоков: " + (-price) + ".");
//                } else {
//                    p.sendMessage("Потрачено блоков: " + (price) + ".");
//                }

                resetSelection(p);
            }}));
        }));
        getPlugin().getLogger().info(getPlayer().getName() + " обновил участок: " + name);
    }

    public void delete() {
        resetSelection(getPlayer());

        wildRegions.remove(this);
        db.regions.deleteRegion(id);
        getPlugin().getLogger().info(getPlayer().getName() + " удалил участок: " + name);
    }

    public void selectNew(Block b) {
        int selectedCorner = getSelectedCorner();

        if (selectedCorner != 0) {
            int[] opposite = getCornerOppositeToSelected();
            int oppositeX = opposite[0];
            int oppositeZ = opposite[1];

            if (updatedRegion == null){
                Region newRegion = new Region(getId(), ownerID, getName(), b.getWorld().getName(), oppositeX, oppositeZ, b.getX(), b.getZ());
                newRegion.selectCorner(b);

                updatedRegion = newRegion;
            } else {
                updatedRegion.x2 = b.getX();
                updatedRegion.z2 = b.getZ();
                updatedRegion.secondCornerSet = true;

                updatedRegion.x3 = x1;
                updatedRegion.z3 = z2;

                updatedRegion.x4 = x2;
                updatedRegion.z4 = z1;

                updatedRegion.selectCorner(b);
            }

            checkNew();
        }
    }

    public void checkNew(){
        Player p = getPlayer();

        int price = getUpdatePrice();

        WildPlayer wildPlayer = getWildPlayer(p);
        if (updatedRegion.areaSelected()) {
            ChatColor color = wildPlayer.getAvailableBlocks() >= price ? ChatColor.GREEN : ChatColor.RED;
            p.sendMessage("Новый участок: " + updatedRegion.getGrid()
                    + (price > 0 ? ". Необходимо блоков: " + color + price : ". Будет возвращено блоков: " + color + (-price)));

            if (price > 0 && wildPlayer.getAvailableBlocks() < price) {
                p.sendMessage(ChatColor.RED + "У вас недостаточно блоков.");
            } else {
                p.sendMessage("ШИФТ + ПКМ чтобы обновить границы.");
            }
        }
    }

    private int getUpdatePrice() {
        Region oldRegion = this;
        Region newRegion = updatedRegion;

        int oldX1 = Math.min(oldRegion.getX1(), oldRegion.getX2());
        int oldX2 = Math.max(oldRegion.getX1(), oldRegion.getX2());
        int oldZ1 = Math.min(oldRegion.getZ1(), oldRegion.getZ2());
        int oldZ2 = Math.max(oldRegion.getZ1(), oldRegion.getZ2());

        // Ensure that the coordinates are ordered correctly for newRegion
        int newX1 = Math.min(newRegion.getX1(), newRegion.getX2());
        int newX2 = Math.max(newRegion.getX1(), newRegion.getX2());
        int newZ1 = Math.min(newRegion.getZ1(), newRegion.getZ2());
        int newZ2 = Math.max(newRegion.getZ1(), newRegion.getZ2());

        // Calculate the overlap region
        int overlapX1 = Math.max(oldX1, newX1);
        int overlapZ1 = Math.max(oldZ1, newZ1);
        int overlapX2 = Math.min(oldX2, newX2);
        int overlapZ2 = Math.min(oldZ2, newZ2);

        int commonBlocks = 0;
        if (overlapX1 <= overlapX2 && overlapZ1 <= overlapZ2) {
            int overlapWidth = overlapX2 - overlapX1 + 1;
            int overlapHeight = overlapZ2 - overlapZ1 + 1;
            commonBlocks = overlapWidth * overlapHeight;
        }

        //old difference but 50%
        int oldDifference = (int) (returnCoefficient * (oldRegion.getArea() - commonBlocks));
        int newDifference = newRegion.getArea() - commonBlocks;

        return newDifference - oldDifference;
    }

    private int[] getCornerOppositeToSelected() {
        int oppositeX = 0;
        int oppositeZ = 0;

        switch (getSelectedCorner()) {
            case 1: {
                oppositeX = getX2();
                oppositeZ = getZ2();
                break;
            }
            case 2: {
                oppositeX = getX1();
                oppositeZ = getZ1();
                break;
            }
            case 3: {
                oppositeX = getX4();
                oppositeZ = getZ4();
                break;
            }
            case 4: {
                oppositeX = getX3();
                oppositeZ = getZ3();
                break;
            }
        }

        return new int[] {oppositeX, oppositeZ};
    }

    public void reset() {
        removeHolo();
        removeSelectedCorner();
        if (updatedRegion != null) {
            updatedRegion.reset();
            updatedRegion = null;
        }
    }

    private Player getPlayer() {
        return Bukkit.getPlayer(ownerID);
    }

    public void setRelations(List<Relation> relations) {
        this.relations = relations;
    }

    public void addRelation(UUID playerId, String relationValue) {
        if (relationValue.equals("clear")) {
            relations = relations.stream().filter(relation -> !relation.playerId().equals(playerId.toString())).collect(Collectors.toList());
            db.regions.removeRelation(playerId, id).thenAccept(removed -> {
                if (!removed) {
                    getPlayer().sendMessage(ChatColor.RED + "Не удалось удалить отношение.");
                    return;
                }

                getPlayer().sendMessage(ChatColor.GREEN + "Отношение успешно удалено.");
            });
            return;
        }

        Timestamp timestamp = new java.sql.Timestamp(System.currentTimeMillis());
        Relation relation = new Relation(playerId.toString(), relationValue, timestamp);
        relations = relations.stream().filter(currentRelation -> !currentRelation.playerId().equals(playerId.toString())).collect(Collectors.toList());
        relations.add(relation);

        db.regions.addRelation(playerId, id, relationValue, timestamp).thenAccept((isAdded) -> {
            if (!isAdded) {
                relations.remove(relation);
                getPlayer().sendMessage(ChatColor.RED + "Не удалось установить отношение");
            } else {
                getPlayer().sendMessage(ChatColor.GREEN + "Вы успешно установили отношение: " + relation.relation());
                getPlugin().getLogger().info(getPlayer().getName() + " создал отношение для участка: " + name + " с " + Bukkit.getPlayer(playerId).getName() + ": " + relation.relation());
            }
        });
    }

    public List<Relation> getRelations() {
        return relations;
    }

    public Relation getRelation(String playerId) {
        return relations.stream().filter((relation) -> relation.playerId().equals(playerId)).findFirst().orElse(null);
    }
}
