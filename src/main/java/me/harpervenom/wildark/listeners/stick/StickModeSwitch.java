package me.harpervenom.wildark.listeners.stick;

import me.harpervenom.wildark.classes.RegionStick;
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

import static me.harpervenom.wildark.listeners.stick.StickRegionListener.*;

public class StickModeSwitch implements Listener {

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
            if (activeRegionMap.containsKey(p.getUniqueId())) {
                resetSelection(p);
            }
            p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacy(ChatColor.GRAY + "Режим: "
                    + ChatColor.WHITE + translateMode(stick.getMode())));
        }

        e.setCancelled(true);
    }

    public String translateMode(String mode) {
        return switch (mode) {
            case "Block" -> "Блок";
            case "Area" -> "Территория";
            case "Region" -> "Участок";
            default -> "";
        };
    }
}
