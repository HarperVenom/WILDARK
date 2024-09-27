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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static me.harpervenom.wildark.listeners.BlockListener.*;
import static me.harpervenom.wildark.listeners.PlayerListener.getWildPlayer;
import static me.harpervenom.wildark.listeners.WildChunksListener.*;

public class StickRegionListener implements Listener {

    private final Database db;

    public StickRegionListener(Database db) {
        this.db = db;
    }

    public static HashMap<UUID, Region> regionMap = new HashMap<>();
    public static HashMap<UUID, Region> selectedRegionMap = new HashMap<>();
    HashMap<UUID, Integer> doubleClick = new HashMap<>();

    @EventHandler
    public void InteractEvent(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        Region region = regionMap.get(p.getUniqueId());
        Block b = e.getClickedBlock();

        if (e.getHand() == EquipmentSlot.OFF_HAND) return;
        ItemStack tool = e.getItem();
        if (tool == null || tool.getType() != Material.STICK) return;

        if (!GeneralListener.regionStickMap.containsKey(p.getUniqueId())) return;
        RegionStick stick = GeneralListener.regionStickMap.get(p.getUniqueId());
        if (!stick.getMode().equals("Region")) return;

        if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (!p.isSneaking()) return;
            if (selectedRegionMap.containsKey(p.getUniqueId())) {
                updateRegion(p);
                return;
            }
            if (region != null) {
                createRegion(p, region);
            }
        } else if (e.getAction() == Action.LEFT_CLICK_BLOCK) {
            if (b == null) return;

            Chunk chunk = b.getChunk();
            if (!wildRegions.containsKey(chunk)) {
                p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Не прогружено"));
                loadWildChunks(getActiveChunks(chunk));
                return;
            }

            Region existingRegion;
            if (selectedRegionMap.containsKey(p.getUniqueId()) && (selectedRegionMap.get(p.getUniqueId()).contains(b.getX(), b.getZ())
                    || selectedRegionMap.get(p.getUniqueId()).getSelectedCorner() != 0)) {
                existingRegion = selectedRegionMap.get(p.getUniqueId());
            } else {
                existingRegion = getBlockRegion(b);
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

        if (selectedRegionMap.containsKey(p.getUniqueId())){
            return;
        }

        if (!regionMap.containsKey(p.getUniqueId())){
            regionMap.put(p.getUniqueId(),new Region(p, loc.getWorld().getName(), loc.getBlockX(), loc.getBlockZ()));
            p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacy(ChatColor.YELLOW + "Первая точка установлена."));
        } else {
            Region region = regionMap.get(p.getUniqueId());
            if (!doubleClick.containsKey(p.getUniqueId())){

                BukkitRunnable task = new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (selectedRegionMap.containsKey(p.getUniqueId())) return;
                        region.setFirstCorner((int)loc.getX(), (int)loc.getZ());
                        doubleClick.remove(p.getUniqueId());
                        p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacy(ChatColor.YELLOW + "Первая точка установлена."));
                        checkRegion(p);
                    }
                };

                int taskId = task.runTaskLater(WILDARK.getPlugin(), 20).getTaskId();
                doubleClick.put(p.getUniqueId(),taskId);
            } else {
                Bukkit.getScheduler().cancelTask(doubleClick.get(p.getUniqueId()));

                region.setSecondCorner((int)loc.getX(), (int)loc.getZ());
                doubleClick.remove(p.getUniqueId());
                p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacy(ChatColor.YELLOW + "Вторая точка установлена."));
                checkRegion(p);
            }
        }
    }

    public void checkRegion(Player p) {
        Region region = regionMap.get(p.getUniqueId());

        WildPlayer wildPlayer = getWildPlayer(p);
        if (region.areaSelected()) {
            ChatColor color = wildPlayer.getAvailableBlocks() >= region.getArea() ? ChatColor.GREEN : ChatColor.RED;
            p.sendMessage(ChatColor.WHITE + "[W] " + ChatColor.GRAY + "Участок: " + ChatColor.WHITE + region.getGrid() + ChatColor.GRAY + ". Необходимо блоков: " + color + region.getArea());

            if (wildPlayer.getAvailableBlocks() < region.getArea()) {
                p.sendMessage(ChatColor.WHITE + "[W] " + ChatColor.RED + "У вас недостаточно блоков.");
            } else {
                p.sendMessage(ChatColor.WHITE + "[W] " + ChatColor.GRAY + "ШИФТ + ПКМ чтобы создать");
            }
        }
    }

    public void checkNewRegion(Player p){
        Region oldRegion = selectedRegionMap.get(p.getUniqueId());
        Region newRegion = regionMap.get(p.getUniqueId());

        int price = getPrice(oldRegion, newRegion);

        WildPlayer wildPlayer = getWildPlayer(p);
        if (newRegion.areaSelected()) {
            ChatColor color = wildPlayer.getAvailableBlocks() >= price ? ChatColor.GREEN : ChatColor.RED;
            p.sendMessage(ChatColor.WHITE + "[W] " + ChatColor.GRAY + "Новый участок: " + ChatColor.WHITE + newRegion.getGrid() + ChatColor.GRAY
                    + (price > 0 ? ". Необходимо блоков: " + color + price : ". Будет возвращено блоков: " + color + (-price)));

            if (price > 0 && wildPlayer.getAvailableBlocks() < price) {
                p.sendMessage(ChatColor.WHITE + "[W] " + ChatColor.RED + "У вас недостаточно блоков.");
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
        WildPlayer wildPlayer = getWildPlayer(p);
        if (wildPlayer.getAvailableBlocks() < region.getArea()) {
            p.sendMessage(ChatColor.WHITE + "[W] " + ChatColor.RED + "У вас недостаточно блоков.");
            return;
        }

        if (wildPlayer.getAvailableRegions() < 1){
            p.sendMessage(ChatColor.WHITE + "[W] " + ChatColor.RED + "У вас максимальное число участков.");
            return;
        }

        db.regions.regionStatus(region).thenAccept(regionStatus -> {
            if (regionStatus.equals("intersect")){
                p.sendMessage(ChatColor.WHITE + "[W] " + ChatColor.RED + "Участок пересекается с другими.");
                return;
            } else if (regionStatus.equals("close")) {
                p.sendMessage(ChatColor.WHITE + "[W] " + ChatColor.RED + "Рядом участок больше вашего.");
                return;
            }

            db.regions.createRegion(region).thenAccept((isCreated) -> {

                Chunk chunk = p.getLocation().getChunk();
                List<Region> newRegions = new ArrayList<>(wildRegions.get(chunk));
                newRegions.add(region);
                wildRegions.put(chunk, newRegions);

                int price = region.getArea();

                db.players.updateAvailableRegions(p, -1);
                db.players.updateAvailableBlock(p,-price);
                p.sendMessage(ChatColor.WHITE + "[W] " + ChatColor.GREEN + "Вы создали регион.");
                p.sendMessage(ChatColor.WHITE + "[W] " + ChatColor.GRAY + "Потрачено блоков: "
                        + ChatColor.WHITE + (price) + ChatColor.GRAY + ".");
                clearRegionMap(p);
            });
        });
    }

    public void updateRegion(Player p) {
        Region oldRegion = selectedRegionMap.get(p.getUniqueId());
        Region newRegion = regionMap.get(p.getUniqueId());

        int price = getPrice(oldRegion, newRegion);

        WildPlayer wildPlayer = getWildPlayer(p);

        if (price > 0 && wildPlayer.getAvailableBlocks() < price) {
            p.sendMessage(ChatColor.WHITE + "[W] " + ChatColor.RED + "У вас недостаточно блоков.");
            return;
        }

        db.regions.regionStatus(newRegion).thenAccept(regionStatus -> Bukkit.getScheduler().runTask(WILDARK.getPlugin(), () -> {
            if (regionStatus.equals("intersect")){
                p.sendMessage(ChatColor.WHITE + "[W] " + ChatColor.RED + "Участок пересекается с другими.");
                return;
            } else if (regionStatus.equals("close")) {
                p.sendMessage(ChatColor.WHITE + "[W] " + ChatColor.RED + "Рядом участок больше вашего.");
                return;
            }

            db.regions.updateRegion(newRegion).thenAccept(updated -> Bukkit.getScheduler().runTask(WILDARK.getPlugin(), () -> {{
                if (!updated) {
                    p.sendMessage(ChatColor.WHITE + "[W] " + ChatColor.RED + "Не удалось обновить регион.");
                    return;
                }

                newRegion.getChunks().stream().forEach(chunk -> {
                    if (!wildRegions.containsKey(chunk)) return;

                    List<Region> updatedRegions = new ArrayList<>(wildRegions.get(chunk));
                    updatedRegions = updatedRegions.stream().filter(region -> region.getId() != oldRegion.getId()).collect(Collectors.toList());
                    updatedRegions.add(newRegion);
                    wildRegions.put(chunk, updatedRegions);
                });

                db.players.updateAvailableBlock(p,-price);

                p.sendMessage(ChatColor.WHITE + "[W] " + ChatColor.GREEN + "Вы обновили регион.");

                newRegion.removeSelectedCorner();

                if (price < 0){
                    p.sendMessage(ChatColor.WHITE + "[W] " + ChatColor.GRAY + "Возвращено блоков: "
                            + ChatColor.WHITE + (-price) + ChatColor.GRAY + ".");
                } else {
                    p.sendMessage(ChatColor.WHITE + "[W] " + ChatColor.GRAY + "Потрачено блоков: "
                            + ChatColor.WHITE + (price) + ChatColor.GRAY + ".");
                }

                clearExistingRegionMap(p);
                clearRegionMap(p);
            }}));
        }));
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
                Region newRegion = new Region(region.getId(), p, region.getName(), b.getWorld().getName(), oppositeX, oppositeZ, b.getX(), b.getZ());
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

        if (selectedRegionMap.containsKey(p.getUniqueId()) && selectedRegionMap.get(p.getUniqueId()).getName().equals(region.getName())){
            Region existing = selectedRegionMap.get(p.getUniqueId());
            boolean selected = existing.selectCorner(b.getX(), b.getZ());
            if (selected) p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacy(ChatColor.YELLOW + "Угол выбран."));
            return;
        }

        p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacy(ChatColor.YELLOW + "Участок выделен."));
        region.setColor("blue");
        selectedRegionMap.put(p.getUniqueId(), region);
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
        if (StickRegionListener.selectedRegionMap.containsKey(p.getUniqueId())) {
            StickRegionListener.selectedRegionMap.get(p.getUniqueId()).removeHolo();
            StickRegionListener.selectedRegionMap.remove(p.getUniqueId());
        }
    }
}
