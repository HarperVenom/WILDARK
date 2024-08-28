package me.harpervenom.wildark.listeners;

import me.harpervenom.wildark.WILDARK;
import me.harpervenom.wildark.classes.HoloBlock;
import me.harpervenom.wildark.classes.RegionStick;
import me.harpervenom.wildark.database.Database;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

public class StickBlockListener implements Listener {

    private Database db;

    public StickBlockListener(Database db) {
        this.db = db;
    }

    @EventHandler
    public void StickUseEvent(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        Block b = e.getClickedBlock();

        if (b == null) return;

        if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK
                || e.getAction() == Action.LEFT_CLICK_AIR) return;

        if (e.getHand() == EquipmentSlot.OFF_HAND) return;
        ItemStack tool = e.getItem();
        if (tool == null || tool.getType() != Material.STICK) return;

        if (p.getGameMode() == GameMode.CREATIVE) {
            e.setCancelled(true);
        }

        if (!GeneralListener.regionStickMap.containsKey(p.getUniqueId())) return;
        RegionStick stick = GeneralListener.regionStickMap.get(p.getUniqueId());
        if (!stick.getMode().equals("Block")) return;

        String blockOwnerID = db.blocks.getOwner(b);
        showInfo(blockOwnerID, p);

        new HoloBlock(p,b.getLocation());
    }

    public void showInfo(String blockOwnerID, Player p) {
        if (blockOwnerID == null) {
            p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacy(ChatColor.GRAY + "Не защищено."));
        } else {
            ChatColor color = blockOwnerID.equals(p.getUniqueId().toString()) ? ChatColor.GREEN : ChatColor.RED;
            p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacy(color + "Защищено."));
            p.playSound(p.getLocation(), Sound.ENTITY_BLAZE_HURT,0.2f,1f);
        }
    }
}
