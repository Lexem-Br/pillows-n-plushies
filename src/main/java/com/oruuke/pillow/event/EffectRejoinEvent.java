package com.oruuke.pillow.event;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.modules.entity.component.EntityScaleComponent;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.oruuke.pillow.interactions.ApplyEffectRandomModelInteraction;

import javax.annotation.Nonnull;

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
                LOGGER.atWarning().log("Player not in world to remove leftover effect!");
                return;
            }

            final var playerRefComponent = store.getComponent(ref, PlayerRef.getComponentType());
            if (playerRefComponent == null) return;

            final var effectControllerComponent = store.getComponent(ref, EffectControllerComponent.getComponentType());
            if (effectControllerComponent == null) return;

            final var activeEffects = effectControllerComponent.getActiveEffects();
            if (activeEffects.isEmpty()) return;
            final var plushieEffect = EntityEffect.getAssetMap().getIndex("Potion_Morph_Hex_Infinite");
            if (plushieEffect == 0) return;

            LOGGER.atInfo().log("Cleaning up leftover effect...");

            effectControllerComponent.removeEffect(ref, plushieEffect, store);
            ModelComponent modelComponent = store.getComponent(ref, ModelComponent.getComponentType());
            store.putComponent(ref, ModelComponent.getComponentType(), new ModelComponent(modelComponent.getModel()));
            store.putComponent(ref, EntityScaleComponent.getComponentType(), new EntityScaleComponent(1.0F));

            LOGGER.atInfo().log("Cleaning complete!");
        });
    }
}
