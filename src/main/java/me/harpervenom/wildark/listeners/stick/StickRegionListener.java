package me.harpervenom.wildark.listeners.stick;

import me.harpervenom.wildark.WILDARK;
import me.harpervenom.wildark.classes.Region;
import me.harpervenom.wildark.classes.RegionStick;
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

import static me.harpervenom.wildark.classes.Region.getPlayerRegion;
import static me.harpervenom.wildark.listeners.WildChunksListener.*;

public class StickRegionListener implements Listener {

    public static HashMap<UUID, Region> activeRegionMap = new HashMap<>();
    HashMap<UUID, Integer> doubleClick = new HashMap<>();

    @EventHandler
    public void InteractEvent(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        Region activeRegion = activeRegionMap.get(p.getUniqueId());
        Block b = e.getClickedBlock();

        if (e.getHand() == EquipmentSlot.OFF_HAND) return;
        ItemStack tool = e.getItem();
        if (tool == null || tool.getType() != Material.STICK) return;

        if (!StickModeSwitch.regionStickMap.containsKey(p.getUniqueId())) return;
        RegionStick stick = StickModeSwitch.regionStickMap.get(p.getUniqueId());
        if (!stick.getMode().equals("Region")) return;

        if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (!p.isSneaking()) return;
            if (activeRegion == null) return;

            if (!activeRegion.exists()) {
                activeRegion.create();
            } else {
                if (activeRegion.updatedRegion != null) activeRegion.update();
            }
        } else if (e.getAction() == Action.LEFT_CLICK_BLOCK) {
            if (b == null) return;

            if (!regionsLoaded) {
                p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Не прогружено"));
                return;
            }

            Region region = getPlayerRegion(p, b);

            if (region != null || (activeRegion != null && activeRegion.getSelectedCorner() != 0)) {

                if (activeRegion != null) {
                    if (region != null && region.getId() != activeRegion.getId()) {
                        activeRegion.reset();
                    } else {

                        if (region != null && region.getSelectedCorner() == 0) {
                            region.selectCorner(b);
                            return;
                        }

                        if (!doubleClick.containsKey(p.getUniqueId())){
                            Region finalRegion = region;
                            BukkitRunnable task = new BukkitRunnable() {
                                @Override
                                public void run() {
                                    if (finalRegion == null) return;
                                    finalRegion.selectCorner(b);
                                    doubleClick.remove(p.getUniqueId());
                                }
                            };

                            int taskId = task.runTaskLater(WILDARK.getPlugin(), 20).getTaskId();
                            doubleClick.put(p.getUniqueId(),taskId);
                        } else {
                            Bukkit.getScheduler().cancelTask(doubleClick.get(p.getUniqueId()));

                            activeRegion.selectNew(b);

                            doubleClick.remove(p.getUniqueId());
                        }

                        return;
                    }
                }

                region.select();
                activeRegionMap.put(p.getUniqueId(), region);
            } else {
                if (activeRegion == null || activeRegion.exists()) {
                    if (activeRegion != null) activeRegion.reset();

                    region = new Region(p, b.getWorld().getName(), b.getX(), b.getZ());
                    activeRegionMap.put(p.getUniqueId(), region);
                } else {
                    region = activeRegion;

                    if (!doubleClick.containsKey(p.getUniqueId())){
                        Region finalRegion = region;
                        BukkitRunnable task = new BukkitRunnable() {
                            @Override
                            public void run() {
                                if (finalRegion.updatedRegion != null) return;

                                finalRegion.setFirstCorner(b.getX(), b.getZ());

                                doubleClick.remove(p.getUniqueId());
                            }
                        };

                        int taskId = task.runTaskLater(WILDARK.getPlugin(), 20).getTaskId();
                        doubleClick.put(p.getUniqueId(),taskId);
                    } else {
                        Bukkit.getScheduler().cancelTask(doubleClick.get(p.getUniqueId()));

                        region.setSecondCorner(b.getX(), b.getZ());

                        doubleClick.remove(p.getUniqueId());
                    }
                }
            }
        }
    }

    @EventHandler
    public void PortalEnter(PlayerChangedWorldEvent e){
        Player p = e.getPlayer();
        resetSelection(p);
    }

    @EventHandler
    public void PlayerLeave(PlayerQuitEvent e){
        Player p = e.getPlayer();
        resetSelection(p);
    }

    public static void resetSelection(Player p){
        if (activeRegionMap.containsKey(p.getUniqueId())) {
            activeRegionMap.get(p.getUniqueId()).reset();
            activeRegionMap.remove(p.getUniqueId());
        }
    }
}
