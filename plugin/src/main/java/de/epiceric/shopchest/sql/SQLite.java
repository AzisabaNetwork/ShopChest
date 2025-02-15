package de.epiceric.shopchest.sql;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import de.epiceric.shopchest.ShopChest;
import de.epiceric.shopchest.shop.Shop;
import de.epiceric.shopchest.utils.Callback;
import de.epiceric.shopchest.utils.Utils;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.sql.*;

public class SQLite extends Database {

    public SQLite(ShopChest plugin) {
        super(plugin);
    }

    @Override
    HikariDataSource getDataSource() {
        try {
            // Initialize driver class so HikariCP can find it
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            plugin.getLogger().severe("Failed to initialize SQLite driver");
            plugin.debug("Failed to initialize SQLite driver");
            plugin.debug(e);
            return null;
        }

        File folder = plugin.getDataFolder();
        File dbFile = new File(folder, "shops.db");

        if (!dbFile.exists()) {
            try {
                dbFile.createNewFile();
            } catch (IOException ex) {
                plugin.getLogger().severe("Failed to create database file");
                plugin.debug("Failed to create database file");
                plugin.debug(ex);
                return null;
            }
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(String.format("jdbc:sqlite:" + dbFile));
        config.setConnectionTestQuery("SELECT 1");
        config.setAutoCommit(true);

        return new HikariDataSource(config);
    }

    /**
     * Vacuums the database synchronously to reduce file size
     */
    public void vacuum() {
        try (Connection con = dataSource.getConnection();
             Statement s = con.createStatement()) {
            s.executeUpdate("VACUUM");

            plugin.debug("Vacuumed SQLite database");
        } catch (final SQLException ex) {
            plugin.getLogger().warning("Failed to vacuum database");
            plugin.debug("Failed to vacuum database");
            plugin.debug(ex);
        }
    }

    @Override
    String getQueryCreateTableShops() {
        return "CREATE TABLE IF NOT EXISTS " + tableShops + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "vendor TINYTEXT NOT NULL,"
                + "product TEXT NOT NULL,"
                + "amount INTEGER NOT NULL,"
                + "world TINYTEXT NOT NULL,"
                + "x INTEGER NOT NULL,"
                + "y INTEGER NOT NULL,"
                + "z INTEGER NOT NULL,"
                + "buyprice FLOAT NOT NULL,"
                + "sellprice FLOAT NOT NULL,"
                + "shoptype TINYTEXT NOT NULL)";
    }

    @Override
    String getQueryCreateTableLog() {
        return "CREATE TABLE IF NOT EXISTS " + tableLogs + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "shop_id INTEGER NOT NULL,"
                + "timestamp TINYTEXT NOT NULL,"
                + "time LONG NOT NULL,"
                + "player_name TINYTEXT NOT NULL,"
                + "player_uuid TINYTEXT NOT NULL,"
                + "product_name TINYTEXT NOT NULL,"
                + "product TEXT NOT NULL,"
                + "amount INTEGER NOT NULL,"
                + "vendor_name TINYTEXT NOT NULL,"
                + "vendor_uuid TINYTEXT NOT NULL,"
                + "admin BIT NOT NULL,"
                + "world TINYTEXT NOT NULL,"
                + "x INTEGER NOT NULL,"
                + "y INTEGER NOT NULL,"
                + "z INTEGER NOT NULL,"
                + "price FLOAT NOT NULL,"
                + "type TINYTEXT NOT NULL)";
    }

    @Override
    String getQueryCreateTableLogout() {
        return "CREATE TABLE IF NOT EXISTS " + tableLogouts + " ("
                + "player VARCHAR(36) PRIMARY KEY NOT NULL,"
                + "time LONG NOT NULL)";
    }

    @Override
    String getQueryCreateTableFields() {
        return "CREATE TABLE IF NOT EXISTS " + tableFields + " ("
                + "field VARCHAR(32) PRIMARY KEY NOT NULL,"
                + "value INTEGER NOT NULL)";
    }

    @Override
    String getQueryGetTable() {
        return "SELECT name FROM sqlite_master WHERE type='table' AND name=?";
    }

    @Override
    public void addShop(Shop shop, Callback<Integer> callback) {
        final String queryNoId = "REPLACE INTO " + tableShops + " (vendor,product,amount,world,x,y,z,buyprice,sellprice,shoptype) VALUES(?,?,?,?,?,?,?,?,?,?) RETURNING id";
        final String queryWithId = "REPLACE INTO " + tableShops + " (id,vendor,product,amount,world,x,y,z,buyprice,sellprice,shoptype) VALUES(?,?,?,?,?,?,?,?,?,?,?)";

        new BukkitRunnable() {
            @Override
            public void run() {
                String query = shop.hasId() ? queryWithId : queryNoId;

                try (Connection con = dataSource.getConnection();
                     PreparedStatement ps = con.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
                    int i = 0;
                    if (shop.hasId()) {
                        i = 1;
                        ps.setInt(1, shop.getID());
                    }

                    ps.setString(i + 1, shop.getVendor().getUniqueId().toString());
                    ps.setString(i + 2, Utils.encode(shop.getProduct().getItemStack()));
                    ps.setInt(i + 3, shop.getProduct().getAmount());
                    ps.setString(i + 4, shop.getLocation().getWorld().getName());
                    ps.setInt(i + 5, shop.getLocation().getBlockX());
                    ps.setInt(i + 6, shop.getLocation().getBlockY());
                    ps.setInt(i + 7, shop.getLocation().getBlockZ());
                    ps.setDouble(i + 8, shop.getBuyPrice());
                    ps.setDouble(i + 9, shop.getSellPrice());
                    ps.setString(i + 10, shop.getShopType().toString());

                    if (!shop.hasId()) {
                        int shopId = -1;
                        ResultSet rs = ps.executeQuery();
                        if (rs.next()) {
                            shopId = rs.getInt(1);
                        }

                        shop.setId(shopId);
                    } else {
                        ps.executeUpdate();
                    }

                    if (callback != null) {
                        callback.callSyncResult(shop.getID());
                    }

                    plugin.debug("Adding shop to database (#" + shop.getID() + ")");
                } catch (SQLException ex) {
                    if (callback != null) {
                        callback.callSyncError(ex);
                    }

                    plugin.getLogger().severe("Failed to add shop to database");
                    plugin.debug("Failed to add shop to database (#" + shop.getID() + ")");
                    plugin.debug(ex);
                }
            }
        }.runTaskAsynchronously(plugin);
    }
}
