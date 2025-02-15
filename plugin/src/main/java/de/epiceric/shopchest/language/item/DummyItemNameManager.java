package de.epiceric.shopchest.language.item;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class DummyItemNameManager implements ItemNameManager {

    private final static String NOT_CONFIGURED_ITEM_NAME = "Not configured";

    @Override
    public @Nullable String getItemName(@Nullable ItemStack itemStack) {
        if (itemStack == null) {
            return null;
        }
        return NOT_CONFIGURED_ITEM_NAME;
    }


}