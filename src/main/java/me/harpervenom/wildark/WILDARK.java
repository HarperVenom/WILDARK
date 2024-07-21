package me.harpervenom.wildark;

import dev.geco.gsit.api.GSitAPI;
import net.coreprotect.CoreProtect;
import net.coreprotect.CoreProtectAPI;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public final class WILDARK extends JavaPlugin {

    @Override
    public void onEnable() {
        // Plugin startup logic
        System.out.println("Здарова");
        CoreProtectAPI coreProtect = getCoreProtect();
        if (coreProtect != null) {
            coreProtect.testAPI();
        }
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    private CoreProtectAPI getCoreProtect() {
        Plugin plugin = getServer().getPluginManager().getPlugin("CoreProtect");

        // Check that CoreProtect is loaded
        if (!(plugin instanceof CoreProtect)) {
            return null;
        }

        // Check that the API is enabled
        CoreProtectAPI CoreProtect = ((CoreProtect) plugin).getAPI();
        if (!CoreProtect.isEnabled()) {
            return null;
        }

        // Check that a compatible version of the API is loaded
        if (CoreProtect.APIVersion() < 10) {
            return null;
        }

        return CoreProtect;
    }
}
