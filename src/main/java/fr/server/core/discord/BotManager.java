package fr.server.core.discord;

import fr.server.core.CorePlugin;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.requests.GatewayIntent;

/**
 * Manages the Discord bot lifecycle using the JDA library.
 * Handles bot startup, shutdown, role validation, and slash command registration.
 */
public class BotManager {

    private final CorePlugin plugin;
    private JDA jda;

    public BotManager(CorePlugin plugin) {
        this.plugin = plugin;
        startBot();
    }

    /**
     * Initializes the JDA bot instance with required intents and listeners.
     */
    private void startBot() {
        String token = plugin.getConfig().getString("bot.token");
        
        // Validate token before attempting to start
        if (token == null || token.isEmpty() || token.equals("YOUR_BOT_TOKEN_HERE")) {
            plugin.getLogger().warning("Discord Bot Token is missing! The bot will not start.");
            return;
        }

        // Start JDA asynchronously to avoid blocking the main server thread
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                jda = JDABuilder.createDefault(token)
                        .enableIntents(GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MEMBERS, GatewayIntent.DIRECT_MESSAGES)
                        .addEventListeners(new DiscordListener(plugin), new SlashCommandListener(plugin))
                        .build();
                
                // Wait for the bot to be fully loaded
                jda.awaitReady();
                plugin.getLogger().info("Discord Bot has successfully connected!");

                // Post-startup registration
                ensureWhitelistedRoleExists();
                registerSlashCommands();
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to start the Discord bot!");
                e.printStackTrace();
            }
        });
    }

    /**
     * Properly shuts down the JDA instance.
     */
    public void stopBot() {
        if (jda != null) {
            jda.shutdown();
            plugin.getLogger().info("Discord Bot has been shut down.");
        }
    }

    /**
     * @return The active JDA instance.
     */
    public JDA getJda() {
        return jda;
    }

    /**
     * Registers the 4 Discord Slash Commands on the configured Guild.
     */
    private void registerSlashCommands() {
        String guildId = plugin.getConfig().getString("bot.guild-id", "").trim().replace("/", "");
        if (guildId.isEmpty() || guildId.equals("YOUR_GUILD_ID_HERE")) {
            plugin.getLogger().warning("Cannot register slash commands: Guild ID is missing!");
            return;
        }

        Guild guild = jda.getGuildById(guildId);
        if (guild == null) {
            plugin.getLogger().warning("Cannot register slash commands: Guild not found!");
            return;
        }

        // Common pseudo option with autocomplete enabled
        OptionData pseudoOption = new OptionData(OptionType.STRING, "pseudo", "Minecraft player name", true)
                .setAutoComplete(true);

        // Update commands list on the specific guild for faster propagation
        guild.updateCommands().addCommands(
                Commands.slash("whitelist", "Add a player to the whitelist")
                        .addOptions(pseudoOption),
                Commands.slash("unwhitelist", "Remove a player from the whitelist")
                        .addOptions(new OptionData(OptionType.STRING, "pseudo", "Minecraft player name", true).setAutoComplete(true)),
                Commands.slash("unlink", "Unlink a player's Discord account")
                        .addOptions(new OptionData(OptionType.STRING, "pseudo", "Minecraft player name", true).setAutoComplete(true)),
                Commands.slash("whitelistlist", "Show all whitelisted players")
        ).queue(
                success -> plugin.getLogger().info("Successfully registered 4 slash commands!"),
                error -> plugin.getLogger().severe("Failed to register slash commands: " + error.getMessage())
        );
    }

    /**
     * Verifies if the whitelisted role exists in the guild, creates it if missing.
     */
    private void ensureWhitelistedRoleExists() {
        String guildId = plugin.getConfig().getString("bot.guild-id", "").trim().replace("/", "");
        String roleName = plugin.getConfig().getString("bot.role-name", "Whitelisted");

        if (guildId == null || guildId.isEmpty() || guildId.equals("YOUR_GUILD_ID_HERE")) return;

        net.dv8tion.jda.api.entities.Guild guild = jda.getGuildById(guildId);
        if (guild == null) return;

        boolean roleExists = guild.getRolesByName(roleName, true).stream()
                .anyMatch(role -> role.getName().equalsIgnoreCase(roleName));

        if (!roleExists) {
            plugin.getLogger().info("Role '" + roleName + "' not found. Creating it...");
            guild.createRole()
                    .setName(roleName)
                    .setPermissions(0L)
                    .setHoisted(false)
                    .setMentionable(false)
                    .queue();
        }
    }

    /**
     * Checks if a Discord user possesses the required whitelisted role.
     */
    public boolean hasRole(String discordId) {
        if (jda == null) return false;

        String guildId = plugin.getConfig().getString("bot.guild-id", "").trim().replace("/", "");
        String roleName = plugin.getConfig().getString("bot.role-name", "Whitelisted");

        if (guildId.isEmpty() || guildId.equals("YOUR_GUILD_ID_HERE")) return false;

        net.dv8tion.jda.api.entities.Guild guild = jda.getGuildById(guildId);
        if (guild == null) return false;

        try {
            net.dv8tion.jda.api.entities.Member member = guild.retrieveMemberById(discordId).complete();
            if (member == null) return false;

            return member.getRoles().stream()
                    .anyMatch(role -> role.getName().equalsIgnoreCase(roleName));
        } catch (Exception e) {
            return false;
        }
    }
}

