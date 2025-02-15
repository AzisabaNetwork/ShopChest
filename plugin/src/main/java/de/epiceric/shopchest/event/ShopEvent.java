package de.epiceric.shopchest.event;

import de.epiceric.shopchest.shop.Shop;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public abstract class ShopEvent extends Event {

    private static final HandlerList handlers = new HandlerList();
    private final Shop shop;
    private final Player player;

    public ShopEvent(Player player, Shop shop) {
        this.player = player;
        this.shop = shop;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    /**
     * @return Shop which is involved in this event
     */
    public Shop getShop() {
        return shop;
    }

    /**
     * @return Player who is involved in this event
     */
    public Player getPlayer() {
        return player;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

}
