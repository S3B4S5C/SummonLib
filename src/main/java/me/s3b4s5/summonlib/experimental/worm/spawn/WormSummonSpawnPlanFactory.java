package me.s3b4s5.summonlib.experimental.worm.spawn;

import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.s3b4s5.summonlib.internal.spawn.SummonSpawnPlanFactory;

import java.util.List;
import java.util.UUID;

public final class WormSummonSpawnPlanFactory implements SummonSpawnPlanFactory {

    private final WormSummonSpawner.WormSpawnConfig cfg;

    public WormSummonSpawnPlanFactory(WormSummonSpawner.WormSpawnConfig cfg) {
        this.cfg = cfg;
    }

    @Override
    public List<Holder<EntityStore>> createPlan(
            Store<EntityStore> store,
            UUID ownerUuid,
            Transform ownerTransform,
            Vector3d spawnPos,
            long spawnSeq,
            int variantIndex
    ) {
        return WormSummonSpawner.buildSpawnPlan(
                store,
                ownerUuid,
                ownerTransform,
                spawnPos,
                spawnSeq,
                variantIndex,
                cfg
        );
    }
}


