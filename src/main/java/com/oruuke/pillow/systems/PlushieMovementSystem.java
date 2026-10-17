package com.oruuke.pillow.systems;

import com.hypixel.hytale.assetstore.map.AssetMapWithIndexes;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.entity.entities.player.movement.MovementManager;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

public class PlushieMovementSystem extends EntityTickingSystem<EntityStore> {
    private static final String[] PLUSHIE_EFFECTS = {"Potion_Morph_Hex_Template", "Potion_Morph_Hex_Infinite"};
    private static final float JUMP_FORCE_MULTIPLIER = 1.0F;
    private static final float DRAG_MULTIPLIER = 100.0F;

    private int[] effectIndexes;

    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(MovementManager.getComponentType(), EffectControllerComponent.getComponentType(), PlayerRef.getComponentType());
    }

    @Override
    public void tick(float dt, int index, @Nonnull ArchetypeChunk<EntityStore> chunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> buffer) {
        final var movementManager = chunk.getComponent(index, MovementManager.getComponentType());
        final var effects = chunk.getComponent(index, EffectControllerComponent.getComponentType());
        final var playerRef = chunk.getComponent(index, PlayerRef.getComponentType());
        if (movementManager == null || effects == null || playerRef == null) return;

        final var settings = movementManager.getSettings();
        final var defaults = movementManager.getDefaultSettings();
        if (settings == null || defaults == null) return;

        final boolean active = hasPlushieEffect(effects);
        final float jumpForce = active ? defaults.jumpForce * JUMP_FORCE_MULTIPLIER : defaults.jumpForce;
        final float drag = active ? defaults.dragCoefficient * DRAG_MULTIPLIER : defaults.dragCoefficient;

        if (settings.jumpForce == jumpForce && settings.dragCoefficient == drag) return;

        settings.jumpForce = jumpForce;
        settings.dragCoefficient = drag;
        movementManager.update(playerRef.getPacketHandler());
    }

    private boolean hasPlushieEffect(EffectControllerComponent effects) {
        if (effectIndexes == null) {
            effectIndexes = new int[PLUSHIE_EFFECTS.length];
            for (int i = 0; i < PLUSHIE_EFFECTS.length; i++) {
                effectIndexes[i] = EntityEffect.getAssetMap().getIndex(PLUSHIE_EFFECTS[i]);
            }
        }
        for (int effectIndex : effectIndexes) {
            if (effectIndex != AssetMapWithIndexes.NOT_FOUND && effects.hasEffect(effectIndex)) return true;
        }
        return false;
    }
}