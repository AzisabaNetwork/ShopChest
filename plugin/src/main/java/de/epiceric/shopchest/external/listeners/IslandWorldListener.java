package de.epiceric.shopchest.external.listeners;

import de.epiceric.shopchest.ShopChest;
import de.epiceric.shopchest.config.Config;
import de.epiceric.shopchest.event.ShopCreateEvent;
import de.epiceric.shopchest.event.ShopExtendEvent;
import de.epiceric.shopchest.utils.Utils;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import pl.islandworld.api.IslandWorldApi;

import java.util.Set;

public class IslandWorldListener implements Listener {
    private final ShopChest plugin;

    public IslandWorldListener(ShopChest plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCreateShop(ShopCreateEvent e) {
        if (!Config.enableIslandWorldIntegration || !IslandWorldApi.isInitialized())
            return;

        Set<Location> chestLocations = Utils.getChestLocations(e.getShop());
        for (Location loc : chestLocations) {
            if (handleForLocation(e.getPlayer(), loc, e))
                return;
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onExtendShop(ShopExtendEvent e) {
        if (!Config.enableIslandWorldIntegration || !IslandWorldApi.isInitialized())
            return;

        handleForLocation(e.getPlayer(), e.getNewChestLocation(), e);
    }

    private boolean handleForLocation(Player player, Location loc, Cancellable e) {
        if (!loc.getWorld().getName().equals(IslandWorldApi.getIslandWorld().getName()))
            return false;

        if (!IslandWorldApi.canBuildOnLocation(player, loc, true)) {
            e.setCancelled(true);
            plugin.debug("Cancel Reason: IslandWorld");
            return true;
        }

        return false;
    }

}