package fr.server.core.commands;

import fr.server.core.CorePlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/**
 * Command executor for /discordwhitelist.
 * Allows administrators to toggle the whitelist filter on or off in-game.
 */
public class ToggleCommand implements CommandExecutor {
    private final CorePlugin plugin;

    public ToggleCommand(CorePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Executes the /discordwhitelist command.
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Enforce administrative permission
        if (!sender.hasPermission("servercore.admin")) {
            sender.sendMessage("§cYou do not have permission to use this command.");
            return true;
        }

        // Toggle the current enabled state in the configuration
        boolean currentState = plugin.getConfig().getBoolean("settings.enabled", false);
        boolean newState = !currentState;
        
        plugin.getConfig().set("settings.enabled", newState);
        plugin.saveConfig();

        String prefix = plugin.getConfig().getString("messages.prefix", "§9[DiscordWhitelist] §r");

        // Notify the sender of the new state
        if (newState) {
            sender.sendMessage(prefix + "§aThe Discord Whitelist filter has been ENABLED!");
        } else {
            sender.sendMessage(prefix + "§cThe Discord Whitelist filter has been DISABLED!");
        }
        
        return true;
    }
}
