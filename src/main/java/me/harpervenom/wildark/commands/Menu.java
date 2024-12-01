package me.harpervenom.wildark.commands;

import me.harpervenom.wildark.WILDARK;
import me.harpervenom.wildark.classes.Region;
import me.harpervenom.wildark.classes.Relation;
import me.harpervenom.wildark.classes.WildPlayer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static me.harpervenom.wildark.WILDARK.db;
import static me.harpervenom.wildark.WILDARK.getPlugin;
import static me.harpervenom.wildark.listeners.PlayerListener.getWildPlayer;

public class Menu implements CommandExecutor, Listener {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player p) {

            WildPlayer wildplayer = getWildPlayer(p);

            if (wildplayer == null) {
                p.sendMessage("Не удалось загрузить профиль.");
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
                Bukkit.getScheduler().runTask(WILDARK.getPlugin(), () -> p.openInventory(menu));
            });
            return true;
        } else {
            sender.sendMessage("This command can only be run by a player.");
            return false;
        }
    }

    HashMap<UUID, List<Inventory>> previousMenu = new HashMap<>();

    @EventHandler
    public void MenuClick(InventoryClickEvent e) {
        if (e.getView().getTitle().equals("Меню")) {
            e.setCancelled(true);

            Player p = (Player) e.getWhoClicked();
            ItemStack clickedItem = e.getCurrentItem();
            if (clickedItem == null) return;
            if (clickedItem.hasItemMeta() && ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName()).equals("Участки")) {
                previousMenu.computeIfAbsent(p.getUniqueId(), k -> new ArrayList<>()).add(e.getClickedInventory());
                openRegionsTab(p);
            }
        }
    }

    @EventHandler
    public void RegionsMenuClick(InventoryClickEvent e) {
        if (!e.getView().getTitle().equals("Ваши Участки")) return;
        e.setCancelled(true);

        Player p = (Player) e.getWhoClicked();
        ItemStack clickedItem = e.getCurrentItem();
        if (clickedItem == null) return;

        WildPlayer wp = getWildPlayer(p);
        if (wp == null) return;
        List<Region> regions = wp.getRegions();

        for (int i = 0; i < regions.size(); i++) {
            if (e.getSlot() == i) {
                previousMenu.computeIfAbsent(p.getUniqueId(), k -> new ArrayList<>()).add(e.getClickedInventory());
                openRegionTab(p, regions.get(i));
                return;
            }
        }
    }

    @EventHandler
    public void RegionMenuClick(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();

        if (!openedRegionTabName.containsKey(p.getUniqueId())) return;
        if (!e.getView().getTitle().equals(openedRegionTabName.get(p.getUniqueId()))) return;
        e.setCancelled(true);
    }

    @EventHandler
    public void InventoryClose(InventoryCloseEvent e) {
        Player p = (Player) e.getPlayer();

        Bukkit.getScheduler().runTaskLater(getPlugin(), () -> {
            if (p.getOpenInventory().getType() == InventoryType.CRAFTING) {
                if (!previousMenu.containsKey(p.getUniqueId())) return;
                openLastMenu(p);
            }
        }, 1);
    }

    public void openRegionsTab(Player p) {
        WildPlayer wp = getWildPlayer(p);
        if (wp == null) return;
        List<Region> regions = wp.getRegions();

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
            regionsTab.setItem(i, regionItems.get(i));
        }

        p.openInventory(regionsTab);
    }

    HashMap<UUID, String> openedRegionTabName = new HashMap<>();

    public void openRegionTab(Player p, Region region) {
        String name = "Участок: " + region.getName();

        Inventory regionTab = Bukkit.createInventory(null, 27, name);

        List<ItemStack> relationItems = new ArrayList<>();

        for (Relation relation : region.getRelations()) {
            ItemStack relationItem = new ItemStack(Material.PLAYER_HEAD);
            ItemMeta meta = relationItem.getItemMeta();

            OfflinePlayer relative = Bukkit.getOfflinePlayer(UUID.fromString(relation.playerId()));

            meta.setDisplayName(ChatColor.WHITE + relative.getName());

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Связь: " + relation.relation());

            meta.setLore(lore);

            relationItem.setItemMeta(meta);

            relationItems.add(relationItem);
        }

        for (int i = 0; i < relationItems.size(); i++) {
            regionTab.setItem(i, relationItems.get(i));
        }

        openedRegionTabName.put(p.getUniqueId(), name);

        p.openInventory(regionTab);
    }

    public void openLastMenu(Player p) {
        List<Inventory> menus = previousMenu.get(p.getUniqueId());
        if (menus != null && !menus.isEmpty()) {
            Inventory lastMenu = menus.remove(menus.size() - 1);
            p.openInventory(lastMenu);
        }
    }
}
