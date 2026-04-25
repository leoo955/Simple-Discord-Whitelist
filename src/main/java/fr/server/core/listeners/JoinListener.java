package fr.server.core.listeners;

import fr.server.core.CorePlugin;
import fr.server.core.discord.BotManager;
import fr.server.core.managers.PlayerManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

/**
 * This listener handles players trying to join the server.
 * It checks everything in the background so it doesn't slow down the main server.
 */
public class JoinListener implements Listener {

    private final CorePlugin plugin;
    private final PlayerManager playerManager;
    private final BotManager botManager;

    public JoinListener(CorePlugin plugin, PlayerManager playerManager, BotManager botManager) {
        this.plugin = plugin;
        this.playerManager = playerManager;
        this.botManager = botManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onAsyncPreLogin(AsyncPlayerPreLoginEvent event) {
        if (!plugin.getConfig().getBoolean("settings.enabled", true)) {
            return;
        }

        String uuid = event.getUniqueId().toString();

        if (!playerManager.isWhitelisted(uuid)) {
            rejectConnection(event);
            return;
        }

        String discordId = playerManager.getDiscordId(uuid);
        if (discordId == null || !botManager.hasRole(discordId)) {
            rejectConnection(event);
        }
    }

    private void rejectConnection(AsyncPlayerPreLoginEvent event) {
        String kickMessage = plugin.getConfig().getString("messages.not-whitelisted", 
                "You are not whitelisted on this server.\nJoin our Discord to link your account.");
        event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST, kickMessage);
    }
}
