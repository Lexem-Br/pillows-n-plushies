package com.oruuke.pillow.interactions;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.spatial.SpatialResource;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.protocol.BlockPosition;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.RotationTuple;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInteraction;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import it.unimi.dsi.fastutil.Pair;
import org.joml.Vector3d;

import javax.annotation.Nonnull;
import java.util.List;

public class SpawnParticlesInteraction extends SimpleInteraction {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private String entityId = "Skeleton";

    public static final BuilderCodec<SpawnParticlesInteraction> CODEC =
            BuilderCodec.builder(SpawnParticlesInteraction.class, SpawnParticlesInteraction::new,
                            SimpleInteraction.CODEC)
                    .append(new KeyedCodec<>("ParticleId", Codec.STRING),
                            (config, value) -> config.entityId = value,
                            (config) -> config.entityId)
                    .documentation("The Id of the particle to spawn.")
                    .add()
                    .build();

    @Override
    protected void tick0(boolean firstRun, float time, @Nonnull InteractionType type, @Nonnull InteractionContext context, @Nonnull CooldownHandler cooldownHandler) {
        try {
            CommandBuffer<EntityStore> accessor = context.getCommandBuffer();
            if (accessor == null) {
                context.getState().state = InteractionState.Failed;
                super.tick0(firstRun, time, type, context, cooldownHandler);
                return;
            }

            BlockPosition blockPosition = context.getTargetBlock();
            if (blockPosition == null) {
                context.getState().state = InteractionState.Failed;
                super.tick0(firstRun, time, type, context, cooldownHandler);
                return;
            }

            String entityToSpawn = this.entityId;
//            if (this.weightedSpawnMap != null) {
//                entityToSpawn = this.weightedSpawnMap.get(ThreadLocalRandom.current());
//            }

            boolean spawned = trySpawn(blockPosition, entityToSpawn, accessor, context);
            if (!spawned) {
                context.getState().state = InteractionState.Failed;
                super.tick0(firstRun, time, type, context, cooldownHandler);
            }

            context.getState().state = InteractionState.Finished;
            super.tick0(firstRun, time, type, context, cooldownHandler);
        } catch (Exception e) {
            LOGGER.atSevere().log("[pillows] Spawn failed: %s", e.getMessage());
            context.getState().state = InteractionState.Failed;
        }
    }

    public static boolean trySpawn(BlockPosition position, String entityId, CommandBuffer<EntityStore> accessor, InteractionContext context) {
        World world = accessor.getExternalData().getWorld();
        Ref<ChunkStore> section = world.getChunkStore().getChunkSectionReferenceAtBlock(position.x, position.y, position.z);
        if (section == null) return false;

        BlockSection blockSection = section.getStore().getComponent(section, BlockSection.getComponentType());
        if (blockSection == null) return false;

        int blockRotationIndex = blockSection.getRotationIndex(position.x, position.y, position.z);
        RotationTuple rotation = RotationTuple.get(blockRotationIndex);
        Rotation3f blockRotation = new Rotation3f(0.0F, (float) (rotation.yaw().getRadians() + Math.PI), 0.0F);

        accessor.run(_store -> {
            int roleIndex = NPCPlugin.get().getIndex(entityId);
            if (roleIndex >= 0) {
                Vector3d blockVector = new Vector3d(position.x, position.y + 1, position.z);
                Pair<Ref<EntityStore>, NPCEntity> npcPair =NPCPlugin.get().spawnEntity(_store, roleIndex, blockVector, blockRotation, null, null);
                if (npcPair == null) {
                    context.getState().state = InteractionState.Failed;
                } else {
                    spawnDeathParticleEffect(npcPair.first(), context.getEntity().getStore());
                }
            }  else {
                context.getState().state = InteractionState.Failed;
                LOGGER.atWarning().log("Unable to spawn entity");
            }
        });

        return true;
    }

    public static void spawnDeathParticleEffect(@Nonnull Ref<EntityStore> ref, Store<EntityStore> store) {
        TransformComponent transformComponent = store.getComponent(ref, TransformComponent.getComponentType());
        if (transformComponent != null) {
            float eyeHeight = 0.0F;
            ModelComponent modelComponent = store.getComponent(ref, ModelComponent.getComponentType());
            if (modelComponent != null) {
                eyeHeight = modelComponent.getModel().getEyeHeight(ref, store);
            }

            Vector3d particlePos = new Vector3d(transformComponent.getPosition());
            particlePos.add(0.0F, eyeHeight, 0.0F);

            SpatialResource<Ref<EntityStore>, EntityStore> playerSpatialResource = store.getResource(
                    EntityModule.get().getPlayerSpatialResourceType()
            );
            List<Ref<EntityStore>> results = SpatialResource.getThreadLocalReferenceList();
            playerSpatialResource.getSpatialStructure().collect(particlePos, 75.0, results);
            ParticleUtil.spawnParticleEffect("Effect_Death", particlePos, results, store);
        }
    }

}
