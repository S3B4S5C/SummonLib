package me.s3b4s5.summonlib.internal.impl.spawn;

import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.BoundingBox;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.PersistentModel;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.UUID;
import java.util.function.IntFunction;

public final class ModelSummonSpawnFactory implements SummonSpawnFactory {

    private final IntFunction<String> modelAssetByVariant;
    private final float modelScale;

    public ModelSummonSpawnFactory(IntFunction<String> modelAssetByVariant, float modelScale) {
        this.modelAssetByVariant = modelAssetByVariant;
        this.modelScale = modelScale;
    }

    @Override
    public Holder<EntityStore> create(
            Store<EntityStore> store,
            UUID ownerUuid,
            Transform ownerTransform,
            Vector3d spawnPos,
            long spawnSeq,
            int variantIndex
    ) {
        String assetId = modelAssetByVariant.apply(variantIndex);
        ModelAsset modelAsset = ModelAsset.getAssetMap().getAsset(assetId);
        if (modelAsset == null) return null;

        Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();

        Model model = Model.createScaledModel(modelAsset, modelScale);

        holder.putComponent(PersistentModel.getComponentType(), new PersistentModel(model.toReference()));
        holder.putComponent(ModelComponent.getComponentType(), new ModelComponent(model));
        holder.putComponent(BoundingBox.getComponentType(), new BoundingBox());
        holder.putComponent(TransformComponent.getComponentType(), new TransformComponent(spawnPos, ownerTransform.getRotation().clone()));

        holder.ensureComponent(UUIDComponent.getComponentType());
        holder.putComponent(NetworkId.getComponentType(), new NetworkId(store.getExternalData().takeNextNetworkId()));

        return holder;
    }
}
