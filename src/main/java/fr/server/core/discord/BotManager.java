package fr.server.core.discord;

import fr.server.core.CorePlugin;
import fr.server.core.managers.PlayerManager;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.SelfUser;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.requests.GatewayIntent;

public class BotManager {

    private final CorePlugin plugin;
    private final PlayerManager playerManager;
    private JDA jda;

    public BotManager(CorePlugin plugin, PlayerManager playerManager) {
        this.plugin = plugin;
        this.playerManager = playerManager;
        startBot();
    }

    private void startBot() {
        String rawToken = System.getenv("DISCORD_BOT_TOKEN");
        if (rawToken == null || rawToken.isEmpty()) {
            rawToken = plugin.getConfig().getString("bot.token");
        }
        
        // We need a final copy for the background task, then we wipe the original for safety.
        final String token = rawToken;
        rawToken = null; 

        if (token == null || token.isEmpty() || token.equals("YOUR_BOT_TOKEN_HERE")) {
            plugin.getLogger().severe("Oops! We couldn't find a valid bot token. The bot won't start.");
            return;
        }

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                // We're asking Discord for just enough permissions to do our job.
                jda = JDABuilder.createDefault(token)
                        .enableIntents(GatewayIntent.GUILD_MEMBERS) // We need to see members to check their roles.
                        .disableIntents(GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_PRESENCES) // We don't need to read messages or see who's online.
                        .addEventListeners(new DiscordListener(plugin, playerManager), new SlashCommandListener(plugin, playerManager))
                        .build();
                
                jda.awaitReady();
                
                // Let's make sure the bot has all the permissions it needs.
                auditPermissions();
                
                plugin.getLogger().info("Success! The bot is connected and ready to go.");

                ensureWhitelistedRoleExists();
                registerSlashCommands();
            } catch (Exception e) {
                plugin.getLogger().severe("Something went wrong while starting the bot. Is the token correct?");
            }
        });
    }

    private void auditPermissions() {
        if (jda == null) return;
        SelfUser self = jda.getSelfUser();
        plugin.getLogger().info("Checking permissions for: " + self.getAsTag());
        
        String guildId = plugin.getConfig().getString("bot.guild-id", "").trim().replace("/", "");
        Guild guild = jda.getGuildById(guildId);
        
        if (guild != null && !guild.getSelfMember().hasPermission(Permission.MANAGE_ROLES)) {
            plugin.getLogger().warning("Wait! The bot doesn't have the 'Manage Roles' permission. It won't be able to give roles.");
        }
    }

    public void stopBot() {
        if (jda != null) {
            jda.shutdownNow(); // Shut down everything immediately.
            jda = null;
            plugin.getLogger().info("The bot has been safely disconnected.");
        }
    }

    private void registerSlashCommands() {
        String guildId = plugin.getConfig().getString("bot.guild-id", "").trim().replace("/", "");
        if (guildId.isEmpty() || guildId.equals("YOUR_GUILD_ID_HERE")) return;

        Guild guild = jda.getGuildById(guildId);
        if (guild == null) return;

        OptionData pseudoOption = new OptionData(OptionType.STRING, "pseudo", "Minecraft player name", true)
                .setAutoComplete(true);

        guild.updateCommands().addCommands(
                Commands.slash("whitelist", "Add a player to the whitelist (Admin Only)")
                        .addOptions(pseudoOption),
                Commands.slash("unwhitelist", "Remove a player from the whitelist (Admin Only)")
                        .addOptions(new OptionData(OptionType.STRING, "pseudo", "Minecraft player name", true)),
                Commands.slash("unlink", "Unlink a player's Discord account (Admin Only)")
                        .addOptions(new OptionData(OptionType.STRING, "pseudo", "Minecraft player name", true)),
                Commands.slash("whitelistlist", "Show all whitelisted players (Admin Only)")
        ).queue();
    }

    private void ensureWhitelistedRoleExists() {
        String guildId = plugin.getConfig().getString("bot.guild-id", "").trim().replace("/", "");
        String roleName = plugin.getConfig().getString("bot.role-name", "Whitelisted");

        if (guildId == null || guildId.isEmpty() || guildId.equals("YOUR_GUILD_ID_HERE")) return;

        Guild guild = jda.getGuildById(guildId);
        if (guild == null) return;

        boolean roleExists = guild.getRolesByName(roleName, true).stream()
                .anyMatch(role -> role.getName().equalsIgnoreCase(roleName));

        if (!roleExists) {
            plugin.getLogger().info("Action: Creating missing role '" + roleName + "' with zero permissions.");
            guild.createRole()
                    .setName(roleName)
                    .setPermissions(0L)
                    .queue();
        }
    }

    public boolean hasRole(String discordId) {
        if (jda == null) return false;

        String guildId = plugin.getConfig().getString("bot.guild-id", "").trim().replace("/", "");
        String roleName = plugin.getConfig().getString("bot.role-name", "Whitelisted");

        if (guildId.isEmpty()) return false;

        Guild guild = jda.getGuildById(guildId);
        if (guild == null) return false;

        try {
            // We're already on a separate thread, so waiting for a response here is fine.
            net.dv8tion.jda.api.entities.Member member = guild.retrieveMemberById(discordId).complete();
            if (member == null) return false;

            return member.getRoles().stream()
                    .anyMatch(role -> role.getName().equalsIgnoreCase(roleName));
        } catch (Exception e) {
            return false;
        }
    }
}
