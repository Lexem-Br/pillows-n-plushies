package com.oruuke.pillow.interactions;

import com.hypixel.hytale.assetstore.codec.ContainedAssetCodec;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.common.map.IWeightedElement;
import com.hypixel.hytale.common.map.IWeightedMap;
import com.hypixel.hytale.common.map.WeightedMap;
import com.hypixel.hytale.common.util.ArrayUtil;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.protocol.Interaction;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.util.InteractionTarget;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.validators.NPCRoleValidator;

import javax.annotation.Nonnull;
import java.util.concurrent.ThreadLocalRandom;

public class ApplyEffectRandomModelInteraction extends SimpleInstantInteraction {
   @Nonnull
   public static final BuilderCodec<ApplyEffectRandomModelInteraction> CODEC = BuilderCodec.builder(
                   ApplyEffectRandomModelInteraction.class, ApplyEffectRandomModelInteraction::new, SimpleInstantInteraction.CODEC
      )
      .documentation("Applies the given entity effect to the entity.")
      .<String>appendInherited(
         new KeyedCodec<>("EffectId", new ContainedAssetCodec<>(EntityEffect.class, EntityEffect.CODEC)),
         (interaction, s) -> interaction.effectId = s,
         interaction -> interaction.effectId,
         (interaction, parent) -> interaction.effectId = parent.effectId
      )
      .addValidator(Validators.nonNull())
      .addValidatorLate(() -> EntityEffect.VALIDATOR_CACHE.getValidator().late())
      .add()
      .<InteractionTarget>appendInherited(
             new KeyedCodec<>("Entity", InteractionTarget.CODEC), (o, i) -> o.entityTarget = i, o -> o.entityTarget, (o, p) -> o.entityTarget = p.entityTarget
      )
      .documentation("The entity to target for this interaction.")
      .addValidator(Validators.nonNull())
      .add()
      .<ApplyEffectRandomModelInteraction.WeightedModel[]>append(
          new KeyedCodec<>("WeightedEntityIds", new ArrayCodec<>(ApplyEffectRandomModelInteraction.WeightedModel.CODEC, ApplyEffectRandomModelInteraction.WeightedModel[]::new)),
          (applyEffectRandomModelInteraction, o) -> applyEffectRandomModelInteraction.weightedModel = o,
              applyEffectRandomModelInteraction -> applyEffectRandomModelInteraction.weightedModel
      )
      .documentation("A weighted list of entity IDs from which an entity will be selected for change the model.")
      .add()
      .afterDecode(interaction -> {
         if (interaction.weightedModel != null && interaction.weightedModel.length > 0) {
           WeightedMap.Builder<String> mapBuilder = WeightedMap.builder(ArrayUtil.EMPTY_STRING_ARRAY);

           for (ApplyEffectRandomModelInteraction.WeightedModel entry : interaction.weightedModel) {
              mapBuilder.put(entry.id, entry.weight);
           }

           interaction.weightedSpawnMap = mapBuilder.build();
         }
      })
      .build();
   private String effectId;
   @Nonnull
   private InteractionTarget entityTarget = InteractionTarget.USER;
   protected ApplyEffectRandomModelInteraction.WeightedModel[] weightedModel;
   protected IWeightedMap<String> weightedSpawnMap;

   public ApplyEffectRandomModelInteraction() {
   }

   @Override
   protected void firstRun(@Nonnull InteractionType type, @Nonnull InteractionContext context, @Nonnull CooldownHandler cooldownHandler) {
      if (this.effectId != null && this.weightedSpawnMap != null) {
         String newModel = this.weightedSpawnMap.get(ThreadLocalRandom.current());
         EntityEffect entityEffect = EntityEffect.getAssetMap().getAsset(this.effectId);
         if (entityEffect != null) {
            Ref<EntityStore> ref = context.getEntity();
            Ref<EntityStore> targetRef = this.entityTarget.getEntity(context, ref);
            CommandBuffer<EntityStore> commandBuffer = context.getCommandBuffer();

            if (targetRef != null && targetRef.isValid() && commandBuffer != null) {
               EffectControllerComponent effectControllerComponent = commandBuffer.getComponent(targetRef, EffectControllerComponent.getComponentType());
               if (effectControllerComponent != null) {
                  effectControllerComponent.addEffect(targetRef, entityEffect, commandBuffer);
                  setModelChange(targetRef, commandBuffer, newModel);
               }
            }
         }
      }
   }

   public void setModelChange(@Nonnull Ref<EntityStore> ownerRef, @Nonnull ComponentAccessor<EntityStore> componentAccessor, String newModel) {
      ModelComponent modelComponent = componentAccessor.getComponent(ownerRef, ModelComponent.getComponentType());
      ModelAsset modelAsset = ModelAsset.getAssetMap().getAsset(newModel);
      if (modelComponent != null) {
         if (modelAsset != null) {
            Model scaledModel = Model.createRandomScaleModel(modelAsset);
            componentAccessor.putComponent(ownerRef, ModelComponent.getComponentType(), new ModelComponent(scaledModel));
         }
      }
   }

   @Override
   protected void simulateFirstRun(@Nonnull InteractionType type, @Nonnull InteractionContext context, @Nonnull CooldownHandler cooldownHandler) {
   }

   @Nonnull
   @Override
   protected Interaction generatePacket() {
      return new com.hypixel.hytale.protocol.ApplyEffectInteraction();
   }

   @Override
   protected void configurePacket(Interaction packet) {
      super.configurePacket(packet);
      com.hypixel.hytale.protocol.ApplyEffectInteraction p = (com.hypixel.hytale.protocol.ApplyEffectInteraction)packet;
      p.effectId = EntityEffect.getAssetMap().getIndex(this.effectId);
      p.entityTarget = this.entityTarget.toProtocol();
   }

   protected static class WeightedModel implements IWeightedElement {
      private static final BuilderCodec<ApplyEffectRandomModelInteraction.WeightedModel> CODEC = BuilderCodec.builder(
                      ApplyEffectRandomModelInteraction.WeightedModel.class, ApplyEffectRandomModelInteraction.WeightedModel::new
              )
              .append(new KeyedCodec<>("Id", Codec.STRING), (spawn, s) -> spawn.id = s, spawn -> spawn.id)
              .documentation("The model id to change.")
              .addValidator(Validators.nonNull())
              .addValidator(NPCRoleValidator.INSTANCE)
              .add()
              .<Double>append(new KeyedCodec<>("Weight", Codec.DOUBLE, true), (spawn, d) -> spawn.weight = d, spawn -> spawn.weight)
              .documentation("The relative weight of this NPC (chance of change the model, this value relative to the sum of all weights).")
              .addValidator(Validators.nonNull())
              .addValidator(Validators.greaterThan(0.0))
              .add()
              .build();
      private String id;
      private double weight;

      private WeightedModel() {
      }

      @Override
      public double getWeight() {
         return this.weight;
      }
   }

   @Nonnull
   @Override
   public String toString() {
      return "ApplyEffectInteraction{effectId='" + this.effectId + "', entityTarget=" + this.entityTarget + "} " + super.toString();
   }
}
