package de.epiceric.shopchest.shop;

import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import de.epiceric.shopchest.ShopChest;

/** A native Bukkit item display for a shop. */
public class ShopItem {
    private final ShopChest plugin;
    private final Set<UUID> viewers = ConcurrentHashMap.newKeySet();
    private final ItemStack itemStack;
    private final Location location;
    private final Item item;

    public ShopItem(ShopChest plugin, ItemStack itemStack, Location location) {
        this.plugin = plugin;
        this.itemStack = itemStack.clone();
        this.location = location.clone();
        this.item = location.getWorld().spawn(location, Item.class, entity -> {
            entity.setItemStack(this.itemStack);
            entity.setGravity(false);
            entity.setVelocity(entity.getVelocity().zero());
            entity.setPickupDelay(Integer.MAX_VALUE);
            entity.setCanMobPickup(false);
            entity.setInvulnerable(true);
            // Shop items do not despawn while loaded, but must not be saved into chunk
            // save files so chunk unloads and restarts do not leave orphan item entities.
            entity.setUnlimitedLifetime(true);
            entity.setPersistent(false);
            // Hidden by default; visibility is managed per player (see Hologram)
            entity.setVisibleByDefault(false);
            // Marker used to remove orphaned items (see ShopUpdateListener#onEntitiesLoad)
            entity.getPersistentDataContainer().set(plugin.getShopEntityKey(),
                    org.bukkit.persistence.PersistentDataType.BYTE, (byte) 1);
        });
    }

    public UUID getEntityUuid() {
        return item == null ? null : item.getUniqueId();
    }

    public boolean isValid() {
        return item != null && item.isValid();
    }

    public Location getLocation() {
        return location.clone();
    }

    public ItemStack getItemStack() {
        return itemStack.clone();
    }

    public boolean isVisible(Player player) {
        return viewers.contains(player.getUniqueId());
    }

    public void setVisible(Player player, boolean visible) {
        if (item == null || !item.isValid()) return;

        if (visible) {
            if (!item.getWorld().equals(player.getWorld())) return;
            if (!player.canSee(item)) {
                player.showEntity(plugin, item);
            }
        } else {
            if (player.canSee(item)) {
                player.hideEntity(plugin, item);
            }
        }
    }

    public void showPlayer(Player player) {
        showPlayer(player, false);
    }

    public void showPlayer(Player player, boolean force) {
        // Already shown: skip the per-entity canSee() lookups
        if (!viewers.add(player.getUniqueId()) && !force) return;
        setVisible(player, true);
    }

    public void hidePlayer(Player player) {
        hidePlayer(player, false);
    }

    public void hidePlayer(Player player, boolean force) {
        // Entities are hidden by default, so a player it was never shown to
        // needs no work. This used to run for every shop on every move.
        if (!viewers.remove(player.getUniqueId()) && !force) return;
        setVisible(player, false);
    }

    public void resetVisible(Player player) {
        if (viewers.remove(player.getUniqueId()) && player.isOnline()) {
            setVisible(player, false);
        }
    }

    public void remove() {
        viewers.clear();
        // Also remove invalid (e.g. chunk being unloaded) items; otherwise they come back on reload
        if (item != null && !item.isDead()) {
            item.remove();
        }
    }

    public void resetForPlayer(Player player) {
        hidePlayer(player, true);
        showPlayer(player, true);
    }
}
