package fr.server.core;

import fr.server.core.listeners.JoinListener;
import fr.server.core.discord.BotManager;
import fr.server.core.managers.PlayerManager;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * This is the heart of our plugin.
 * We've designed it to be super secure by keeping our internal managers private.
 * This way, no other plugin can mess with our Discord connection or player data.
 */
public class CorePlugin extends JavaPlugin {

    private BotManager botManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        
        // We set up our managers here, but we keep them hidden from the outside world for safety.
        PlayerManager playerManager = new PlayerManager(this);
        
        // The bot needs to know about players, so we give it the manager directly.
        this.botManager = new BotManager(this, playerManager);
        
        if (getCommand("discordwhitelist") != null) {
            getCommand("discordwhitelist").setExecutor(new fr.server.core.commands.ToggleCommand(this));
        }
        
        // We tell our listener exactly what it needs to work, nothing more.
        getServer().getPluginManager().registerEvents(new JoinListener(this, playerManager, botManager), this);
        
        getLogger().info("The plugin is up and running! Everything is locked down and safe.");
    }

    @Override
    public void onDisable() {
        if (botManager != null) {
            botManager.stopBot();
            botManager = null; // Clean up so we don't leak memory.
        }
        getLogger().info("System safely shut down.");
    }
    
    // We've intentionally left out any 'getters' for the managers to keep things secure.
}
