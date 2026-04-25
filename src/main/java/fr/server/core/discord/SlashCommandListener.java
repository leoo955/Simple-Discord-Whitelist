package fr.server.core.discord;

import fr.server.core.CorePlugin;
import fr.server.core.managers.PlayerManager;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Collectors;

/**
 * This class handles all the slash commands from Discord.
 * It's how admins can manage the whitelist without even opening Minecraft.
 */
public class SlashCommandListener extends ListenerAdapter {

    private final CorePlugin plugin;
    private final PlayerManager playerManager;

    public SlashCommandListener(CorePlugin plugin, PlayerManager playerManager) {
        this.plugin = plugin;
        this.playerManager = playerManager;
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        // We only want admins to be able to use these commands.
        if (event.getMember() == null || !event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
            event.reply("Sorry, you need to be an administrator to use this.").setEphemeral(true).queue();
            return;
        }

        switch (event.getName()) {
            case "whitelist" -> handleWhitelist(event);
            case "unwhitelist" -> handleUnwhitelist(event);
            case "unlink" -> handleUnlink(event);
            case "whitelistlist" -> handleList(event);
        }
    }

    private void handleWhitelist(SlashCommandInteractionEvent event) {
        String pseudo = event.getOption("pseudo").getAsString();
        // Let's make sure the name isn't too long.
        if (pseudo.length() > 16) {
            event.reply("That name looks a bit too long for Minecraft.").setEphemeral(true).queue();
            return;
        }

        OfflinePlayer player = Bukkit.getOfflinePlayer(pseudo);
        String uuid = player.getUniqueId().toString();
        Member member = event.getMember();

        playerManager.whitelistPlayer(uuid, member.getId());

        String roleName = plugin.getConfig().getString("bot.role-name", "Whitelisted");
        Role role = event.getGuild().getRolesByName(roleName, true).stream().findFirst().orElse(null);

        if (role != null) {
            event.getGuild().addRoleToMember(member, role).queue();
        }

        event.reply("Got it! **" + pseudo + "** has been added to the whitelist.").setEphemeral(true).queue();
    }

    private void handleUnwhitelist(SlashCommandInteractionEvent event) {
        String pseudo = event.getOption("pseudo").getAsString();
        OfflinePlayer player = Bukkit.getOfflinePlayer(pseudo);
        playerManager.unwhitelistPlayer(player.getUniqueId().toString());
        
        event.reply("**" + pseudo + "** has been removed from the whitelist.").setEphemeral(true).queue();
    }

    private void handleUnlink(SlashCommandInteractionEvent event) {
        String pseudo = event.getOption("pseudo").getAsString();
        OfflinePlayer player = Bukkit.getOfflinePlayer(pseudo);
        playerManager.unwhitelistPlayer(player.getUniqueId().toString());
        
        event.reply("Account unlinked for **" + pseudo + "**.").setEphemeral(true).queue();
    }

    private void handleList(SlashCommandInteractionEvent event) {
        String list = playerManager.getWhitelistedPlayers().stream()
                .limit(50) // We'll show up to 50 players so we don't spam the chat.
                .map(uuid -> {
                    try {
                        return Bukkit.getOfflinePlayer(java.util.UUID.fromString(uuid)).getName();
                    } catch (Exception e) {
                        return "Unknown";
                    }
                })
                .collect(Collectors.joining(", "));
        
        event.reply("Here are the first 50 whitelisted players: " + (list.isEmpty() ? "None yet!" : list)).setEphemeral(true).queue();
    }
}
