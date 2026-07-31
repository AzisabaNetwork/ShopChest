package de.epiceric.shopchest.nms;

import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.entity.Display.Billboard;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;

import de.epiceric.shopchest.ShopChest;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

/**
 * A native 1.21 TextDisplay-backed hologram line.
 *
 * The class name is retained to avoid a broad API-breaking rename inside this
 * legacy plugin; it no longer creates or sends ArmorStand/NMS packets.
 */
public class ArmorStandWrapper {
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacySection();
    // Legacy hologram locations were the feet of a marker ArmorStand. Its
    // displayed name was about 1.975 blocks above that point; TextDisplay's
    // origin is the text itself, so retain the former visual placement.
    private static final double TEXT_DISPLAY_Y_OFFSET = 1.975;

    private final ShopChest plugin;
    private final TextDisplay display;
    private Location location;
    private String customName;

    public ArmorStandWrapper(ShopChest plugin, Location location, String customName) {
        this.plugin = plugin;
        this.location = location.clone();
        this.customName = customName;
        Location displayLocation = location.clone().add(0, TEXT_DISPLAY_Y_OFFSET, 0);
        this.display = location.getWorld().spawn(displayLocation, TextDisplay.class, textDisplay -> {
            textDisplay.text(LEGACY_SERIALIZER.deserialize(customName));
            textDisplay.setBillboard(Billboard.CENTER);
            textDisplay.setSeeThrough(true);
            textDisplay.setShadowed(false);
            textDisplay.setDefaultBackground(false);
            textDisplay.setPersistent(false);
            textDisplay.setInvulnerable(true);
        });

        // Hologram visibility is managed per player by Hologram, so newly
        // created displays must not flash to players outside its view range.
        for (Player player : location.getWorld().getPlayers()) {
            player.hideEntity(plugin, display);
        }
    }

    public void setVisible(Player player, boolean visible) {
        if (visible) {
            player.showEntity(plugin, display);
        } else {
            player.hideEntity(plugin, display);
        }
    }

    public void setLocation(Location location) {
        this.location = location.clone();
        display.teleport(location.clone().add(0, TEXT_DISPLAY_Y_OFFSET, 0));
    }

    public void setCustomName(String customName) {
        this.customName = customName;
        display.text(LEGACY_SERIALIZER.deserialize(customName));
    }

    public void remove() {
        display.remove();
    }

    public UUID getUuid() {
        return display.getUniqueId();
    }

    public Location getLocation() {
        return location.clone();
    }

    public String getCustomName() {
        return customName;
    }

}
