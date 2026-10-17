package com.oruuke.pillow.modules.entity.condition;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.condition.Condition;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.time.Instant;

/**
 * A condition that checks if the player is connected.
 */
public class IsConnectedCondition extends Condition {
    @Nonnull
    public static final BuilderCodec<IsConnectedCondition> CODEC = BuilderCodec.builder(IsConnectedCondition.class, IsConnectedCondition::new, Condition.BASE_CODEC).build();

    protected IsConnectedCondition() {
    }

    @Override
    public boolean eval0(@Nonnull ComponentAccessor<EntityStore> componentAccessor, @Nonnull Ref<EntityStore> ref, @Nonnull Instant currentTime) {
        if (componentAccessor.getComponent(ref, Player.getComponentType()) != null) {
            Store<EntityStore> store = ref.getStore();
            UUIDComponent uuidComponent = store.getComponent(ref, UUIDComponent.getComponentType());
            if (uuidComponent != null) {
                var playerRef = Universe.get().getPlayer(uuidComponent.getUuid());
                boolean connected = playerRef != null;
                return connected;
            }
            return false;
        }
        return false;
    }

    @Nonnull
    @Override
    public String toString() {
        return "IsConnectedCondition{} " + super.toString();
    }
}