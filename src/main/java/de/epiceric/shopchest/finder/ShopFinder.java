package de.epiceric.shopchest.finder;

import java.io.File;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.block.ShulkerBox;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;

import de.epiceric.shopchest.ShopChest;
import de.epiceric.shopchest.utils.Permissions;

/**
 * Shop search GUI ({@code /findshop}), formerly the separate ShopChestFinder plugin.
 */
public class ShopFinder {
    static final String CONFIG_SECTION = "shop-finder";
    static final List<String> MATERIAL_LISTS = List.of("group-materials", "exclude-materials", "exclude-vanilla-materials");

    private final ShopChest plugin;
    private final FinderPlayerSettings playerSettings;
    private final FinderCache cache;
    private final FinderMenu menu;

    private boolean enabled;
    private long refreshIntervalTicks;
    private volatile double ignoredPriceThreshold;
    // Replaced as a whole on reload; read from the async cache rebuild
    private volatile Set<Material> groupMaterials = EnumSet.noneOf(Material.class);
    private volatile Set<Material> excludeMaterials = EnumSet.noneOf(Material.class);
    private volatile Set<Material> excludeVanillaMaterials = EnumSet.noneOf(Material.class);

    public ShopFinder(ShopChest plugin) {
        this.plugin = plugin;
        migrateLegacyConfig();
        this.playerSettings = new FinderPlayerSettings(plugin);
        this.cache = new FinderCache(plugin, this);
        this.menu = new FinderMenu(plugin, this);
        loadSettings();
    }

    /**
     * Registers the command and listener and starts refreshing the shop list.
     */
    public void enable() {
        PluginCommand command = plugin.getCommand("findshop");
        if (command != null) {
            FinderCommand executor = new FinderCommand(this);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        }
        plugin.getServer().getPluginManager().registerEvents(menu, plugin);
        cache.start(refreshIntervalTicks);
    }

    public void disable() {
        cache.stop();
        playerSettings.saveNow();
    }

    /**
     * Re-reads the finder section of config.yml (called after {@code /shop reload}).
     */
    public void reload() {
        loadSettings();
        cache.stop();
        cache.start(refreshIntervalTicks);
    }

    private void loadSettings() {
        FileConfiguration config = plugin.getConfig();
        enabled = config.getBoolean(CONFIG_SECTION + ".enabled", true);
        refreshIntervalTicks = Math.max(5, config.getLong(CONFIG_SECTION + ".refresh-interval", 60)) * 20L;
        ignoredPriceThreshold = config.getDouble(CONFIG_SECTION + ".ignore-price-at-or-below", 1);
        groupMaterials = readMaterials("group-materials");
        excludeMaterials = readMaterials("exclude-materials");
        excludeVanillaMaterials = readMaterials("exclude-vanilla-materials");
    }

    private Set<Material> readMaterials(String list) {
        Set<Material> result = EnumSet.noneOf(Material.class);
        for (String name : plugin.getConfig().getStringList(CONFIG_SECTION + "." + list)) {
            Material material = Material.matchMaterial(name);
            if (material == null) {
                plugin.getLogger().warning("Invalid material in " + CONFIG_SECTION + "." + list + ": " + name);
            } else {
                result.add(material);
            }
        }
        return result;
    }

    /**
     * Copies the lists and player settings of the old ShopChestFinder plugin
     * the first time the integrated finder runs.
     */
    private void migrateLegacyConfig() {
        File legacyFolder = new File(plugin.getDataFolder().getParentFile(), "ShopChestFinder");
        File legacyConfigFile = new File(legacyFolder, "config.yml");

        if (!plugin.getConfig().isSet(CONFIG_SECTION) && legacyConfigFile.isFile()) {
            YamlConfiguration legacyConfig = YamlConfiguration.loadConfiguration(legacyConfigFile);
            plugin.getConfig().set(CONFIG_SECTION + ".enabled", true);
            plugin.getConfig().set(CONFIG_SECTION + ".refresh-interval", 60);
            for (String list : MATERIAL_LISTS) {
                plugin.getConfig().set(CONFIG_SECTION + "." + list, legacyConfig.getStringList(list));
            }
            plugin.saveConfig();
            plugin.getLogger().info("Imported settings from plugins/ShopChestFinder/config.yml");
        }

        FinderPlayerSettings.migrateLegacyData(plugin, new File(legacyFolder, "data.yml"));
    }

    /**
     * Saves a material list edited by {@code /findshop config} and applies it.
     */
    void setMaterialList(String list, List<String> materials) {
        plugin.getConfig().set(CONFIG_SECTION + "." + list, materials);
        plugin.saveConfig();
        loadSettings();
        cache.refresh();
    }

    List<String> getMaterialList(String list) {
        return plugin.getConfig().getStringList(CONFIG_SECTION + "." + list);
    }

    /**
     * @return Prices at or below this value are treated as disabled (display shops)
     */
    double getIgnoredPriceThreshold() {
        return ignoredPriceThreshold;
    }

    public boolean isEnabled() {
        return enabled;
    }

    FinderPlayerSettings getPlayerSettings() {
        return playerSettings;
    }

    FinderCache getCache() {
        return cache;
    }

    FinderMenu getMenu() {
        return menu;
    }

    static boolean isAdmin(Player player) {
        // "shopchestfinder.admin" is kept for servers migrating from the standalone plugin
        return player.hasPermission(Permissions.FINDER_ADMIN) || player.hasPermission("shopchestfinder.admin");
    }

    /**
     * @return Whether the product should be hidden from the finder
     */
    boolean isExcluded(ItemStack item) {
        if (item == null || item.getType().isAir()) return true;
        Material type = item.getType();

        if (excludeMaterials.contains(type)) return true;

        if (excludeVanillaMaterials.contains(type)) {
            if (!item.hasItemMeta()) return true;
            ItemMeta meta = item.getItemMeta();
            return !meta.hasDisplayName() && !meta.hasLore();
        }
        return false;
    }

    /**
     * Removes data that should not split otherwise equal products into
     * separate entries (shulker box contents, map IDs, or everything for
     * materials listed in {@code group-materials}).
     *
     * @return A copy of the item with an amount of 1
     */
    ItemStack stripUniqueData(ItemStack item) {
        if (groupMaterials.contains(item.getType())) {
            return new ItemStack(item.getType());
        }

        ItemStack clone = item.clone();
        clone.setAmount(1);

        if (clone.hasItemMeta() && clone.getItemMeta() instanceof BlockStateMeta bsm
                && bsm.hasBlockState() && bsm.getBlockState() instanceof ShulkerBox shulker) {
            shulker.getInventory().clear();
            bsm.setBlockState(shulker);
            clone.setItemMeta(bsm);
        }

        if (clone.getType() == Material.FILLED_MAP) {
            // Keep only what players can see: name, lore and enchantments
            ItemStack plainMap = new ItemStack(Material.FILLED_MAP);
            ItemMeta plainMeta = plainMap.getItemMeta();
            if (clone.hasItemMeta()) {
                ItemMeta oldMeta = clone.getItemMeta();
                if (oldMeta.hasDisplayName()) plainMeta.displayName(oldMeta.displayName());
                if (oldMeta.hasLore()) plainMeta.lore(oldMeta.lore());
                for (Map.Entry<Enchantment, Integer> entry : oldMeta.getEnchants().entrySet()) {
                    plainMeta.addEnchant(entry.getKey(), entry.getValue(), true);
                }
            }
            plainMap.setItemMeta(plainMeta);
            return plainMap;
        }

        return clone;
    }
}
