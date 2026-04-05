package fr.server.core.discord;

import fr.server.core.CorePlugin;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;

import java.awt.Color;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Handles Discord slash command interactions and autocomplete events.
 */
public class SlashCommandListener extends ListenerAdapter {

    private final CorePlugin plugin;

    public SlashCommandListener(CorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (event.getMember() == null)
            return;

        // Restrict commands to Discord Administrators
        if (!event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
            EmbedBuilder eb = new EmbedBuilder();
            eb.setTitle("Permission Denied");
            eb.setDescription("You do not have permission to use this command.\nRequired: `Administrator`");
            eb.setColor(Color.RED);
            event.replyEmbeds(eb.build()).setEphemeral(true).queue();
            return;
        }

        switch (event.getName()) {
            case "whitelist" -> handleWhitelist(event);
            case "unwhitelist" -> handleUnwhitelist(event);
            case "unlink" -> handleUnlink(event);
            case "whitelistlist" -> handleWhitelistList(event);
        }
    }

    /**
     * Provides autocomplete suggestions for the 'pseudo' option.
     */
    @Override
    public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event) {
        if (!event.getFocusedOption().getName().equals("pseudo"))
            return;

        String input = event.getFocusedOption().getValue().toLowerCase();
        List<String> names = plugin.getPlayerManager().getAllPlayerNames();

        List<Command.Choice> choices = names.stream()
                .filter(name -> name.toLowerCase().startsWith(input))
                .limit(25)
                .map(name -> new Command.Choice(name, name))
                .collect(Collectors.toList());

        event.replyChoices(choices).queue();
    }

    /**
     * Handler for /whitelist command.
     */
    private void handleWhitelist(SlashCommandInteractionEvent event) {
        String pseudo = event.getOption("pseudo").getAsString();

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            plugin.getPlayerManager().addPlayerByName(pseudo);

            EmbedBuilder eb = new EmbedBuilder();
            eb.setTitle("Player Whitelisted");
            eb.setDescription("**" + pseudo + "** has been added to the whitelist!");
            eb.setColor(new Color(87, 242, 135));
            eb.addField("Player", pseudo, true);
            eb.addField("Action", "Added to whitelist", true);
            eb.setFooter("DiscordWhitelist • by LEOO955",
                    event.getJDA().getSelfUser().getEffectiveAvatarUrl());

            event.replyEmbeds(eb.build()).queue();
        });
    }

    /**
     * Handler for /unwhitelist command.
     */
    private void handleUnwhitelist(SlashCommandInteractionEvent event) {
        String pseudo = event.getOption("pseudo").getAsString();

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            String discordId = plugin.getPlayerManager().removeWhitelist(pseudo);

            if (discordId == null) {
                EmbedBuilder eb = new EmbedBuilder();
                eb.setTitle("Player Not Found");
                eb.setDescription("**" + pseudo + "** was not found in the database.");
                eb.setColor(Color.RED);
                eb.setFooter("DiscordWhitelist • by LEOO955",
                        event.getJDA().getSelfUser().getEffectiveAvatarUrl());
                event.replyEmbeds(eb.build()).setEphemeral(true).queue();
                return;
            }

            // Remove Discord role if linked
            if (!discordId.isEmpty()) {
                removeDiscordRole(discordId);
            }

            EmbedBuilder eb = new EmbedBuilder();
            eb.setTitle("Player Removed from Whitelist");
            eb.setDescription("**" + pseudo + "** has been removed from the whitelist!");
            eb.setColor(new Color(255, 165, 0));
            eb.addField("Player", pseudo, true);
            eb.addField("Action", "Removed from whitelist", true);
            eb.setFooter("DiscordWhitelist • by LEOO955",
                    event.getJDA().getSelfUser().getEffectiveAvatarUrl());

            event.replyEmbeds(eb.build()).queue();
        });
    }

    /**
     * Handler for /unlink command.
     */
    private void handleUnlink(SlashCommandInteractionEvent event) {
        String pseudo = event.getOption("pseudo").getAsString();

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            String discordId = plugin.getPlayerManager().unlinkPlayer(pseudo);

            if (discordId == null) {
                EmbedBuilder eb = new EmbedBuilder();
                eb.setTitle("Player Not Found");
                eb.setDescription("**" + pseudo + "** was not found in the database.");
                eb.setColor(Color.RED);
                eb.setFooter("DiscordWhitelist • by LEOO955",
                        event.getJDA().getSelfUser().getEffectiveAvatarUrl());
                event.replyEmbeds(eb.build()).setEphemeral(true).queue();
                return;
            }

            // Remove Discord role if was linked
            if (!discordId.isEmpty()) {
                removeDiscordRole(discordId);
            }

            EmbedBuilder eb = new EmbedBuilder();
            eb.setTitle("Player Unlinked");
            eb.setDescription("**" + pseudo + "**'s Discord account has been unlinked!");
            eb.setColor(new Color(255, 215, 0));
            eb.addField("Player", pseudo, true);
            eb.addField("Action", "Discord unlinked", true);
            eb.setFooter("DiscordWhitelist • by LEOO955",
                    event.getJDA().getSelfUser().getEffectiveAvatarUrl());

            event.replyEmbeds(eb.build()).queue();
        });
    }

    /**
     * Handler for /whitelistlist command.
     */
    private void handleWhitelistList(SlashCommandInteractionEvent event) {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            List<Map<String, String>> players = plugin.getPlayerManager().getWhitelistedPlayers();

            EmbedBuilder eb = new EmbedBuilder();
            eb.setTitle("Whitelist (" + players.size() + " players)");
            eb.setColor(new Color(88, 101, 242));

            if (players.isEmpty()) {
                eb.setDescription("No players are currently whitelisted.");
            } else {
                StringBuilder sb = new StringBuilder();
                int count = 0;
                for (Map<String, String> info : players) {
                    count++;
                    String linked = info.get("linked").equals("true") ? "✅" : "❌";
                    String discordTag = info.get("discord_id").equals("N/A") ? "N/A"
                            : "<@" + info.get("discord_id") + ">";
                    sb.append("**").append(count).append(".** `").append(info.get("name")).append("`")
                            .append(" — Discord: ").append(discordTag)
                            .append(" — Linked: ").append(linked)
                            .append("\n");

                    // Discord embed limit: split into multiple fields if too long
                    if (count % 10 == 0) {
                        eb.addField("", sb.toString(), false);
                        sb.setLength(0);
                    }
                }
                if (sb.length() > 0) {
                    eb.addField("", sb.toString(), false);
                }
            }

            eb.setFooter("DiscordWhitelist • by LEOO955",
                    event.getJDA().getSelfUser().getEffectiveAvatarUrl());

            event.replyEmbeds(eb.build()).queue();
        });
    }

    /**
     * Revokes the white-list role from a specified Discord user ID.
     */
    private void removeDiscordRole(String discordId) {
        String guildId = plugin.getConfig().getString("bot.guild-id", "").trim().replace("/", "");
        String roleName = plugin.getConfig().getString("bot.role-name", "Whitelisted");

        if (guildId.isEmpty())
            return;

        Guild guild = plugin.getBotManager().getJda().getGuildById(guildId);
        if (guild == null)
            return;

        guild.retrieveMemberById(discordId).queue(member -> {
            List<Role> roles = guild.getRolesByName(roleName, true);
            if (!roles.isEmpty()) {
                Role role = roles.get(0);
                guild.removeRoleFromMember(member, role).queue(
                        success -> plugin.getLogger()
                                .info("Removed '" + roleName + "' role from " + member.getEffectiveName()),
                        error -> plugin.getLogger()
                                .severe("Failed to remove role: " + error.getMessage()));
            }
        });
    }
}
