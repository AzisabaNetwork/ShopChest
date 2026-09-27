package de.epiceric.shopchest.external;

import java.util.Optional;

import org.codemc.worldguardwrapper.WorldGuardWrapper;
import org.codemc.worldguardwrapper.flag.IWrappedFlag;
import org.codemc.worldguardwrapper.flag.WrappedState;

import de.epiceric.shopchest.ShopChest;
import de.epiceric.shopchest.config.Config;

public class WorldGuardShopFlag {

    public static void register(final ShopChest plugin) {
        WorldGuardWrapper wrapper = WorldGuardWrapper.getInstance();

        try {
            Optional<IWrappedFlag<WrappedState>> createFlag = wrapper.registerFlag("create-shop",
                    WrappedState.class, Config.wgAllowCreateShopDefault ? WrappedState.ALLOW : WrappedState.DENY);
                    
            Optional<IWrappedFlag<WrappedState>> useFlag = wrapper.registerFlag("use-shop",
                    WrappedState.class, Config.wgAllowUseShopDefault ? WrappedState.ALLOW : WrappedState.DENY);
                    
            Optional<IWrappedFlag<WrappedState>> useAdminFlag = wrapper.registerFlag("use-admin-shop",
                    WrappedState.class, Config.wgAllowUseAdminShopDefault ? WrappedState.ALLOW : WrappedState.DENY);

            plugin.debug("Flag create-shop: " + String.valueOf(createFlag.isPresent()));
            plugin.debug("Flag use-shop: " + String.valueOf(useFlag.isPresent()));
            plugin.debug("Flag use-admin-shop: " + String.valueOf(useAdminFlag.isPresent()));
        } catch (IllegalStateException e) {
            plugin.getLogger().warning("WorldGuard custom flags could not be registered after server startup.");
            plugin.getLogger().warning("Restart the server to enable create-shop, use-shop, and use-admin-shop flags.");
            plugin.debug("WorldGuard custom flag registration is only available during server startup");
        }
    }

}
