package me.harpervenom.wildark.listeners;

import me.harpervenom.wildark.WILDARK;
import me.harpervenom.wildark.classes.Region;
import me.harpervenom.wildark.classes.RegionStick;
import me.harpervenom.wildark.classes.WildPlayer;
import me.harpervenom.wildark.database.Database;
import me.harpervenom.wildark.database.managers.PlayersManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.HashMap;
import java.util.UUID;

public class StickRegionListener implements Listener {

    private final Database db;

    public StickRegionListener(Database db) {
        this.db = db;
    }

    HashMap<UUID, Region> regionMap = new HashMap<>();
    HashMap<UUID, Integer> doubleClick = new HashMap<>();

    @EventHandler
    public void SelectBorders(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        WildPlayer wildPlayer = db.players.getPlayer(p.getUniqueId().toString());

        if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) return;
        if (e.getHand() == EquipmentSlot.OFF_HAND) return;
        ItemStack tool = e.getItem();
        if (tool == null || tool.getType() != Material.STICK) return;

        if (!GeneralListener.regionStickMap.containsKey(p.getUniqueId())) return;
        RegionStick stick = GeneralListener.regionStickMap.get(p.getUniqueId());
        if (!stick.getMode().equals("Region")) return;

        Block b = e.getClickedBlock();
        if (b == null) return;
        Location bLoc = b.getLocation();
        if (!regionMap.containsKey(p.getUniqueId())){
            regionMap.put(p.getUniqueId(),new Region(bLoc));
            p.sendMessage(ChatColor.GRAY + "- Первая точка установлена.");
        } else {
            Region region = regionMap.get(p.getUniqueId());
            if (!doubleClick.containsKey(p.getUniqueId())){

                BukkitRunnable task = new BukkitRunnable() {
                    @Override
                    public void run() {
                        region.setFirstCorner(bLoc);
                        doubleClick.remove(p.getUniqueId());
                        p.sendMessage(ChatColor.GRAY + "- Первая точка установлена.");
                        checkRegion(p,wildPlayer,region);
                    }
                };

                int taskId = task.runTaskLater(WILDARK.getPlugin(), 20).getTaskId();
                doubleClick.put(p.getUniqueId(),taskId);
            } else {
                Bukkit.getScheduler().cancelTask(doubleClick.get(p.getUniqueId()));

                region.setSecondCorner(bLoc);
                doubleClick.remove(p.getUniqueId());
                p.sendMessage(ChatColor.GRAY + "-- Вторая точка установлена.");
                checkRegion(p,wildPlayer,region);
            }

        }
    }

    @EventHandler
    public void CreateRegion(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (!p.isSneaking()) return;

        if (e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.LEFT_CLICK_BLOCK) return;
        if (e.getHand() == EquipmentSlot.OFF_HAND) return;
        ItemStack tool = e.getItem();
        if (tool == null || tool.getType() != Material.STICK) return;

        if (!GeneralListener.regionStickMap.containsKey(p.getUniqueId())) return;
        RegionStick stick = GeneralListener.regionStickMap.get(p.getUniqueId());
        if (!stick.getMode().equals("Region")) return;

        WildPlayer wildPlayer = db.players.getPlayer(p.getUniqueId().toString());
        Region region = regionMap.get(p.getUniqueId());
        if (wildPlayer.getAvailableBlocks() >= region.getArea()) {
            db.regions.createRegion(wildPlayer, region);
            p.sendMessage("Вы успешно создали регион.");
            regionMap.remove(p.getUniqueId());
        }
    }

    public String composePointMessage(int number, Location bLoc){
        return ChatColor.GRAY + "(" + number + ") X "
                + ChatColor.WHITE + (int) bLoc.getX()
                + ChatColor.GRAY + " Z "
                + ChatColor.WHITE + (int) bLoc.getZ();
    }

    public void checkRegion(Player p, WildPlayer wildPlayer, Region region) {
        if (region.areaSelected()) {
            ChatColor color = wildPlayer.getAvailableBlocks() >= region.getArea() ? ChatColor.GREEN : ChatColor.RED;
            p.sendMessage(ChatColor.GRAY + "--- Выделенный участок: " + ChatColor.WHITE + region.getGrid() + ChatColor.GRAY + ". Кол-во блоков: " + color + region.getArea());
//            p.sendMessage(ChatColor.GRAY + "-----------------------------");
//            p.sendMessage(ChatColor.GRAY + "Выделенный участок: ");
//            p.sendMessage(composePointMessage(1,region.getFirstCorner()));
//            p.sendMessage(composePointMessage(2,region.getSecondCorner()));
//            p.sendMessage(ChatColor.GRAY + "Кол-во блоков: " + color + region.getArea());

            if (wildPlayer.getAvailableBlocks() < region.getArea()) {
                p.sendMessage(ChatColor.GRAY + "У вас недостаточно блоков для выделенного участка.");
            } else {
                p.sendMessage(ChatColor.GRAY + "Чтобы создать участок, нажмите ШИФТ + ПКМ.");
            }
            p.sendMessage("\n");
        }
    }
}
