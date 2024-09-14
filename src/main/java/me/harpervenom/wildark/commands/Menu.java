package me.harpervenom.wildark.commands;

import me.harpervenom.wildark.WILDARK;
import me.harpervenom.wildark.classes.Region;
import me.harpervenom.wildark.classes.WildPlayer;
import me.harpervenom.wildark.database.Database;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

import static me.harpervenom.wildark.listeners.PlayerListener.getWildPlayer;

public class Menu implements CommandExecutor, Listener {

    private final Database db;

    public Menu(Database db) {
        this.db = db;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player p) {

            WildPlayer wildplayer = getWildPlayer(p);

            if (wildplayer == null) {
                p.sendMessage( "Не удалось загрузить профиль.");
            }

            db.regions.getPlayerRegions(p).thenAccept(regions -> {
                int totalUsedBlocks = regions.stream().reduce(0, (total, region) -> total + region.getArea(), Integer::sum);

                Inventory menu = Bukkit.createInventory(null, 27, "Меню");

                ItemStack blocksItem = new ItemStack(Material.GRASS_BLOCK);
                ItemMeta blocksMeta = blocksItem.getItemMeta();
                if (blocksMeta != null) {
                    blocksMeta.setDisplayName(ChatColor.YELLOW + "Блоки");

                    List<String> lore = new ArrayList<>();
                    lore.add(ChatColor.GRAY + "Доступно: " + ChatColor.GREEN + wildplayer.getAvailableBlocks());
                    lore.add(ChatColor.GRAY + "Используется: " + ChatColor.WHITE + totalUsedBlocks);
                    blocksMeta.setLore(lore);

                    blocksItem.setItemMeta(blocksMeta);
                }

                menu.setItem(11, blocksItem);

                ItemStack regionsItem = new ItemStack(Material.MAP);
                ItemMeta regionsMeta = regionsItem.getItemMeta();
                if (regionsMeta != null) {
                    regionsMeta.setDisplayName(ChatColor.YELLOW + "Участки");

                    List<String> lore = new ArrayList<>();
                    lore.add(ChatColor.GRAY + "Доступно: " + ChatColor.GREEN + wildplayer.getAvailableRegions());
                    lore.add(ChatColor.GRAY + "Создано: " + ChatColor.WHITE + regions.size());
                    if (!regions.isEmpty()) {
                        lore.add(ChatColor.DARK_GRAY + "*Клик для просмотра*");
                    }

                    regionsMeta.setLore(lore);

                    regionsItem.setItemMeta(regionsMeta);
                }

                menu.setItem(15, regionsItem);
                p.sendMessage("here");
                Bukkit.getScheduler().runTask(WILDARK.getPlugin(), () -> p.openInventory(menu));
            });
            return true;
        } else {
            sender.sendMessage("This command can only be run by a player.");
            return false;
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (e.getView().getTitle().equals("Ваши Участки")){
            e.setCancelled(true);
            return;
        }

        if (e.getView().getTitle().equals("Меню")) {
            e.setCancelled(true);

            Player p = (Player) e.getWhoClicked();
            ItemStack clickedItem = e.getCurrentItem();
            if (clickedItem == null) return;
            if (clickedItem.hasItemMeta() && ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName()).equals("Участки")) {
                openRegionsTab(p);
            }
        }
    }

    public void openRegionsTab(Player p) {
        db.regions.getPlayerRegions(p).thenAccept(regions -> {
            Inventory regionsTab = Bukkit.createInventory(null, 27, "Ваши Участки");

            List<ItemStack> regionItems = new ArrayList<>();

            for (Region region : regions) {
                ItemStack item = new ItemStack(Material.FILLED_MAP);
                ItemMeta itemMeta = item.getItemMeta();

                itemMeta.setDisplayName(ChatColor.YELLOW + region.getName());
                itemMeta.addItemFlags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP);

                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GRAY + "Размер: " + ChatColor.WHITE + region.getWidth() + "x" + region.getLength());
                itemMeta.setLore(lore);

                item.setItemMeta(itemMeta);

                regionItems.add(item);
            }

            for (int i = 0; i < regionItems.size(); i++) {
                regionsTab.setItem(i,regionItems.get(i));
            }

            Bukkit.getScheduler().runTask(WILDARK.getPlugin(), () -> p.openInventory(regionsTab));
        });
    }
}
