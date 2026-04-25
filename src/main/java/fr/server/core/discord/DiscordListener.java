package fr.server.core.discord;

import fr.server.core.CorePlugin;
import fr.server.core.managers.PlayerManager;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

/**
 * This listener keeps an eye on Discord events.
 * For example, if someone loses their 'Whitelisted' role on Discord, 
 * we'll automatically remove them from the Minecraft whitelist too.
 */
public class DiscordListener extends ListenerAdapter {

    private final CorePlugin plugin;
    private final PlayerManager playerManager;

    public DiscordListener(CorePlugin plugin, PlayerManager playerManager) {
        this.plugin = plugin;
        this.playerManager = playerManager;
    }

    @Override
    public void onGuildMemberRoleRemove(@NotNull GuildMemberRoleRemoveEvent event) {
        String roleName = plugin.getConfig().getString("bot.role-name", "Whitelisted");
        
        // We check if the role they just lost is the one we care about.
        boolean affected = event.getRoles().stream()
                .anyMatch(role -> role.getName().equalsIgnoreCase(roleName));

        if (affected) {
            String discordId = event.getUser().getId();
            
            // If they lost the role, we find their Minecraft account and remove it from the whitelist.
            playerManager.getWhitelistedPlayers().stream()
                    .filter(uuid -> discordId.equals(playerManager.getDiscordId(uuid)))
                    .forEach(uuid -> {
                        playerManager.unwhitelistPlayer(uuid);
                        plugin.getLogger().info("The whitelist for " + uuid + " has been revoked because they lost their Discord role.");
                    });
        }
    }
}
