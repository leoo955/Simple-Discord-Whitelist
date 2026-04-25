package fr.server.core.managers;

import fr.server.core.CorePlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Set;

public class PlayerManager {

    private final CorePlugin plugin;
    private final File playersFile;
    private FileConfiguration playersConfig;

    public PlayerManager(CorePlugin plugin) {
        this.plugin = plugin;
        this.playersFile = new File(plugin.getDataFolder(), "players.yml");
        loadConfig();
    }

    /**
     * This loads our player database. If it doesn't exist, we create a new one.
     */
    private void loadConfig() {
        if (!playersFile.exists()) {
            try {
                playersFile.getParentFile().mkdirs();
                playersFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Oops! We couldn't create the players.yml file.");
            }
        }
        playersConfig = YamlConfiguration.loadConfiguration(playersFile);
    }

    public void whitelistPlayer(String uuid, String discordId) {
        playersConfig.set("players." + uuid + ".discordId", discordId);
        saveConfig();
    }

    public void unwhitelistPlayer(String uuid) {
        playersConfig.set("players." + uuid, null);
        saveConfig();
    }

    public boolean isWhitelisted(String uuid) {
        return playersConfig.contains("players." + uuid);
    }

    public String getDiscordId(String uuid) {
        return playersConfig.getString("players." + uuid + ".discordId");
    }

    public Set<String> getWhitelistedPlayers() {
        if (playersConfig.getConfigurationSection("players") == null) {
            return Set.of();
        }
        return playersConfig.getConfigurationSection("players").getKeys(false);
    }

    private void saveConfig() {
        try {
            playersConfig.save(playersFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Database Error: Critical failure while saving players.yml.");
        }
    }
}
