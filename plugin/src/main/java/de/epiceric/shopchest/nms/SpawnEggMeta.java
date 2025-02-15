package de.epiceric.shopchest.nms;

import de.epiceric.shopchest.ShopChest;
import de.epiceric.shopchest.utils.Utils;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.inventivetalent.reflection.resolver.minecraft.OBCClassResolver;

import java.lang.reflect.InvocationTargetException;

public class SpawnEggMeta {

    private final static EntityType MOOSHROOM;

    static {
        // The constant name changed in 1.20.5
        // Doing this ensure compatibility with older version when using reflection nms module
        EntityType MOOSHROOM_ENTITY_TYPE;
        try {
            MOOSHROOM_ENTITY_TYPE = EntityType.valueOf("MUSHROOM_COW");
        } catch (IllegalArgumentException e) {
            MOOSHROOM_ENTITY_TYPE = EntityType.MOOSHROOM;
        }
        MOOSHROOM = MOOSHROOM_ENTITY_TYPE;
    }

    private static String getNBTEntityID(ShopChest plugin, ItemStack stack) {
        try {
            OBCClassResolver obcClassResolver = new OBCClassResolver();
            Class<?> craftItemStackClass = obcClassResolver.resolveSilent("inventory.CraftItemStack");

            if (craftItemStackClass == null) {
                plugin.debug("Failed to get NBTEntityID: Could not find CraftItemStack class");
                return null;
            }

            Object nmsStack = craftItemStackClass.getMethod("asNMSCopy", ItemStack.class).invoke(null, stack);

            Object nbtTagCompound = nmsStack.getClass().getMethod("getTag").invoke(nmsStack);
            if (nbtTagCompound == null) return null;

            Object entityTagCompound = nbtTagCompound.getClass().getMethod("getCompound", String.class).invoke(nbtTagCompound, "EntityTag");
            if (entityTagCompound == null) return null;

            Object id = entityTagCompound.getClass().getMethod("getString", String.class).invoke(entityTagCompound, "id");
            if (id instanceof String) return (String) id;

        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            plugin.getLogger().severe("Failed to get NBTEntityID with reflection");
            plugin.debug("Failed to get NBTEntityID with reflection");
            plugin.debug(e);
        }

        return null;
    }

    /**
     * @param plugin An instance of the {@link ShopChest} plugin
     * @param stack  {@link ItemStack} (Spawn Egg) of which the Entity should be gotten
     * @return The {@link EntityType} the Spawn Egg will spawn or <b>null</b> if <i>nbtEntityID</i> is null
     */
    public static EntityType getEntityTypeFromItemStack(ShopChest plugin, ItemStack stack) {
        if (Utils.getMajorVersion() == 8) {
            EntityType type = null;

            for (EntityType entityType : EntityType.values()) {
                if (entityType.getTypeId() == stack.getDurability()) {
                    type = entityType;
                    break;
                }
            }

            return type;
        }

        String nbtEntityID = getNBTEntityID(plugin, stack);

        if (nbtEntityID == null) return null;

        if (Utils.getMajorVersion() >= 11) {
            if (nbtEntityID.contains(":")) nbtEntityID = nbtEntityID.split(":")[1];
            return EntityType.fromName(nbtEntityID);
        }

        switch (nbtEntityID) {
            case "PigZombie":
                return EntityType.valueOf("PIG_ZOMBIE");
            case "CaveSpider":
                return EntityType.CAVE_SPIDER;
            case "LavaSlime":
                return EntityType.MAGMA_CUBE;
            case "MushroomCow":
                return MOOSHROOM;
            case "EntityHorse":
                return EntityType.HORSE;
            case "PolarBear":
                return EntityType.POLAR_BEAR;
            case "Ozelot":
                return EntityType.OCELOT;
            default:
                return EntityType.valueOf(nbtEntityID.toUpperCase());

        }
    }

}