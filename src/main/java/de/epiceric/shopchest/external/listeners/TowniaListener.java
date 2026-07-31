package de.epiceric.shopchest.external.listeners;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import de.epiceric.shopchest.ShopChest;
import de.epiceric.shopchest.config.Config;
import de.epiceric.shopchest.event.ShopCreateEvent;
import de.epiceric.shopchest.event.ShopExtendEvent;
import de.epiceric.shopchest.utils.Utils;
import net.azisaba.townia.api.TowniaAPI;
import net.azisaba.townia.data.Plot;
import net.azisaba.townia.data.Town;
import net.azisaba.townia.data.TowniaPlayer;

/** Applies ShopChest's legacy Towny plot settings to Townia plots. */
public class TowniaListener implements Listener {
    private final ShopChest plugin;

    public TowniaListener(ShopChest plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCreateShop(ShopCreateEvent event) {
        Set<Location> chestLocations = Utils.getChestLocations(event.getShop());
        for (Location location : chestLocations) {
            if (handle(event.getPlayer(), location, event)) return;
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onExtendShop(ShopExtendEvent event) {
        handle(event.getPlayer(), event.getNewChestLocation(), event);
    }

    private boolean handle(Player player, Location location, Cancellable event) {
        TowniaAPI api = TowniaAPI.get();
        if (api == null) return false;

        Plot plot = api.getPlot(location.getChunk()).orElse(null);
        if (plot == null || plot.getTownUuid() == null) return false;

        Town town = api.getTown(plot.getTownUuid()).orElse(null);
        TowniaPlayer resident = api.getResident(player.getUniqueId()).orElse(null);
        if (town == null || resident == null || !town.getId().equals(resident.getTownUuid())) {
            event.setCancelled(true);
            plugin.debug("Cancel Reason: Townia (not a resident of this town)");
            return true;
        }

        List<String> allowedPlots = resident.isMayor()
                ? Config.townyShopPlotsMayor : Config.townyShopPlotsResidents;
        String plotType = plot.getPlotType() == null ? "DEFAULT" : plot.getPlotType().name();
        if (!isAllowed(allowedPlots, plotType)) {
            event.setCancelled(true);
            plugin.debug("Cancel Reason: Townia (plot type is not allowed)");
            return true;
        }
        return false;
    }

    private boolean isAllowed(List<String> allowedPlots, String plotType) {
        String normalized = plotType.toUpperCase(Locale.ROOT);
        for (String allowed : allowedPlots) {
            String candidate = allowed.toUpperCase(Locale.ROOT);
            if (candidate.equals(normalized)
                    || (candidate.equals("COMMERCIAL") && normalized.equals("SHOP"))
                    || (candidate.equals("RESIDENTIAL") && normalized.equals("DEFAULT"))) {
                return true;
            }
        }
        return false;
    }
}
