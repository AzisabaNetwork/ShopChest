package de.epiceric.shopchest.finder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;

import de.epiceric.shopchest.ShopChest;

/**
 * Per-player finder settings: whether a vendor's shops are listed for other
 * players and whether other players may teleport to them.
 */
class FinderPlayerSettings {
    private static final String FILE_NAME = "finder-data.yml";

    private final ShopChest plugin;
    private final File file;
    // Read by the async cache rebuild, hence concurrent
    private final Set<UUID> publicPlayers = ConcurrentHashMap.newKeySet();
    private final Set<UUID> tpAllowedPlayers = ConcurrentHashMap.newKeySet();
    private final Object saveLock = new Object();

    FinderPlayerSettings(ShopChest plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), FILE_NAME);
        load();
    }

    /**
     * Copies data.yml of the old ShopChestFinder plugin if no data exists yet.
     */
    static void migrateLegacyData(ShopChest plugin, File legacyFile) {
        File file = new File(plugin.getDataFolder(), FILE_NAME);
        if (file.exists() || !legacyFile.isFile()) return;

        try {
            Files.copy(legacyFile.toPath(), file.toPath(), StandardCopyOption.COPY_ATTRIBUTES);
            plugin.getLogger().info("Imported player settings from plugins/ShopChestFinder/data.yml");
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to import plugins/ShopChestFinder/data.yml: " + e.getMessage());
        }
    }

    private void load() {
        YamlConfiguration data = YamlConfiguration.loadConfiguration(file);
        readUuids(data.getStringList("public-players"), publicPlayers);
        readUuids(data.getStringList("tp-allowed-players"), tpAllowedPlayers);
    }

    private void readUuids(List<String> values, Set<UUID> target) {
        for (String value : values) {
            try {
                target.add(UUID.fromString(value));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid UUID in " + FILE_NAME + ": " + value);
            }
        }
    }

    boolean isPublic(UUID uuid) {
        return publicPlayers.contains(uuid);
    }

    void setPublic(UUID uuid, boolean isPublic) {
        if (isPublic ? publicPlayers.add(uuid) : publicPlayers.remove(uuid)) {
            saveAsync();
        }
    }

    boolean isTpAllowed(UUID uuid) {
        return tpAllowedPlayers.contains(uuid);
    }

    void setTpAllowed(UUID uuid, boolean allowed) {
        if (allowed ? tpAllowedPlayers.add(uuid) : tpAllowedPlayers.remove(uuid)) {
            saveAsync();
        }
    }

    private void saveAsync() {
        if (!plugin.isEnabled()) {
            saveNow();
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, this::saveNow);
    }

    void saveNow() {
        // Snapshot and write under one lock so an older snapshot never overwrites a newer one
        synchronized (saveLock) {
            YamlConfiguration data = new YamlConfiguration();
            data.set("public-players", toStrings(publicPlayers));
            data.set("tp-allowed-players", toStrings(tpAllowedPlayers));

            try {
                data.save(file);
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to save " + FILE_NAME + ": " + e.getMessage());
            }
        }
    }

    private static List<String> toStrings(Set<UUID> uuids) {
        List<String> result = new ArrayList<>(uuids.size());
        for (UUID uuid : uuids) result.add(uuid.toString());
        return result;
    }
}
