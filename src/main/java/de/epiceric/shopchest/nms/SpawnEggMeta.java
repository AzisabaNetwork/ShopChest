package de.epiceric.shopchest.nms;

import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

/** Modern Bukkit spawn-egg metadata access. */
public class SpawnEggMeta {
    public static EntityType getEntityTypeFromItemStack(ItemStack stack) {
        if (stack.getItemMeta() instanceof org.bukkit.inventory.meta.SpawnEggMeta) {
            org.bukkit.inventory.meta.SpawnEggMeta meta = (org.bukkit.inventory.meta.SpawnEggMeta) stack.getItemMeta();
            if (meta.getCustomSpawnedType() != null) return meta.getCustomSpawnedType();
            return meta.getSpawnedEntity() == null ? null : meta.getSpawnedEntity().getEntityType();
        }
        return null;
    }
}
