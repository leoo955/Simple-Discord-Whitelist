package fr.server.core.listeners;

import fr.server.core.CorePlugin;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Random;
import java.util.UUID;

/**
 * Listens for player connection events to enforce the Discord whitelist.
 */
public class JoinListener implements Listener {

    private final CorePlugin plugin;
    private final Random random = new Random();

    public JoinListener(CorePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Translates color codes and retrieves messages from the configuration.
     */
    private String getMessage(String path, String def) {
        String msg = plugin.getConfig().getString("messages." + path, def);
        return ChatColor.translateAlternateColorCodes('&', msg);
    }

    /**
     * Handles the pre-login phase to check if a player is allowed to join.
     * This event runs asynchronously.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        // Skip check if the whitelist is disabled in config
        if (!plugin.getConfig().getBoolean("settings.enabled", false)) return;

        UUID uuid = event.getUniqueId();
        String name = event.getName();
        
        // Ensure the player is in the local database
        plugin.getPlayerManager().ensurePlayerExists(uuid, name);

        // Check if the player has linked their Discord account
        if (!plugin.getPlayerManager().isLinked(uuid)) {
            // Generate a random 6-digit linking code
            String code = String.format("%03d %03d", random.nextInt(1000), random.nextInt(1000));
            plugin.getPlayerManager().addPendingCode(uuid, code);

            // Kick the player with the linking instructions
            String kickMessage = getMessage("kick-discord-link", "&cDiscord Link Required.\nCode: {code}")
                    .replace("{code}", code);
                    
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST, kickMessage);
            return;
        }

        // Check if the linked Discord user has the required "Whitelisted" role
        String discordId = plugin.getPlayerManager().getDiscordId(uuid);
        if (discordId == null || !plugin.getBotManager().hasRole(discordId)) {
            String roleName = plugin.getConfig().getString("bot.role-name", "Whitelisted");
            String kickMessage = ChatColor.translateAlternateColorCodes('&', 
                "&cVerification Failed.\n\n&7You must have the &b" + roleName + " &7role on our Discord server to join!");
            
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST, kickMessage);
            return;
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Reserved for future join logic
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Reserved for future quit logic
    }
}

