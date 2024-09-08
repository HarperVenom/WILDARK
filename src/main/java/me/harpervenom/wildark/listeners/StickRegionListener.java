package me.harpervenom.wildark.listeners;

import me.harpervenom.wildark.WILDARK;
import me.harpervenom.wildark.classes.Region;
import me.harpervenom.wildark.classes.RegionStick;
import me.harpervenom.wildark.classes.WildPlayer;
import me.harpervenom.wildark.database.Database;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.UUID;

public class StickRegionListener implements Listener {

    private final Database db;

    public StickRegionListener(Database db) {
        this.db = db;
    }

    public static HashMap<UUID, Region> regionMap = new HashMap<>();
    public static HashMap<UUID, Region> existingRegionMap = new HashMap<>();
    HashMap<UUID, Integer> doubleClick = new HashMap<>();

    @EventHandler
    public void InteractEvent(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        Region region = regionMap.get(p.getUniqueId());
        Block b = e.getClickedBlock();
        if (b == null) return;

        if (e.getHand() == EquipmentSlot.OFF_HAND) return;
        ItemStack tool = e.getItem();
        if (tool == null || tool.getType() != Material.STICK) return;

        if (!GeneralListener.regionStickMap.containsKey(p.getUniqueId())) return;
        RegionStick stick = GeneralListener.regionStickMap.get(p.getUniqueId());
        if (!stick.getMode().equals("Region")) return;

        if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (!p.isSneaking()) return;
            if (existingRegionMap.containsKey(p.getUniqueId())) {
                updateRegion(p);
                return;
            }
            if (region != null) {
                createRegion(p, region);
            }
        } else if (e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.LEFT_CLICK_BLOCK) {
            Region existingRegion;
            if (existingRegionMap.containsKey(p.getUniqueId()) && (existingRegionMap.get(p.getUniqueId()).contains(b.getX(), b.getZ())
                    || existingRegionMap.get(p.getUniqueId()).getSelectedCorner() != 0)) {
                existingRegion = existingRegionMap.get(p.getUniqueId());
            } else {
                existingRegion = db.regions.getBlockRegion(b);
            }

            if (existingRegion != null) {
                selectRegion(p, existingRegion, b);
                return;
            }

            selectBorder(p, b);
        }
    }

    public void selectBorder(Player p, Block b) {
        Location loc = b.getLocation();

        if (existingRegionMap.containsKey(p.getUniqueId())){
            return;
        }

        if (!regionMap.containsKey(p.getUniqueId())){
            regionMap.put(p.getUniqueId(),new Region(p, loc.getWorld().getName(), loc.getBlockX(), loc.getBlockZ()));
            p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacy(ChatColor.WHITE + "Первая точка установлена."));
        } else {
            Region region = regionMap.get(p.getUniqueId());
            if (!doubleClick.containsKey(p.getUniqueId())){

                BukkitRunnable task = new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (existingRegionMap.containsKey(p.getUniqueId())) return;
                        region.setFirstCorner((int)loc.getX(), (int)loc.getZ());
                        doubleClick.remove(p.getUniqueId());
                        p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacy(ChatColor.WHITE + "Первая точка установлена."));
                        checkRegion(p);
                    }
                };

                int taskId = task.runTaskLater(WILDARK.getPlugin(), 20).getTaskId();
                doubleClick.put(p.getUniqueId(),taskId);
            } else {
                Bukkit.getScheduler().cancelTask(doubleClick.get(p.getUniqueId()));

                region.setSecondCorner((int)loc.getX(), (int)loc.getZ());
                doubleClick.remove(p.getUniqueId());
                p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacy(ChatColor.WHITE + "Вторая точка установлена."));
                checkRegion(p);
            }
        }
    }

    public void checkRegion(Player p) {
        Region region = regionMap.get(p.getUniqueId());

        WildPlayer wildPlayer = db.players.getPlayer(p.getUniqueId().toString());
        if (region.areaSelected()) {
            ChatColor color = wildPlayer.getAvailableBlocks() >= region.getArea() ? ChatColor.WHITE : ChatColor.RED;
            p.sendMessage(ChatColor.WHITE + "[W] " + ChatColor.GRAY + "Участок: " + ChatColor.WHITE + region.getGrid() + ChatColor.GRAY + ". Необходимо блоков: " + color + region.getArea());

            if (wildPlayer.getAvailableBlocks() < region.getArea()) {
                p.sendMessage(ChatColor.WHITE + "[W] " + ChatColor.GRAY + "У вас недостаточно блоков.");
            } else {
                p.sendMessage(ChatColor.WHITE + "[W] " + ChatColor.GRAY + "ШИФТ + ПКМ чтобы создать");
            }
        }
    }

    public void checkNewRegion(Player p){
        Region oldRegion = existingRegionMap.get(p.getUniqueId());
        Region newRegion = regionMap.get(p.getUniqueId());

        int price = getPrice(oldRegion, newRegion);

        WildPlayer wildPlayer = db.players.getPlayer(p.getUniqueId().toString());
        if (newRegion.areaSelected()) {
            ChatColor color = wildPlayer.getAvailableBlocks() >= price ? ChatColor.WHITE : ChatColor.RED;
            p.sendMessage(ChatColor.WHITE + "[W] " + ChatColor.GRAY + "Новый участок: " + ChatColor.WHITE + newRegion.getGrid() + ChatColor.GRAY
                    + ". Необходимо блоков: " + color + price);

            if (price > 0 && wildPlayer.getAvailableBlocks() < price) {
                p.sendMessage(ChatColor.WHITE + "[W] " + ChatColor.GRAY + "У вас недостаточно блоков.");
            } else {
                p.sendMessage(ChatColor.WHITE + "[W] " + ChatColor.GRAY + "ШИФТ + ПКМ чтобы обновить границы.");
            }
        }
    }

    private static int getPrice(Region oldRegion, Region newRegion) {

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
        int oldDifference = (int) (0.5 * (oldRegion.getArea() - commonBlocks));
        int newDifference = newRegion.getArea() - commonBlocks;

        return newDifference - oldDifference;
    }

    public void createRegion(Player p, Region region) {
        WildPlayer wildPlayer = db.players.getPlayer(p.getUniqueId().toString());
        if (wildPlayer.getAvailableBlocks() < region.getArea()) {
            p.sendMessage(ChatColor.WHITE + "[W] " + ChatColor.GRAY + "У вас недостаточно блоков.");
            return;
        }

        String regionStatus = db.regions.regionStatus(region);

        if (regionStatus.equals("intersect")){
            p.sendMessage(ChatColor.WHITE + "[W] " + ChatColor.GRAY + "Участок пересекается с другими.");
            return;
        } else if (regionStatus.equals("close")) {
            p.sendMessage(ChatColor.WHITE + "[W] " + ChatColor.GRAY + "Рядом участок больше вашего.");
            return;
        }

        db.regions.createRegion(p, region);
        p.sendMessage(ChatColor.WHITE + "[W] " + ChatColor.GREEN + "Вы создали регион.");
        p.sendMessage(ChatColor.WHITE + "[W] " + ChatColor.GRAY + "Потрачено блоков: "
                + ChatColor.WHITE + (region.getArea()) + ChatColor.GRAY + ".");
        clearRegionMap(p);
    }

    public void updateRegion(Player p) {
        Region oldRegion = existingRegionMap.get(p.getUniqueId());
        Region newRegion = regionMap.get(p.getUniqueId());

        int price = getPrice(oldRegion, newRegion);

        WildPlayer wildPlayer = db.players.getPlayer(p.getUniqueId().toString());

        if (price > 0 && wildPlayer.getAvailableBlocks() < price) {
            p.sendMessage(ChatColor.WHITE + "[W] " + ChatColor.GRAY + "У вас недостаточно блоков.");
            return;
        }

        String regionStatus = db.regions.regionStatus(newRegion);

        if (regionStatus.equals("intersect")){
            p.sendMessage(ChatColor.WHITE + "[W] " + ChatColor.GRAY + "Участок пересекается с другими.");
            return;
        }

        p.sendMessage(ChatColor.WHITE + "[W] " + ChatColor.GREEN + "Вы обновили регион.");

        if (price < 0){
            p.sendMessage(ChatColor.WHITE + "[W] " + ChatColor.GRAY + "Возвращено блоков: "
                    + ChatColor.WHITE + (-price) + ChatColor.GRAY + ".");
        } else {
            p.sendMessage(ChatColor.WHITE + "[W] " + ChatColor.GRAY + "Потрачено блоков: "
                    + ChatColor.WHITE + (price) + ChatColor.GRAY + ".");
        }

        clearExistingRegionMap(p);
        clearRegionMap(p);
    }

    public void selectRegion(Player p, Region region, Block b) {

        if (region.getSelectedCorner() != 0) {
            int selectedCorner = region.getSelectedCorner();
            int oppositeX = 0;
            int oppositeZ = 0;

            switch (selectedCorner) {
                case 1: {
                    oppositeX = region.getX2();
                    oppositeZ = region.getZ2();
                    break;
                }
                case 2: {
                    oppositeX = region.getX1();
                    oppositeZ = region.getZ1();
                    break;
                }
                case 3: {
                    oppositeX = region.getX4();
                    oppositeZ = region.getZ4();
                    break;
                }
                case 4: {
                    oppositeX = region.getX3();
                    oppositeZ = region.getZ3();
                    break;
                }
            }
            if (!regionMap.containsKey(p.getUniqueId())){
                Region newRegion = new Region(p,b.getWorld().getName(), oppositeX, oppositeZ);
                newRegion.setId(existingRegionMap.get(p.getUniqueId()).getId());
                newRegion.setSecondCorner(b.getX(), b.getZ());
                newRegion.selectCorner(b.getX(), b.getZ());
                regionMap.put(p.getUniqueId(), newRegion);
                checkNewRegion(p);
            } else {
                Region newRegion = regionMap.get(p.getUniqueId());
                newRegion.setSecondCorner(b.getX(), b.getZ());
                newRegion.selectCorner(b.getX(), b.getZ());
                checkNewRegion(p);
            }

            return;
        }

        if (existingRegionMap.containsKey(p.getUniqueId()) && existingRegionMap.get(p.getUniqueId()).getName().equals(region.getName())){
            Region existing = existingRegionMap.get(p.getUniqueId());
            existing.selectCorner(b.getX(), b.getZ());
            p.sendMessage(ChatColor.WHITE + "[W] " + ChatColor.YELLOW + "Угол выбран.");
            return;
        }

        p.sendMessage(ChatColor.WHITE + "[W] " + ChatColor.YELLOW + "Участок выделен.");
        existingRegionMap.put(p.getUniqueId(), region);
        region.showHolo();
        clearRegionMap(p);
    }

    @EventHandler
    public void PortalEnter(PlayerChangedWorldEvent e){
        Player p = e.getPlayer();
        clearExistingRegionMap(p);
        clearRegionMap(p);
    }

    @EventHandler
    public void PlayerLeave(PlayerQuitEvent e){
        Player p = e.getPlayer();
        clearExistingRegionMap(p);
        clearRegionMap(p);
    }

    public void clearRegionMap(Player p){
        if (StickRegionListener.regionMap.containsKey(p.getUniqueId())) {
            StickRegionListener.regionMap.get(p.getUniqueId()).removeHolo();
            StickRegionListener.regionMap.remove(p.getUniqueId());
        }
    }

    public void clearExistingRegionMap(Player p){
        if (StickRegionListener.existingRegionMap.containsKey(p.getUniqueId())) {
            StickRegionListener.existingRegionMap.get(p.getUniqueId()).removeHolo();
            StickRegionListener.existingRegionMap.remove(p.getUniqueId());
        }
    }
}
