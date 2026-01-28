package me.s3b4s5.summonlib.internal.impl.spawn;

import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.UUID;

public interface SummonSpawnFactory {
    Holder<EntityStore> create(
            Store<EntityStore> store,
            UUID ownerUuid,
            Transform ownerTransform,
            Vector3d spawnPos,
            long spawnSeq,
            int variantIndex
    );
}
