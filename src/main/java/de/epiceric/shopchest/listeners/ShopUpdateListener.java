package de.epiceric.shopchest.listeners;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.scheduler.BukkitRunnable;

import de.epiceric.shopchest.ShopChest;
import de.epiceric.shopchest.shop.Shop;
import de.epiceric.shopchest.utils.Callback;

public class ShopUpdateListener implements Listener {

    private final ShopChest plugin;
    private final Set<Chunk> newLoadedChunks = new HashSet<>();

    public ShopUpdateListener(ShopChest plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryUpdate(InventoryMoveItemEvent e) {
        if (e.getSource().getType() != InventoryType.CHEST && e.getDestination().getType() != InventoryType.CHEST) return;

        Location loc = null;

        if (e.getSource().getHolder(false) instanceof Chest) {
            loc =  ((Chest) e.getSource().getHolder(false)).getLocation();
        } else if (e.getSource().getHolder(false) instanceof DoubleChest) {
            loc =  ((DoubleChest) e.getSource().getHolder(false)).getLocation();
        } else if (e.getDestination().getHolder(false) instanceof Chest) {
            loc =  ((Chest) e.getDestination().getHolder(false)).getLocation();
        } else if (e.getDestination().getHolder(false) instanceof DoubleChest) {
            loc =  ((DoubleChest) e.getDestination().getHolder(false)).getLocation();
        }

        if (loc != null) {
            Shop shop = plugin.getShopUtils().getShop(loc);
            if (shop != null && plugin.getHologramFormat().isDynamic()) {
                // Hoppers move items every few ticks; coalesce into one update per tick
                plugin.getUpdater().queueHologramUpdate(shop);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent e) {
        final Player p = e.getPlayer();
        plugin.getShopUtils().resetPlayerLocation(p);
        new BukkitRunnable() {
            @Override
            public void run() {
                if (p.isOnline()) {
                    plugin.getShopUtils().updateShops(p, true);
                }
            }
        }.runTaskLater(plugin, 1L);
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent e) {
        // If done without delay, Bukkit#getOnlinePlayers() would still
        // contain the player even though he left, so the shop updater
        // would show the shop again.
        new BukkitRunnable(){
            @Override
            public void run() {
                // Only the shops shown to this player need to be reset
                plugin.getShopUtils().forgetPlayer(e.getPlayer());
            }
        }.runTaskLater(plugin, 1L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerChangedWorld(PlayerChangedWorldEvent e) {
        final Player p = e.getPlayer();
        plugin.getShopUtils().forgetPlayer(p);
        new BukkitRunnable() {
            @Override
            public void run() {
                if (p.isOnline()) {
                    plugin.getShopUtils().updateShops(p, true);
                }
            }
        }.runTaskLater(plugin, 1L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawn(PlayerRespawnEvent e) {
        final Player p = e.getPlayer();
        plugin.getShopUtils().forgetPlayer(p);
        new BukkitRunnable() {
            @Override
            public void run() {
                if (p.isOnline()) {
                    plugin.getShopUtils().updateShops(p, true);
                }
            }
        }.runTaskLater(plugin, 1L);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent e) {
        Location from = e.getFrom();
        Location to = e.getTo();
        if (to == null) return;
        final Player p = e.getPlayer();

        // Compare block coordinates; getChunk() may load the target chunk synchronously
        if (!from.getWorld().equals(to.getWorld())
                || (from.getBlockX() >> 4) != (to.getBlockX() >> 4)
                || (from.getBlockZ() >> 4) != (to.getBlockZ() >> 4)) {
            plugin.getShopUtils().resetPlayerLocation(p);
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (p.isOnline()) {
                        plugin.getShopUtils().updateShops(p, true);
                    }
                }
            }.runTaskLater(plugin, 5L);
        } else {
            // Same chunk (e.g. rotation only): no PlayerMoveEvent follows, so update next tick
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (p.isOnline()) plugin.getShopUtils().updateShops(p, true);
            });
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent e) {
        // TextDisplay/Item visibility uses Bukkit entities. Updating on
        // the move event avoids delay when walking across shops.
        plugin.getUpdater().updateShops(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChunkUnload(ChunkUnloadEvent e) {
        for (Shop shop : getShopsInChunk(e.getChunk())) {
            shop.removeHologram();
            shop.removeItem();
        }
    }

    /**
     * Looks up the shops whose main location lies in the given chunk. Only the
     * chunk's chests are checked, instead of every loaded shop on the server.
     */
    private Collection<Shop> getShopsInChunk(Chunk chunk) {
        // Uses the chunk index of ShopUtils instead of scanning the chunk's tile entities
        return plugin.getShopUtils().getShopsInChunk(chunk);
    }

    /**
     * Removes shop entities that no loaded shop owns any more, e.g. entities that
     * were in an unloading chunk and came back with it, or leftovers of a crash.
     */
    @EventHandler
    public void onEntitiesLoad(org.bukkit.event.world.EntitiesLoadEvent e) {
        org.bukkit.NamespacedKey key = plugin.getShopEntityKey();
        java.util.Set<java.util.UUID> owned = null;
        for (org.bukkit.entity.Entity entity : e.getEntities()) {
            if (!entity.getPersistentDataContainer().has(key, org.bukkit.persistence.PersistentDataType.BYTE)) continue;
            if (owned == null) {
                owned = new java.util.HashSet<>();
                for (Shop shop : getShopsInChunk(e.getChunk())) {
                    if (shop.hasHologram()) {
                        for (de.epiceric.shopchest.nms.ArmorStandWrapper w : shop.getHologram().getArmorStandWrappers()) {
                            owned.add(w.getUuid());
                        }
                    }
                    if (shop.hasItem() && shop.getItem().getEntityUuid() != null) owned.add(shop.getItem().getEntityUuid());
                }
            }
            if (!owned.contains(entity.getUniqueId())) entity.remove();
        }
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent e) {
        Chunk chunk = e.getChunk();

        // Restore entities for shops that are already loaded in memory
        for (Shop shop : getShopsInChunk(chunk)) {
            shop.checkOrRecreateEntities();
        }

        if (!plugin.getShopDatabase().isInitialized()) {
            return;
        }

        // Wait 10 ticks after first event is triggered, so that multiple
        // chunk loads can be handled at the same time without having to
        // send a database request for each chunk.
        if (newLoadedChunks.isEmpty()) {
            new BukkitRunnable(){
                @Override
                public void run() {
                    int chunkCount = newLoadedChunks.size();
                    plugin.getShopUtils().loadShops(newLoadedChunks.toArray(new Chunk[chunkCount]), new Callback<Integer>(plugin) {
                        @Override
                        public void onResult(Integer result) {
                            if (result == 0) {
                                return;
                            }
                            plugin.debug("Loaded " + result + " shops in " + chunkCount + " chunks");
                        }

                        @Override
                        public void onError(Throwable throwable) {
                            // Database connection probably failed => disable plugin to prevent more errors
                            plugin.getLogger().severe("Failed to load shops in newly loaded chunks");
                            plugin.debug("Failed to load shops in newly loaded chunks");
                            if (throwable != null) plugin.debug(throwable);
                        }
                    });
                    newLoadedChunks.clear();
                }
            }.runTaskLater(plugin, 10L);
        }

        newLoadedChunks.add(e.getChunk());
    }
}
