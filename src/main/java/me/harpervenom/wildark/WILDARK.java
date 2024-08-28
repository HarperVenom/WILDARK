package me.harpervenom.wildark;

import me.harpervenom.wildark.database.Database;
import me.harpervenom.wildark.listeners.*;
import org.bukkit.plugin.java.JavaPlugin;

public final class WILDARK extends JavaPlugin {

    private Database db;
    static WILDARK plugin;

    public static WILDARK getPlugin() {
        return plugin;
    }

//    public DatabaseManager getDatabaseManager() {
//        return databaseManager;
//    }

    @Override
    public void onEnable() {
        // Plugin startup logic
        plugin = this;

        db = new Database();
        db.init();

        System.out.println("WILDDARK запущен.");

        getServer().getPluginManager().registerEvents(new GeneralListener(db), this);

        getServer().getPluginManager().registerEvents(new StickBlockListener(db), this);
        getServer().getPluginManager().registerEvents(new StickRegionListener(db), this);
        getServer().getPluginManager().registerEvents(new StickAreaListener(db), this);

        getServer().getPluginManager().registerEvents(new GeneralListener(db), this);
        getServer().getPluginManager().registerEvents(new BlockListener(db), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(db), this);

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        db.close();
    }
}
