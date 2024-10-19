package me.harpervenom.wildark.commands;

import me.harpervenom.wildark.WILDARK;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class DayDuration implements CommandExecutor {

    private static BukkitTask timeTask;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player p) {
            if (!p.isOp()) return false;
        }

        if (args.length != 1) return false;

        try {
            int seconds = Integer.parseInt(args[0]);

            sender.sendMessage("Setting day duration to " + seconds + " seconds.");
            setDayDurationInSeconds(seconds);

        } catch (NumberFormatException e) {
            sender.sendMessage("Error: The argument must be a valid integer.");
            return false;
        }

        return true;
    }

    public static void setDayDurationInSeconds(int seconds) {
        World world = WILDARK.getPlugin().getServer().getWorld("world");
        if (world == null) return;

        int ticks = seconds * 20;
        int period = 1;
        double jump = 24000.0/ticks;
        if (jump < 1) {
            period *= (int) Math.round(1/jump);
            jump = 1;
        }

        WILDARK.getPlugin().getConfig().set("day-duration-seconds", seconds);
        WILDARK.getPlugin().saveConfig();

        if (timeTask != null && !timeTask.isCancelled()) {
            timeTask.cancel();
        }

        double finalJump = jump;
        timeTask = new BukkitRunnable() {
            @Override
            public void run() {
                long currentTime = world.getTime();

                world.setTime(currentTime + (int) finalJump);

            }
        }.runTaskTimer(WILDARK.getPlugin(), 0, period);
    }
}
