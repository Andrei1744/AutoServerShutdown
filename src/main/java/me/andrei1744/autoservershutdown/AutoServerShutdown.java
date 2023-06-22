package me.andrei1744.autoservershutdown;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;

public class AutoServerShutdown extends JavaPlugin {
    private int shutdownHour = 23; // Default shutdown hour
    private int shutdownMinute = 0; // Default shutdown minute
    private FileConfiguration config;


    @Override
    public void onEnable() {
        // Create or load the configuration file
        createConfig();

        // Retrieve the shutdown time from the configuration file
        loadShutdownTime();

        // Register the command
        getCommand("shutdown").setExecutor(this);

        // Schedule the initial server shutdown task
        scheduleShutdownTask();
    }


    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("shutdown")) {
            if (args.length == 2) {
                try {
                    int newShutdownHour = Integer.parseInt(args[0]);
                    int newShutdownMinute = Integer.parseInt(args[1]);

                    if (isValidHour(newShutdownHour) && isValidMinute(newShutdownMinute)) {
                        shutdownHour = newShutdownHour;
                        shutdownMinute = newShutdownMinute;

                        // Save the new shutdown time to the configuration file
                        config.set("shutdown.hour", shutdownHour);
                        config.set("shutdown.minute", shutdownMinute);
                        saveConfig();

                        // Cancel the previous shutdown task and schedule a new one
                        Bukkit.getScheduler().cancelTasks(this);
                        scheduleShutdownTask();

                        sender.sendMessage("Server shutdown time has been changed to " + newShutdownHour + ":" + newShutdownMinute + ".");
                    } else {
                        sender.sendMessage("Invalid hour or minute. Please enter valid values (hour: 0-23, minute: 0-59).");
                    }
                } catch (NumberFormatException e) {
                    sender.sendMessage("Invalid hour or minute format. Please enter valid integer values (hour: 0-23, minute: 0-59).");
                }
            } else if (args.length == 1 && args[0].equalsIgnoreCase("time")) {
                // Display the remaining time until shutdown
                int remainingSeconds = getRemainingSecondsUntilShutdown();

                int minutes = remainingSeconds / 60;
                int seconds = remainingSeconds % 60;

                sender.sendMessage("Time remaining until shutdown: " + minutes + " minutes, " + seconds + " seconds.");
            } else {
                sender.sendMessage("Usage: /shutdown <hour> <minute> OR /shutdown time");
            }

            return true;
        }
        return false;
    }

    private void createConfig() {
        // Create the plugin folder if it doesn't exist
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        // Create or load the configuration file
        File configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            saveResource("config.yml", false);
        }

        config = YamlConfiguration.loadConfiguration(configFile);
        config.options().copyDefaults(true);
        saveConfig();
    }

    private void loadShutdownTime() {
        shutdownHour = config.getInt("shutdown.hour", shutdownHour);
        shutdownMinute = config.getInt("shutdown.minute", shutdownMinute);
    }

    private void scheduleShutdownTask() {
        // Get the current time
        Calendar calendar = Calendar.getInstance();
        int currentHour = calendar.get(Calendar.HOUR_OF_DAY);
        int currentMinute = calendar.get(Calendar.MINUTE);
        int currentSecond = calendar.get(Calendar.SECOND);

        // Calculate the number of seconds until the desired time
        int secondsUntilShutdown;
        if (shutdownHour > currentHour || (shutdownHour == currentHour && shutdownMinute > currentMinute)) {
            int remainingHours = shutdownHour - currentHour;
            int remainingMinutes = shutdownMinute - currentMinute;
            int remainingSeconds = 60 - currentSecond;
            secondsUntilShutdown = (remainingHours * 3600 + remainingMinutes * 60 + remainingSeconds);
        } else {
            int remainingHours = 24 - currentHour + shutdownHour;
            int remainingMinutes = shutdownMinute - currentMinute;
            int remainingSeconds = 60 - currentSecond;
            secondsUntilShutdown = (remainingHours * 3600 + remainingMinutes * 60 + remainingSeconds);
        }

        // Schedule the warning tasks and the server shutdown task
        Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> sendWarningMessage("warnings.15_minutes"), (secondsUntilShutdown - (15 * 60)) * 20);
        Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> sendWarningMessage("warnings.10_minutes"), (secondsUntilShutdown - (10 * 60)) * 20);
        Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> sendWarningMessage("warnings.5_minutes"), (secondsUntilShutdown - (5 * 60)) * 20);
        Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> sendWarningMessage("warnings.5_seconds"), (secondsUntilShutdown - 5) * 20);
        Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> sendWarningMessage("warnings.4_seconds"), (secondsUntilShutdown - 4) * 20);
        Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> sendWarningMessage("warnings.3_seconds"), (secondsUntilShutdown - 3) * 20);
        Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> sendWarningMessage("warnings.2_seconds"), (secondsUntilShutdown - 2) * 20);
        Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> sendWarningMessage("warnings.1_second"), (secondsUntilShutdown - 1) * 20);
        Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> Bukkit.shutdown(), secondsUntilShutdown * 20);
    }

    private boolean isValidHour(int hour) {
        return hour >= 0 && hour <= 23;
    }

    private boolean isValidMinute(int minute) {
        return minute >= 0 && minute <= 59;
    }

    private void sendWarningMessage(String path) {
        String message = config.getString(path);
        if (message != null) {
            Bukkit.broadcastMessage(message);
        }
    }

    private int getRemainingSecondsUntilShutdown() {
        // Get the current time
        Calendar calendar = Calendar.getInstance();
        int currentHour = calendar.get(Calendar.HOUR_OF_DAY);
        int currentMinute = calendar.get(Calendar.MINUTE);
        int currentSecond = calendar.get(Calendar.SECOND);

        // Calculate the number of seconds until the desired time
        int secondsUntilShutdown;
        if (shutdownHour > currentHour || (shutdownHour == currentHour && shutdownMinute > currentMinute)) {
            int remainingHours = shutdownHour - currentHour;
            int remainingMinutes = shutdownMinute - currentMinute;
            int remainingSeconds = 60 - currentSecond;
            secondsUntilShutdown = (remainingHours * 3600 + remainingMinutes * 60 + remainingSeconds);
        } else {
            int remainingHours = 24 - currentHour + shutdownHour;
            int remainingMinutes = shutdownMinute - currentMinute;
            int remainingSeconds = 60 - currentSecond;
            secondsUntilShutdown = (remainingHours * 3600 + remainingMinutes * 60 + remainingSeconds);
        }

        return secondsUntilShutdown;
    }
}
