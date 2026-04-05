package fr.server.core.discord;

import fr.server.core.CorePlugin;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.awt.Color;
import java.util.List;

/**
 * Listens for private messages to the bot for account linking.
 */
public class DiscordListener extends ListenerAdapter {

    private final CorePlugin plugin;

    public DiscordListener(CorePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Triggered when the bot receives a message.
     * Filters for private messages to process linking codes.
     */
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        // Ignore bot messages
        if (event.getAuthor().isBot()) return;

        // Process only Direct Messages (DMs)
        if (!event.isFromGuild()) {
            String code = event.getMessage().getContentRaw().trim();
            String discordId = event.getAuthor().getId();

            // Attempt to validate the provided code
            boolean success = plugin.getPlayerManager().validateCode(code, discordId);

            if (success) {
                // Success: Notify the user and assign the Discord role
                EmbedBuilder eb = new EmbedBuilder();
                eb.setTitle("✅ Minecraft Account Linked");
                eb.setDescription("Your Minecraft account has been successfully linked!\nYou can now connect to the server.");
                eb.setColor(Color.GREEN);
                eb.setThumbnail(event.getAuthor().getEffectiveAvatarUrl());
                
                event.getChannel().sendMessageEmbeds(eb.build()).queue();

                assignRole(event.getAuthor().getId());
            } else {
                // Failure: Notify the user about the invalid code
                EmbedBuilder eb = new EmbedBuilder();
                eb.setTitle("❌ Verification Failed");
                eb.setDescription("Invalid or expired code.\nPlease join the Minecraft server again to receive a new code.");
                eb.setColor(Color.RED);
                
                event.getChannel().sendMessageEmbeds(eb.build()).queue();
            }
        }
    }

    /**
     * Assigns the configured whitelisted role to a Discord member.
     */
    private void assignRole(String discordId) {
        String guildId = plugin.getConfig().getString("bot.guild-id", "").trim().replace("/", "");
        String roleName = plugin.getConfig().getString("bot.role-name", "Whitelisted");

        if (guildId.isEmpty()) return;

        Guild guild = plugin.getBotManager().getJda().getGuildById(guildId);
        if (guild == null) return;

        // Retrieve the member and add the role
        guild.retrieveMemberById(discordId).queue(member -> {
            List<Role> roles = guild.getRolesByName(roleName, true);
            if (!roles.isEmpty()) {
                Role role = roles.get(0);
                guild.addRoleToMember(member, role).queue(
                        success -> plugin.getLogger().info("Assigned '" + roleName + "' role to " + member.getEffectiveName()),
                        error -> plugin.getLogger().severe("Failed to assign role: " + error.getMessage()));
            }
        });
    }
}
