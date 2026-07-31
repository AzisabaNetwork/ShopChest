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
            entity.setPersistent(false);
        });

        for (Player player : location.getWorld().getPlayers()) {
            player.hideEntity(plugin, item);
        }
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

    public void showPlayer(Player player) {
        showPlayer(player, false);
    }

    public void showPlayer(Player player, boolean force) {
        if (viewers.add(player.getUniqueId()) || force) {
            player.showEntity(plugin, item);
        }
    }

    public void hidePlayer(Player player) {
        hidePlayer(player, false);
    }

    public void hidePlayer(Player player, boolean force) {
        if (viewers.remove(player.getUniqueId()) || force) {
            player.hideEntity(plugin, item);
        }
    }

    public void resetVisible(Player player) {
        viewers.remove(player.getUniqueId());
    }

    public void remove() {
        viewers.clear();
        item.remove();
    }

    public void resetForPlayer(Player player) {
        hidePlayer(player);
        showPlayer(player);
    }
}
