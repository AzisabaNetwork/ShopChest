package de.epiceric.shopchest.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * @deprecated Use {@link ShopsLoadedEvent} instead since shops are loaded
 * dynamically based on chunk loading
 */
@Deprecated
public class ShopInitializedEvent extends Event {

    private static final HandlerList handlers = new HandlerList();
    private final int amount;

    public ShopInitializedEvent(int amount) {
        this.amount = amount;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public int getAmount() {
        return amount;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
}
