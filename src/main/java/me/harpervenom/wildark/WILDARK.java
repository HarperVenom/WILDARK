package me.harpervenom.wildark;

import me.harpervenom.wildark.commands.*;
import me.harpervenom.wildark.database.Database;
import me.harpervenom.wildark.keys.classes.listeners.KeyListener;
import me.harpervenom.wildark.keys.classes.listeners.LockListener;
import me.harpervenom.wildark.listeners.*;
import me.harpervenom.wildark.listeners.stick.StickModeSwitch;
import me.harpervenom.wildark.listeners.stick.StickAreaListener;
import me.harpervenom.wildark.listeners.stick.StickBlockListener;
import me.harpervenom.wildark.listeners.stick.StickRegionListener;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.GameRule;
import org.bukkit.World;
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

        getServer().getPluginManager().registerEvents(new NicknameListener(), this);

        getCommand("m").setExecutor(new Menu());
        getCommand("setdayduration").setExecutor(new DayDuration());
        getCommand("grant").setExecutor(new Grant());
        getCommand("help").setExecutor(new Help());
        getCommand("delete").setExecutor(new DeleteRegion());

        getCommand("mute").setExecutor(new Mute());
        getCommand("ban").setExecutor(new Ban());

        getServer().getPluginManager().registerEvents(new Menu(), this);

        getPlugin().getLogger().info("Плагин запущен.");

        int savedDuration = getConfig().getInt("day-duration-seconds", -1);  // Default to -1 if not set

        if (savedDuration != -1) {
            DayDuration.setDayDurationInSeconds(savedDuration);  // Reapply the saved day duration
            getPlugin().getLogger().info("Длительность дня: " + savedDuration + " секунд.");
        }

        getServer().setSpawnRadius(0);
        getPlugin().getLogger().info("Радиус спавна: 0.");

        for (World world : Bukkit.getWorlds()) {
            world.setDifficulty(Difficulty.HARD);
            world.getWorldBorder().setSize(4000);
            world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
            world.setGameRule(GameRule.REDUCED_DEBUG_INFO, true);
            world.setGameRule(GameRule.PLAYERS_SLEEPING_PERCENTAGE, 101);
        }
        getPlugin().getLogger().info("Диаметр мира: 4000.");
        getPlugin().getLogger().info("Сложность: Сложная.");
        getPlugin().getLogger().info("Координаты скрыты.");
        getPlugin().getLogger().info("Пропустить ночь невозможно.");
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
