package fr.server.core;

import fr.server.core.listeners.JoinListener;
import fr.server.core.discord.BotManager;
import fr.server.core.managers.PlayerManager;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main entry point for the DiscordWhitelist plugin.
 * Handles initialization of managers, commands, and listeners.
 */
public class CorePlugin extends JavaPlugin {

    private PlayerManager playerManager;
    private BotManager botManager;

    @Override
    public void onEnable() {
        // Initialize configuration and data files
        saveDefaultConfig();
        
        // Initialize core management components
        this.playerManager = new PlayerManager(this);
        this.botManager = new BotManager(this);
        
        // Register administrative Minecraft commands
        if (getCommand("discordwhitelist") != null) {
            getCommand("discordwhitelist").setExecutor(new fr.server.core.commands.ToggleCommand(this));
        }
        
        // Register connection events
        getServer().getPluginManager().registerEvents(new JoinListener(this), this);
        
        // Log plugin status to console
        getLogger().info("DiscordWhitelist has been enabled successfully.");
    }

    @Override
    public void onDisable() {
        // Ensure the JDA instance is properly shut down
        if (botManager != null) {
            botManager.stopBot();
        }
        getLogger().info("DiscordWhitelist has been disabled.");
    }

    /**
     * @return The manager handling player data and linking states.
     */
    public PlayerManager getPlayerManager() {
        return playerManager;
    }

    /**
     * @return The manager handling the Discord bot instance and roles.
     */
    public BotManager getBotManager() {
        return botManager;
    }
}

