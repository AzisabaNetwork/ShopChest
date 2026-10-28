package de.epiceric.shopchest.finder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;

import de.epiceric.shopchest.ShopChest;
import de.epiceric.shopchest.language.LanguageUtils;
import de.epiceric.shopchest.shop.Shop;
import de.epiceric.shopchest.shop.Shop.ShopType;
import de.epiceric.shopchest.utils.Callback;

/**
 * Periodically rebuilt snapshot of all shops, grouped by product.
 * <p>
 * The snapshot is read from the database, so shops in unloaded chunks are
 * listed as well. Visibility (public/private vendors) is applied when a menu
 * is opened, so changing a setting takes effect immediately.
 */
class FinderCache {

    /** Immutable copy of the shop data the finder needs. */
    record FinderShop(int id, UUID vendor, String vendorName, boolean admin,
                      String world, int x, int y, int z, double buyPrice, double sellPrice, int amount) {

        /** Buy price per single item, or 0 if buying is disabled. */
        double unitBuyPrice() {
            return buyPrice > 0 ? buyPrice / amount : 0;
        }

        /** Sell price per single item, or 0 if selling is disabled. */
        double unitSellPrice() {
            return sellPrice > 0 ? sellPrice / amount : 0;
        }
    }

    /** All shops selling or buying the same (stripped) product. */
    record ItemGroup(ItemStack icon, String searchText, List<FinderShop> shops) {
    }

    private final ShopChest plugin;
    private final ShopFinder finder;
    private final AtomicBoolean refreshing = new AtomicBoolean();

    private volatile List<ItemGroup> groups = Collections.emptyList();
    private BukkitTask task;

    FinderCache(ShopChest plugin, ShopFinder finder) {
        this.plugin = plugin;
        this.finder = finder;
    }

    void start(long intervalTicks) {
        // Short initial delay: the database connects asynchronously on startup
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::refresh, 100L, intervalTicks);
    }

    void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    List<ItemGroup> getGroups() {
        return groups;
    }

    /**
     * Rebuilds the snapshot in the background. Overlapping refreshes are skipped.
     */
    void refresh() {
        if (plugin.getShopDatabase() == null || !plugin.getShopDatabase().isInitialized()) return;
        if (!refreshing.compareAndSet(false, true)) return;

        plugin.getShopDatabase().getAllShops(new Callback<Collection<Shop>>(plugin) {
            @Override
            public void onResult(Collection<Shop> shops) {
                // Grouping clones and compares item metas; keep it off the main thread
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    try {
                        groups = buildGroups(shops);
                    } catch (RuntimeException e) {
                        plugin.getLogger().severe("Failed to build shop finder list: " + e);
                        plugin.debug(e);
                    } finally {
                        refreshing.set(false);
                    }
                });
            }

            @Override
            public void onError(Throwable throwable) {
                refreshing.set(false);
            }
        });
    }

    private List<ItemGroup> buildGroups(Collection<Shop> shops) {
        // Stripped products all have an amount of 1, so ItemStack#equals/hashCode
        // match isSimilar() and grouping is a hash lookup instead of a linear scan
        Map<ItemStack, List<FinderShop>> byProduct = new LinkedHashMap<>();

        for (Shop shop : shops) {
            ItemStack product = shop.getProduct().getItemStack();
            if (finder.isExcluded(product)) continue;

            Location loc = shop.getLocation();
            String worldName = loc.getWorld() == null ? null : loc.getWorld().getName();
            if (worldName == null) continue; // World is not loaded, the shop can't be visited

            // Display shops use a token price (e.g. 1) and would distort the
            // statistics; treat such prices as disabled
            double ignoredPrice = finder.getIgnoredPriceThreshold();
            double buyPrice = shop.getBuyPrice() > ignoredPrice ? shop.getBuyPrice() : 0;
            double sellPrice = shop.getSellPrice() > ignoredPrice ? shop.getSellPrice() : 0;
            int amount = shop.getProduct().getAmount();
            if ((buyPrice <= 0 && sellPrice <= 0) || amount <= 0) continue;

            String vendorName = shop.getVendor().getName();
            FinderShop entry = new FinderShop(shop.getID(), shop.getVendor().getUniqueId(),
                    vendorName == null ? shop.getVendor().getUniqueId().toString() : vendorName,
                    shop.getShopType() == ShopType.ADMIN, worldName,
                    loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(),
                    buyPrice, sellPrice, amount);

            byProduct.computeIfAbsent(finder.stripUniqueData(product), k -> new ArrayList<>()).add(entry);
        }

        List<ItemGroup> result = new ArrayList<>(byProduct.size());
        for (Map.Entry<ItemStack, List<FinderShop>> e : byProduct.entrySet()) {
            ItemStack icon = e.getKey();
            result.add(new ItemGroup(icon, buildSearchText(icon), List.copyOf(e.getValue())));
        }
        result.sort(Comparator.comparing(g -> g.icon().getType().name()));
        return List.copyOf(result);
    }

    /**
     * Text matched by {@code /findshop search}: material name, custom name and
     * the localized item name from the language file (e.g. Japanese names).
     */
    private static String buildSearchText(ItemStack item) {
        StringBuilder sb = new StringBuilder();
        String material = item.getType().name().toLowerCase(Locale.ROOT);
        sb.append(material).append('\n').append(material.replace('_', ' '));

        if (item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (meta.hasDisplayName()) {
                sb.append('\n').append(stripColors(meta.getDisplayName()));
            }
        }

        try {
            String localized = LanguageUtils.getItemName(item);
            if (localized != null) {
                sb.append('\n').append(stripColors(localized));
            }
        } catch (RuntimeException ignored) {
            // e.g. a player head without a resolvable owner; the other names still match
        }
        return sb.toString().toLowerCase(Locale.ROOT);
    }

    // "&a" style codes that were stored literally, including "&#rrggbb" hex colors
    private static final Pattern AMPERSAND_CODES = Pattern.compile("(?i)&(#[0-9a-f]{6}|[0-9a-fk-orx])");

    /**
     * Removes section sign and ampersand color codes, so a colored (or
     * gradient, where every character has its own code) name still contains
     * the plain text players search for.
     */
    static String stripColors(String text) {
        String stripped = ChatColor.stripColor(text);
        return stripped == null ? "" : AMPERSAND_CODES.matcher(stripped).replaceAll("");
    }
}
