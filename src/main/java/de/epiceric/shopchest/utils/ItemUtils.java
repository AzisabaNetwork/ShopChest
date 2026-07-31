package de.epiceric.shopchest.utils;

import java.util.*;

import io.lumine.mythic.bukkit.MythicBukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;

public class ItemUtils {

    public static Map<Enchantment, Integer> getEnchantments(ItemStack itemStack) {
        if (itemStack.getItemMeta() instanceof EnchantmentStorageMeta) {
            EnchantmentStorageMeta esm = (EnchantmentStorageMeta) itemStack.getItemMeta();
            return esm.getStoredEnchants();
        } else {
            return itemStack.getEnchantments();
        }
    }

    public static PotionType getPotionEffect(ItemStack itemStack) {
        if (itemStack.getItemMeta() instanceof PotionMeta) {
            return ((PotionMeta) itemStack.getItemMeta()).getBasePotionType();
        }

        return null;
    }

    public static boolean isExtendedPotion(ItemStack itemStack) {
        if (itemStack.getItemMeta() instanceof PotionMeta) {
            PotionType potionType = ((PotionMeta) itemStack.getItemMeta()).getBasePotionType();
            return potionType != null && potionType.name().startsWith("LONG_");
        }

        return false;
    }

    public static boolean isBannerPattern(ItemStack itemStack) {
        return itemStack.getType().name().endsWith("BANNER_PATTERN");
    }

    public static boolean isAir(Material type) {
        return Arrays.asList("AIR", "CAVE_AIR", "VOID_AIR").contains(type.name());
    }

    /**
     * Get the {@link ItemStack} from a String
     * @param item Serialized ItemStack e.g. {@code "STONE"} or {@code "STONE:1"}
     * @return The de-serialized ItemStack or {@code null} if the serialized item is invalid
     */
    public static ItemStack getItemStack(String item) {
        if (item.trim().isEmpty()) return null;

        if (item.contains(":")) {
            Material mat = Material.getMaterial(item.split(":")[0]);
            if (mat == null) return null;
            return new ItemStack(mat, 1, Short.parseShort(item.split(":")[1]));
        } else {
            Material mat = Material.getMaterial(item);
            if (mat == null) return null;
            return new ItemStack(mat, 1);
        }
    }

    public static String getMythicType(ItemStack itemStack) {
        if(itemStack == null || itemStack.getType() == Material.AIR) return null;
        return MythicBukkit.inst().getItemManager().getMythicTypeFromItem(itemStack);
    }

    // From LifeCore
    public static String toString(ItemStack stack) {
        List<String> props = new ArrayList();
        props.add("[Type: " + stack.getType().name() + "]");
        props.add("[Amount: " + stack.getAmount() + "]");
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            if (meta.hasDisplayName()) {
                props.add("[Name: " + meta.getDisplayName() + "]");
            }

            if (meta.hasLore()) {
                props.add("[Lore: " + Objects.requireNonNull(meta.getLore()).size() + " entries]");
            }

            if (meta.hasCustomModelData()) {
                props.add("[CustomModelData: " + meta.getCustomModelData() + "]");
            }

            if (getMythicType(stack) != null) {
                props.add("[MMID: " + getMythicType(stack) + "]");
            }
        }

        return String.join("", props);
    }

}
