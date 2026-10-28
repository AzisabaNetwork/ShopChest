package de.epiceric.shopchest.finder;

import org.bukkit.Bukkit;

import net.azisaba.townia.api.TowniaAPI;
import net.azisaba.townia.data.Plot;
import net.azisaba.townia.data.Town;

/**
 * Resolves the Townia town a shop is located in. Townia classes are only
 * referenced here, so the finder still works when Townia is not installed.
 */
final class FinderTowns {
    private FinderTowns() {}

    /**
     * @return The name of the town claiming the chunk, or {@code null} if the
     *         chunk is wilderness or Townia is unavailable
     */
    static String getTownName(String world, int blockX, int blockZ) {
        if (!Bukkit.getPluginManager().isPluginEnabled("Townia")) return null;

        try {
            TowniaAPI api = TowniaAPI.get();
            if (api == null) return null;

            // Looks up Townia's in-memory plot cache; the chunk does not need to be loaded
            Plot plot = api.getPlot(world, blockX >> 4, blockZ >> 4).orElse(null);
            if (plot == null || plot.getTownUuid() == null) return null;

            Town town = api.getTown(plot.getTownUuid()).orElse(null);
            return town == null ? null : town.getName();
        } catch (LinkageError | RuntimeException e) {
            // Incompatible Townia version: just don't show town names
            return null;
        }
    }
}
