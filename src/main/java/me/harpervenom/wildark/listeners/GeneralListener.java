package me.harpervenom.wildark.listeners;

import me.harpervenom.wildark.classes.RegionStick;
import me.harpervenom.wildark.classes.WildPlayer;
import me.harpervenom.wildark.database.Database;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.UUID;

public class GeneralListener implements Listener {

    private final Database db;

    public GeneralListener(Database db) {
        this.db = db;
    }

    public static HashMap<UUID, RegionStick> regionStickMap = new HashMap<>();

    @EventHandler
    public void SwitchStickModeEvent(PlayerInteractEvent e) {
        Player p = e.getPlayer();

        if (p.isSneaking()) return;
        if (e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.LEFT_CLICK_BLOCK) return;
        if (e.getHand() == EquipmentSlot.OFF_HAND) return;
        ItemStack tool = e.getItem();
        if (tool == null || tool.getType() != Material.STICK) return;


        if (!regionStickMap.containsKey(p.getUniqueId())) {
            regionStickMap.put(p.getUniqueId(), new RegionStick());
        } else {
            RegionStick stick = regionStickMap.get(p.getUniqueId());
            stick.switchMode();
            if (StickRegionListener.regionMap.containsKey(p.getUniqueId())) {
                StickRegionListener.regionMap.get(p.getUniqueId()).removeHolo();
                StickRegionListener.regionMap.remove(p.getUniqueId());
            }
            if (StickRegionListener.selectedRegionMap.containsKey(p.getUniqueId())) {
                StickRegionListener.selectedRegionMap.get(p.getUniqueId()).removeHolo();
                StickRegionListener.selectedRegionMap.remove(p.getUniqueId());
            }
            p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacy(ChatColor.GRAY + "Режим: "
                    + ChatColor.WHITE + translateMode(stick.getMode())));
        }
    }

    public String translateMode(String mode) {
        switch (mode) {
            case "Block" : return "Блок";
            case "Area" : return "Территория";
            case "Region" : return "Участок";
        }
        return "";
    }
}
