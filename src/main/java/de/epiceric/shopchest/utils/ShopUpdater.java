package de.epiceric.shopchest.utils;

import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import de.epiceric.shopchest.ShopChest;

public class ShopUpdater {
    private static final int SHOPS_CREATED_PER_TICK = 2;

    private static class EntityCreation {
        private final World world;
        private final Runnable runnable;

        private EntityCreation(World world, Runnable runnable) {
            this.world = world;
            this.runnable = runnable;
        }
    }

    
    private final ShopChest plugin;
    private final Queue<EntityCreation> entityCreations = new ConcurrentLinkedQueue<>();
    private boolean running;
    private BukkitTask entityCreationTask;

    public ShopUpdater(ShopChest plugin) {
        this.plugin = plugin;
    }

    /**
     * Start task, except if it is already
     */
    public void start() {
        running = true;
        entityCreationTask = Bukkit.getScheduler().runTaskTimer(plugin, this::processEntityCreations, 1L, 1L);
    }

    /**
     * Stop any running task then start it again
     */
    public void restart() {
        stop();
        start();
    }

    /**
     * Stop task properly
     */
    public void stop() {
        running = false;
        if (entityCreationTask != null) {
            entityCreationTask.cancel();
            entityCreationTask = null;
        }
        entityCreations.clear();
    }

    /**
     * @return whether task is running or not
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * Queue a task to update shops for the given player
     * 
     * @param player Player to show updates
     */
    public void updateShops(Player player) {
        queue(() -> plugin.getShopUtils().updateShops(player));
    }

    /**
     * Queue a task to update shops for players in the given world
     * 
     * @param world World in whose players to show updates
     */
    public void updateShops(World world) {
        queue(() -> {
            for (Player player : world.getPlayers()) {
                plugin.getShopUtils().updateShops(player);
            }
        });
    }

    /**
     * Queue a task to update shops for all players
     */
    public void updateShops() {
        queue(() -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                plugin.getShopUtils().updateShops(player);
            }
        });
    }

    /**
     * Register a task to run before next loop
     *
     * @param runnable task to run
     */
    public void queue(Runnable runnable) {
        if (Bukkit.isPrimaryThread()) {
            runnable.run();
        } else {
            Bukkit.getScheduler().runTask(plugin, runnable);
        }
    }

    /**
     * Queue Bukkit entity creation so loading many shops does not stall one
     * server tick. The supplied runnable is always invoked on the main thread.
     */
    public void queueEntityCreation(World world, Runnable runnable) {
        entityCreations.add(new EntityCreation(world, runnable));
    }

    private void processEntityCreations() {
        Set<World> updatedWorlds = new HashSet<>();

        for (int i = 0; i < SHOPS_CREATED_PER_TICK; i++) {
            EntityCreation creation = entityCreations.poll();
            if (creation == null) {
                break;
            }

            creation.runnable.run();
            updatedWorlds.add(creation.world);
        }

        for (World world : updatedWorlds) {
            for (Player player : world.getPlayers()) {
                plugin.getShopUtils().resetPlayerLocation(player);
                plugin.getShopUtils().updateShops(player);
            }
        }
    }
}
