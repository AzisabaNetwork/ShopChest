package de.epiceric.shopchest.shop;

import java.util.Collections;
import java.util.List;

import de.epiceric.shopchest.event.ShopBuySellEvent.Type;

/**
 * A single buy or sell transaction that has been logged in the economy log.
 */
public final class Transaction {
    private final long time;
    private final String playerName;
    private final String productName;
    private final int amount;
    private final String vendorName;
    private final boolean admin;
    private final String world;
    private final int x;
    private final int y;
    private final int z;
    private final double price;
    private final Type type;

    public Transaction(long time, String playerName, String productName, int amount, String vendorName,
            boolean admin, String world, int x, int y, int z, double price, Type type) {
        this.time = time;
        this.playerName = playerName;
        this.productName = productName;
        this.amount = amount;
        this.vendorName = vendorName;
        this.admin = admin;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.price = price;
        this.type = type;
    }

    /** @return Time of the transaction in milliseconds since the epoch */
    public long getTime() {
        return time;
    }

    /** @return Name of the player who bought or sold something */
    public String getPlayerName() {
        return playerName;
    }

    /** @return Localized name of the product at the time of the transaction */
    public String getProductName() {
        return productName;
    }

    public int getAmount() {
        return amount;
    }

    /** @return Name of the shop's vendor */
    public String getVendorName() {
        return vendorName;
    }

    public boolean isAdminShop() {
        return admin;
    }

    public String getWorld() {
        return world;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public double getPrice() {
        return price;
    }

    /** @return {@link Type#BUY} if the player bought from the shop, {@link Type#SELL} if the player sold to it */
    public Type getType() {
        return type;
    }

    /**
     * One page of a transaction history query.
     */
    public static final class Page {
        private final List<Transaction> transactions;
        private final int total;

        public Page(List<Transaction> transactions, int total) {
            this.transactions = Collections.unmodifiableList(transactions);
            this.total = total;
        }

        public List<Transaction> getTransactions() {
            return transactions;
        }

        /** @return Total number of transactions (across all pages) */
        public int getTotal() {
            return total;
        }
    }
}
