package com.oruuke.pillow.event;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class EffectRejoinEvent {
    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static void onPlayerReady(PlayerReadyEvent event) {
        Player player = event.getPlayer();

        final var ref = player.getReference();
        final var store = ref.getStore();
        final var world = store.getExternalData().getWorld();


        world.execute(() -> {
            final var playerComponent = store.getComponent(ref, Player.getComponentType());
            if (playerComponent == null) {
                LOGGER.atWarning().log("Player not in world to remove plushie effect!");
                return;
            }

            final var playerRefComponent = store.getComponent(ref, PlayerRef.getComponentType());
            assert playerRefComponent != null;

            final var effectControllerComponent = store.getComponent(ref, EffectControllerComponent.getComponentType());
            assert effectControllerComponent != null;

            final var activeEffects = effectControllerComponent.getActiveEffects();
            if (activeEffects.isEmpty()) return;
            final var plushieEffect = EntityEffect.getAssetMap().getIndex("Potion_Morph_Hex_Infinite");
            if (plushieEffect == 0) return;

            LOGGER.atInfo().log("Cleaning up leftover effect...");
            ComponentAccessor<EntityStore> componentAccessor = store;
            effectControllerComponent.removeEffect(ref, plushieEffect, componentAccessor);
            LOGGER.atInfo().log("Cleaning complete!");
        });
    }
}
