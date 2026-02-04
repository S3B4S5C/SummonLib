//package me.s3b4s5.summonlib.internal.impl.definition;
//
//import com.hypixel.hytale.component.Holder;
//import com.hypixel.hytale.component.Store;
//import com.hypixel.hytale.math.vector.Transform;
//import com.hypixel.hytale.math.vector.Vector3d;
//import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
//
//import me.s3b4s5.summonlib.api.follow.ModelFollowController;
//import me.s3b4s5.summonlib.internal.impl.spawn.SummonSpawnPlanFactory;
//import me.s3b4s5.summonlib.internal.impl.spawn.WormSummonSpawner;
//
//import javax.annotation.Nullable;
//import java.util.List;
//import java.util.UUID;
//
//public final class WormSummonDefinition extends SummonDefinition {
//
//    public final WormSummonSpawner.WormSpawnConfig wormConfig;
//
//    public WormSummonDefinition(
//            String id,
//            int slotCost,
//            float damage,
//            double detectRadius,
//            boolean requireOwnerLoS,
//            boolean requireSummonLoS,
//            ModelFollowController followController,
//            WormSummonSpawner.WormSpawnConfig wormConfig
//    ) {
//        this(id, slotCost, damage, detectRadius, requireOwnerLoS, requireSummonLoS, followController, wormConfig, null);
//    }
//
//    public WormSummonDefinition(
//            String id,
//            int slotCost,
//            float damage,
//            double detectRadius,
//            boolean requireOwnerLoS,
//            boolean requireSummonLoS,
//            ModelFollowController followController,
//            WormSummonSpawner.WormSpawnConfig wormConfig,
//            @Nullable SummonTuning tuning
//    ) {
//        super(
//                id,
//                slotCost,
//                damage,
//                detectRadius,
//                requireOwnerLoS,
//                requireSummonLoS,
//                followController,
//                null, // no single-holder factory
//                new WormPlanFactory(wormConfig),
//                tuning
//        );
//        this.wormConfig = wormConfig;
//    }
//
//    private static final class WormPlanFactory implements SummonSpawnPlanFactory {
//        private final WormSummonSpawner.WormSpawnConfig cfg;
//
//        private WormPlanFactory(WormSummonSpawner.WormSpawnConfig cfg) {
//            this.cfg = cfg;
//        }
//
//        @Override
//        public List<Holder<EntityStore>> createPlan(
//                Store<EntityStore> store,
//                UUID ownerUuid,
//                Transform ownerTransform,
//                Vector3d spawnPos,
//                long spawnSeq,
//                int variantIndex
//        ) {
//            return WormSummonSpawner.buildSpawnPlan(
//                    store,
//                    ownerUuid,
//                    ownerTransform,
//                    spawnPos,
//                    spawnSeq,
//                    variantIndex,
//                    cfg
//            );
//        }
//    }
//}
