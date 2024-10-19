package me.harpervenom.wildark.listeners.stick;

import me.harpervenom.wildark.classes.Region;
import me.harpervenom.wildark.classes.RegionStick;
import me.harpervenom.wildark.classes.WildBlock;
import me.harpervenom.wildark.listeners.BlockListener;
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

import static me.harpervenom.wildark.Materials.getMaxBlockHealth;
import static me.harpervenom.wildark.listeners.BlockListener.damagedBlocks;
import static me.harpervenom.wildark.listeners.BlockListener.getMainBlock;
import static me.harpervenom.wildark.listeners.WildChunksListener.*;

public class StickBlockListener implements Listener {

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

        e.setCancelled(true);

        if (!StickModeSwitch.regionStickMap.containsKey(p.getUniqueId())) return;
        RegionStick stick = StickModeSwitch.regionStickMap.get(p.getUniqueId());
        if (!stick.getMode().equals("Block")) return;

        showInfo(b, p);
    }

    public void showInfo(Block b, Player p) {
        b = getMainBlock(b);
        Chunk chunk = b.getChunk();

        if (chunkNotLoaded(p, chunk)) return;

        Region region = BlockListener.getBlockRegion(b);
        WildBlock wildBlock = BlockListener.getWildBlock(b);

        if (wildBlock == null) {
            p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.GRAY + "Не защищено"));
            return;
        }
//        p.sendMessage(ChatColor.YELLOW + "id: " + wildBlock.getId());

        if (region == null){
            p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.GRAY + "Не защищено"));
            return;
        }

        int maxBlockHealth = getMaxBlockHealth(b);
        int health = maxBlockHealth;
        if (damagedBlocks.containsKey(b)) {
            health = damagedBlocks.get(b);
        }

        ChatColor color = wildBlock.getOwnerId().equals(p.getUniqueId().toString()) ? ChatColor.GREEN : ChatColor.RED;
        p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacy(color + "Защищено" + " " + health + "/" + maxBlockHealth));
    }
}
