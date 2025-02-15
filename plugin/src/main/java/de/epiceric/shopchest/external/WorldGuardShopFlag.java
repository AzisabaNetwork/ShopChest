package de.epiceric.shopchest.external;

import de.epiceric.shopchest.ShopChest;
import de.epiceric.shopchest.config.Config;
import org.codemc.worldguardwrapper.WorldGuardWrapper;
import org.codemc.worldguardwrapper.flag.IWrappedFlag;
import org.codemc.worldguardwrapper.flag.WrappedState;

import java.util.Optional;

public class WorldGuardShopFlag {

    public static void register(final ShopChest plugin) {
        WorldGuardWrapper wrapper = WorldGuardWrapper.getInstance();

        Optional<IWrappedFlag<WrappedState>> createFlag = wrapper.registerFlag("create-shop",
                WrappedState.class, Config.wgAllowCreateShopDefault ? WrappedState.ALLOW : WrappedState.DENY);

        Optional<IWrappedFlag<WrappedState>> useFlag = wrapper.registerFlag("use-shop",
                WrappedState.class, Config.wgAllowUseShopDefault ? WrappedState.ALLOW : WrappedState.DENY);

        Optional<IWrappedFlag<WrappedState>> useAdminFlag = wrapper.registerFlag("use-admin-shop",
                WrappedState.class, Config.wgAllowUseAdminShopDefault ? WrappedState.ALLOW : WrappedState.DENY);

        plugin.debug("Flag create-shop: " + createFlag.isPresent());
        plugin.debug("Flag use-shop: " + useFlag.isPresent());
        plugin.debug("Flag use-admin-shop: " + useAdminFlag.isPresent());
    }

}
