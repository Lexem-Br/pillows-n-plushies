package com.oruuke.pillow.builtin;

import com.hypixel.hytale.component.ComponentRegistryProxy;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.oruuke.pillow.builtin.system.CastSpellLimiterSystem;

public class HexcodeBuiltin {
    public static HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static void setup(JavaPlugin plugin) {
        ComponentRegistryProxy<EntityStore> entityStoreRegistry = plugin.getEntityStoreRegistry();
        entityStoreRegistry.registerSystem(new CastSpellLimiterSystem());
        LOGGER.atInfo().log("registered spell limiter");
    }
}
