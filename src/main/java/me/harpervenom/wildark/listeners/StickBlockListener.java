package me.harpervenom.wildark.listeners;

import me.harpervenom.wildark.classes.Region;
import me.harpervenom.wildark.classes.RegionStick;
import me.harpervenom.wildark.classes.WildBlock;
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

import java.util.List;

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

        showInfo(b, p);
    }

    public void showInfo(Block b, Player p) {
        Chunk chunk = b.getChunk();

        if (!BlockListener.wildBlocks.containsKey(chunk) || !BlockListener.wildRegions.containsKey(chunk)){
            p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Не прогружено"));
            BlockListener.loadWildChunks(chunk);
        } else {
            Region region = BlockListener.getBlockRegion(b);
            WildBlock wildBlock = BlockListener.getWildBlock(b);

            if (wildBlock == null) {
                p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.GRAY + "Не защищено"));
                return;
            }
            if (region == null){
                p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.GRAY + "Не защищено"));
                return;
            }

            ChatColor color = wildBlock.getOwnerId().equals(p.getUniqueId().toString()) ? ChatColor.GREEN : ChatColor.RED;
            p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacy(color + "Защищено"));
        }

//        Chunk chunk = b.getChunk();
//
//        WildBlock wildBlock = BlockListener.getWildBlock(b);
//
//        if (BlockListener.wildBlocks.containsKey(chunk)) {
//            List<WildBlock> wildBlocks = BlockListener.wildBlocks.get(chunk);
//
//            if (wildBlocks.isEmpty()) {
//                p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacy(ChatColor.GRAY + "Не защищено"));
//                return;
//            }
//
//            for (WildBlock wildBlock : wildBlocks) {
//                if (!wildBlock.getLoc().equals(b.getLocation())) continue;
//
//                String ownerId = wildBlock.getOwnerId();
//
//                if (ownerId == null) {
//                    p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacy(ChatColor.GRAY + "Не защищено"));
//                } else {
//                    ChatColor color = ownerId.equals(p.getUniqueId().toString()) ? ChatColor.GREEN : ChatColor.RED;
//                    p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacy(color + "Защищено"));
//                }
//                return;
//            }
//            p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacy(ChatColor.GRAY + "Не защищено"));
//        } else {
//            p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Не прогружено"));
//            BlockListener.loadWildChunks(chunk);
//        }
    }
}
