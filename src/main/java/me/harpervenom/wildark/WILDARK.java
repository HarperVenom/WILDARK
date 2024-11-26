package me.harpervenom.wildark;

import me.harpervenom.wildark.commands.DayDuration;
import me.harpervenom.wildark.commands.Menu;
import me.harpervenom.wildark.database.Database;
import me.harpervenom.wildark.keys.classes.listeners.KeyListener;
import me.harpervenom.wildark.keys.classes.listeners.LockListener;
import me.harpervenom.wildark.listeners.*;
import me.harpervenom.wildark.listeners.stick.StickModeSwitch;
import me.harpervenom.wildark.listeners.stick.StickAreaListener;
import me.harpervenom.wildark.listeners.stick.StickBlockListener;
import me.harpervenom.wildark.listeners.stick.StickRegionListener;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

import static me.harpervenom.wildark.commands.DayDuration.setDayDurationInSeconds;

public final class WILDARK extends JavaPlugin {

    public static Database db;
    static WILDARK plugin;
    private static FileConfiguration languageConfig;

    public static WILDARK getPlugin() {
        return plugin;
    }

    @Override
    public void onEnable() {
        // Plugin startup logic
        plugin = this;

        db = new Database();
        db.init();

        saveDefaultConfig();
        loadLanguageFile("ru_RU");

        getServer().getPluginManager().registerEvents(new StickModeSwitch(), this);

        getServer().getPluginManager().registerEvents(new StickBlockListener(), this);
        getServer().getPluginManager().registerEvents(new StickRegionListener(), this);
        getServer().getPluginManager().registerEvents(new StickAreaListener(), this);

        getServer().getPluginManager().registerEvents(new BlockListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(), this);
        getServer().getPluginManager().registerEvents(new WildChunksListener(), this);

        getServer().getPluginManager().registerEvents(new LockListener(), this);
        getServer().getPluginManager().registerEvents(new KeyListener(), this);

        getCommand("m").setExecutor(new Menu());
        getCommand("setdayduration").setExecutor(new DayDuration());

        getServer().getPluginManager().registerEvents(new Menu(), this);

        System.out.println("[WILDARK] Плагин запущен.");

        int savedDuration = getConfig().getInt("day-duration-seconds", -1);  // Default to -1 if not set

        if (savedDuration != -1) {
            DayDuration.setDayDurationInSeconds(savedDuration);  // Reapply the saved day duration
            System.out.println("[WILDARK] Длительность дня: " + savedDuration + " секунд.");
        }
    }

    private void loadLanguageFile(String lang) {
        File langFile = new File(getDataFolder(), lang + ".yml");

        if (!langFile.exists()) {
            langFile.getParentFile().mkdirs();
            saveResource(lang + ".yml", false);  // Copy default language file from resources
        }

        languageConfig = YamlConfiguration.loadConfiguration(langFile);
    }

    // Getter for the language configuration
    public static String getMessage(String path) {
        return languageConfig.getString(path);
    }

    @Override
    public void onDisable() {
        db.close();
    }
}
