package de.epiceric.shopchest.utils.legacy;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;
import org.bukkit.scheduler.BukkitRunnable;

import de.epiceric.shopchest.ShopChest;
import de.epiceric.shopchest.shop.Shop;
import de.epiceric.shopchest.utils.Callback;
import de.epiceric.shopchest.utils.Utils;
import io.lumine.mythic.bukkit.MythicBukkit;

/**
 * Brings shop products saved on 1.15.2 / MythicMobs 4 up to date, the same way MythicItemIdCompat
 * does for inventories and chests (which it cannot do for ShopChest's database):
 * <ol>
 *     <li>A leftover MythicMobs 4 {@code MYTHIC_TYPE} tag becomes {@code mythicmobs:type}
 *         (MythicMobs 5 migrates it while reading the item's type).</li>
 *     <li>Products that lost the tag are matched against the old {@code Items} YAML in
 *         {@code plugins/MythicItemIdCompat/legacy-mythic-items/} (or SearchableInfiniteShop's copy).</li>
 *     <li>A missing CustomModelData or head skin is restored for products with an ID.</li>
 * </ol>
 * The product is never recreated; name, lore, enchantments and durability stay as they are.
 * MMIDs listed in MythicItemIdCompat's {@code excluded-mmids} are left alone.
 */
public class LegacyItemMigrator {
    private static final NamespacedKey TYPE = new NamespacedKey("mythicmobs", "type");
    private static final int SHOPS_PER_TICK = 50;

    private final ShopChest plugin;
    private boolean loaded;
    private LegacyMythicDefinitions definitions;
    private Set<String> excludedExact = Collections.emptySet();
    private List<String> excludedPrefixes = Collections.emptyList();
    private boolean databaseMigrationRunning;
    // Generated MythicMobs items by ID (empty when MythicMobs has no such ID); generating one per lookup is costly
    private final Map<String, java.util.Optional<ItemStack>> currentItems = new HashMap<>();

    public LegacyItemMigrator(ShopChest plugin) {
        this.plugin = plugin;
    }

    /** Reloads the legacy definitions and exclusions on next use. */
    public void reload() {
        loaded = false;
        definitions = null;
        currentItems.clear();
    }

    private void ensureLoaded() {
        if (loaded) return;
        loaded = true;

        File pluginsFolder = plugin.getDataFolder().getParentFile();
        File compatFolder = new File(pluginsFolder, "MythicItemIdCompat");

        loadExclusions(new File(compatFolder, "config.yml"));

        File directory = new File(compatFolder, "legacy-mythic-items");
        if (!directory.isDirectory()) {
            directory = new File(pluginsFolder, "SearchableInfiniteShop/legacy-mythic-items");
        }
        if (!directory.isDirectory()) {
            plugin.getLogger().info("Legacy MythicMobs Items not found (plugins/MythicItemIdCompat/legacy-mythic-items/);"
                    + " only products that still carry MYTHIC_TYPE will get their MMID back");
            return;
        }
        try {
            definitions = LegacyMythicDefinitions.load(directory, this::currentItem);
            plugin.getLogger().info("Legacy MythicMobs item definitions loaded: " + definitions.summary());
        } catch (IOException | RuntimeException e) {
            plugin.getLogger().warning("Could not load legacy MythicMobs item definitions: " + e.getMessage());
        }
    }

    private void loadExclusions(File compatConfig) {
        Set<String> exact = new HashSet<>();
        List<String> prefixes = new ArrayList<>();
        if (compatConfig.isFile()) {
            for (String raw : YamlConfiguration.loadConfiguration(compatConfig).getStringList("excluded-mmids")) {
                if (raw == null) continue;
                String entry = raw.trim().toLowerCase(Locale.ROOT);
                if (entry.isEmpty()) continue;
                if (entry.endsWith("*")) {
                    if (entry.length() > 1) prefixes.add(entry.substring(0, entry.length() - 1));
                } else {
                    exact.add(entry);
                }
            }
        }
        excludedExact = exact;
        excludedPrefixes = prefixes;
    }

    private boolean isExcluded(String id) {
        String lower = id.trim().toLowerCase(Locale.ROOT);
        if (excludedExact.contains(lower)) return true;
        for (String prefix : excludedPrefixes) if (lower.startsWith(prefix)) return true;
        return false;
    }

    private ItemStack currentItem(String id) {
        return currentItems.computeIfAbsent(id, key -> {
            try {
                return java.util.Optional.ofNullable(MythicBukkit.inst().getItemManager().getItemStack(key));
            } catch (RuntimeException e) {
                return java.util.Optional.empty();
            }
        }).map(ItemStack::clone).orElse(null);
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    /**
     * Migrates the given item in place. Must be called on the server thread.
     *
     * @return Whether the item was changed
     */
    public boolean migrate(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return false;
        ensureLoaded();

        try {
            ItemMeta meta = item.getItemMeta();
            String id = meta.getPersistentDataContainer().get(TYPE, PersistentDataType.STRING);
            boolean changed = false;
            boolean setId = false;

            if (isBlank(id)) {
                // MythicMobs moves MYTHIC_TYPE into mythicmobs:type on the item it reads: read a copy
                // and keep the result only if the ID is not excluded
                ItemStack copy = item.clone();
                String legacy = MythicBukkit.inst().getItemManager().getMythicTypeFromItem(copy);
                if (!isBlank(legacy)) {
                    if (isExcluded(legacy)) return false;
                    ItemMeta copyMeta = copy.getItemMeta();
                    if (isBlank(copyMeta.getPersistentDataContainer().get(TYPE, PersistentDataType.STRING))) {
                        copyMeta.getPersistentDataContainer().set(TYPE, PersistentDataType.STRING, legacy);
                    }
                    item.setItemMeta(copyMeta);
                    meta = item.getItemMeta();
                    id = legacy;
                    changed = true;
                } else if (definitions != null) {
                    id = definitions.match(item);
                    if (id == null) return false;
                    setId = true;
                } else {
                    return false;
                }
            }

            if (isExcluded(id)) return changed;

            ItemStack current = currentItem(id);
            if (current == null) {
                // Not (or no longer) defined in MythicMobs: only keep what the old tag told us
                return changed;
            }

            Integer model = null;
            if (!meta.hasCustomModelData()) {
                ItemMeta standard = current.getItemMeta();
                if (standard != null && standard.hasCustomModelData()) model = standard.getCustomModelData();
                if (model == null && definitions != null) model = definitions.modelFor(id);
            }

            boolean restoreSkin = LegacyMythicDefinitions.isSkinlessHead(item) && current.getType() == Material.PLAYER_HEAD
                    && ((current.getItemMeta() instanceof SkullMeta && !LegacyMythicDefinitions.isSkinlessHead(current))
                        || (definitions != null && definitions.headSkinFor(id) != null));

            if (!setId && model == null && !restoreSkin) return changed;

            if (setId) meta.getPersistentDataContainer().set(TYPE, PersistentDataType.STRING, id);
            if (model != null) meta.setCustomModelData(model);
            if (restoreSkin) restoreHeadProfile((SkullMeta) meta, current, definitions == null ? null : definitions.headSkinFor(id), id);
            item.setItemMeta(meta);
            return true;
        } catch (RuntimeException e) {
            plugin.debug("Failed to migrate legacy item " + item.getType() + ": " + e);
            plugin.debug(e);
            return false;
        }
    }

    private static void restoreHeadProfile(SkullMeta target, ItemStack current, String oldSkinUrl, String id) {
        PlayerProfile profile = null;
        if (current.getItemMeta() instanceof SkullMeta && !LegacyMythicDefinitions.isSkinlessHead(current)) {
            profile = ((SkullMeta) current.getItemMeta()).getOwnerProfile();
        }
        if (profile == null && oldSkinUrl != null) {
            try {
                profile = Bukkit.createPlayerProfile(UUID.nameUUIDFromBytes(id.getBytes(StandardCharsets.UTF_8)));
                PlayerTextures textures = profile.getTextures();
                textures.setSkin(new URL(oldSkinUrl));
                profile.setTextures(textures);
            } catch (MalformedURLException | IllegalArgumentException ignored) {
                return;
            }
        }
        if (profile != null) target.setOwnerProfile(profile);
    }

    /**
     * Migrates every shop in the database, including shops in chunks that are not loaded,
     * a few shops per tick.
     */
    public void migrateDatabase() {
        if (databaseMigrationRunning) return;
        databaseMigrationRunning = true;

        plugin.getShopDatabase().getAllShops(new Callback<Collection<Shop>>(plugin) {
            @Override
            public void onResult(Collection<Shop> shops) {
                final Iterator<Shop> iterator = new ArrayList<>(shops).iterator();
                final Map<Integer, String> changed = new HashMap<>();
                final int total = shops.size();

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        for (int i = 0; i < SHOPS_PER_TICK && iterator.hasNext(); i++) {
                            Shop shop = iterator.next();
                            if (migrate(shop.getProduct().getItemStack())) {
                                changed.put(shop.getID(), Utils.encode(shop.getProduct().getItemStack()));
                            }
                        }
                        if (iterator.hasNext()) return;

                        cancel();
                        databaseMigrationRunning = false;
                        plugin.getShopDatabase().updateProducts(changed);
                        if (!changed.isEmpty()) {
                            plugin.getLogger().info("Migrated the legacy MythicMobs products of " + changed.size()
                                    + " of " + total + " shops");
                        }
                    }
                }.runTaskTimer(plugin, 1L, 1L);
            }

            @Override
            public void onError(Throwable throwable) {
                databaseMigrationRunning = false;
                plugin.getLogger().warning("Could not read shops for the legacy item migration");
            }
        });
    }
}
