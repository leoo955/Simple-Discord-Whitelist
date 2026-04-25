package fr.server.core.commands;

import fr.server.core.CorePlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/**
 * This command lets admins turn the whitelist filter on or off from inside Minecraft.
 */
public class ToggleCommand implements CommandExecutor {
    private final CorePlugin plugin;

    public ToggleCommand(CorePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * This is where the magic happens when someone runs the /discordwhitelist command.
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check if the person has permission to do this.
        if (!sender.hasPermission("servercore.admin")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }

        // Switch the whitelist state: if it's on, turn it off, and vice versa.
        boolean currentState = plugin.getConfig().getBoolean("settings.enabled", false);
        boolean newState = !currentState;
        
        plugin.getConfig().set("settings.enabled", newState);
        plugin.saveConfig();

        String prefix = plugin.getConfig().getString("messages.prefix", "§9[DiscordWhitelist] §r");

        // Tell the user what we just did.
        if (newState) {
            sender.sendMessage(prefix + "§aThe Discord Whitelist filter has been ENABLED!");
        } else {
            sender.sendMessage(prefix + "§cThe Discord Whitelist filter has been DISABLED!");
        }
        
        return true;
    }
}
