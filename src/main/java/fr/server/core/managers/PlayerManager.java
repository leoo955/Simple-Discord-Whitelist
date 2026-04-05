package fr.server.core.managers;

import fr.server.core.CorePlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Manages player data persistence using a YAML file.
 * Handles whitelist status, Discord linking, and pending verification codes.
 */
public class PlayerManager {

    private final CorePlugin plugin;
    private final File file;
    private FileConfiguration config;
    
    // Stores temporary linking codes mapped to player UUIDs
    private final Map<String, UUID> pendingCodes = new HashMap<>();

    public PlayerManager(CorePlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "players.yml");
        
        // Create the data file if it doesn't exist
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    /**
     * Saves the current player data to the YAML file.
     */
    public void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Initializes or updates a player's entry in the database.
     */
    public void ensurePlayerExists(UUID uuid, String name) {
        String path = "players." + uuid.toString();
        if (!config.contains(path)) {
            config.set(path + ".name", name);
            config.set(path + ".whitelisted", false);
            config.set(path + ".linked", false);
            config.set(path + ".discord_id", null);
            save();
        } else {
            // Update the name in case the player changed it
            config.set(path + ".name", name);
            save();
        }
    }

    /**
     * @return True if the player is marked as whitelisted in the data file.
     */
    public boolean isWhitelisted(UUID uuid) {
        return config.getBoolean("players." + uuid.toString() + ".whitelisted", false);
    }

    /**
     * @return True if the player has successfully linked their Discord account.
     */
    public boolean isLinked(UUID uuid) {
        return config.getBoolean("players." + uuid.toString() + ".linked", false);
    }

    /**
     * Registers a new pending linking code for a player.
     * Spaces are removed to normalize the code for bot comparison.
     */
    public void addPendingCode(UUID uuid, String code) {
        pendingCodes.put(code.replace(" ", ""), uuid);
    }

    /**
     * Validates a code provided by a user on Discord.
     * If valid, links the player's UUID to the provided Discord ID.
     */
    public boolean validateCode(String code, String discordId) {
        String cleanCode = code.replace(" ", "");
        UUID uuid = pendingCodes.get(cleanCode);
        
        if (uuid == null) return false;

        String path = "players." + uuid.toString();
        config.set(path + ".linked", true);
        config.set(path + ".whitelisted", true);
        config.set(path + ".discord_id", discordId);
        save();
        
        pendingCodes.remove(cleanCode);
        return true;
    }

    /**
     * @return The Discord ID associated with a player, or null if not linked.
     */
    public String getDiscordId(UUID uuid) {
        return config.getString("players." + uuid.toString() + ".discord_id");
    }

    // ========================================
    // SLASH COMMAND SUPPORTING METHODS
    // ========================================

    /**
     * Searches for a player's UUID based on their Minecraft username.
     * Performs a case-insensitive search through all entries in players.yml.
     */
    public UUID findUuidByName(String name) {
        ConfigurationSection section = config.getConfigurationSection("players");
        if (section == null) return null;

        for (String key : section.getKeys(false)) {
            String storedName = config.getString("players." + key + ".name");
            if (storedName != null && storedName.equalsIgnoreCase(name)) {
                try {
                    return UUID.fromString(key);
                } catch (IllegalArgumentException e) {
                    return null;
                }
            }
        }
        return null;
    }

    /**
     * Forcefully adds a player to the whitelist by name.
     * If the player doesn't exist, a placeholder entry is created.
     */
    public boolean addPlayerByName(String name) {
        UUID uuid = findUuidByName(name);
        if (uuid != null) {
            config.set("players." + uuid.toString() + ".whitelisted", true);
            save();
            return true;
        }
        // Generate a deterministic UUID based on name for offline/new players
        UUID newUuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes());
        String path = "players." + newUuid.toString();
        config.set(path + ".name", name);
        config.set(path + ".whitelisted", true);
        config.set(path + ".linked", false);
        config.set(path + ".discord_id", null);
        save();
        return true;
    }

    /**
     * Removes a player from the whitelist and returns their Discord ID for role removal.
     */
    public String removeWhitelist(String name) {
        UUID uuid = findUuidByName(name);
        if (uuid == null) return null;

        String path = "players." + uuid.toString();
        String discordId = config.getString(path + ".discord_id");
        config.set(path + ".whitelisted", false);
        save();
        return discordId != null ? discordId : "";
    }

    /**
     * Resets a player's linking status.
     * The player remains whitelisted but must re-link their Discord account.
     */
    public String unlinkPlayer(String name) {
        UUID uuid = findUuidByName(name);
        if (uuid == null) return null;

        String path = "players." + uuid.toString();
        String discordId = config.getString(path + ".discord_id");
        config.set(path + ".linked", false);
        config.set(path + ".discord_id", null);
        save();
        return discordId != null ? discordId : "";
    }

    /**
     * Collects all currently whitelisted players and their status.
     */
    public List<Map<String, String>> getWhitelistedPlayers() {
        List<Map<String, String>> players = new ArrayList<>();
        ConfigurationSection section = config.getConfigurationSection("players");
        if (section == null) return players;

        for (String key : section.getKeys(false)) {
            boolean whitelisted = config.getBoolean("players." + key + ".whitelisted", false);
            if (whitelisted) {
                Map<String, String> info = new LinkedHashMap<>();
                info.put("name", config.getString("players." + key + ".name", "Unknown"));
                info.put("uuid", key);
                info.put("discord_id", config.getString("players." + key + ".discord_id", "N/A"));
                info.put("linked", String.valueOf(config.getBoolean("players." + key + ".linked", false)));
                players.add(info);
            }
        }
        return players;
    }

    /**
     * @return A list of all known player names, used for Discord command autocomplete.
     */
    public List<String> getAllPlayerNames() {
        List<String> names = new ArrayList<>();
        ConfigurationSection section = config.getConfigurationSection("players");
        if (section == null) return names;

        for (String key : section.getKeys(false)) {
            String name = config.getString("players." + key + ".name");
            if (name != null) {
                names.add(name);
            }
        }
        return names;
    }
}
