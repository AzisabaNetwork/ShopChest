package de.epiceric.shopchest.event;

import de.epiceric.shopchest.shop.Shop;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.Collection;

/**
 * Called when shops have been loaded and added to the server
 */
public class ShopsLoadedEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final Collection<Shop> shops;

    public ShopsLoadedEvent(Collection<Shop> shops) {
        this.shops = shops;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public Collection<Shop> getShops() {
        return shops;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
}
