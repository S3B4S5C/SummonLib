//// File: SummonWormHeadCombatSystem.java
//package me.s3b4s5.summonlib.systems.worm;
//
//import com.hypixel.hytale.component.*;
//import com.hypixel.hytale.component.query.Query;
//import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
//import com.hypixel.hytale.math.vector.Transform;
//import com.hypixel.hytale.math.vector.Vector3d;
//import com.hypixel.hytale.protocol.AnimationSlot;
//import com.hypixel.hytale.server.core.entity.UUIDComponent;
//import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
//import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
//import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
//import com.hypixel.hytale.server.npc.entities.NPCEntity;
//
//import me.s3b4s5.summonlib.api.follow.FollowOwnerController;
//import me.s3b4s5.summonlib.internal.Logger;
//import me.s3b4s5.summonlib.internal.animation.DefaultSummonAnimator;
//import me.s3b4s5.summonlib.internal.movement.NpcLeashMovement;
//import me.s3b4s5.summonlib.internal.movement.SummonMovement;
//import me.s3b4s5.summonlib.internal.tick.ContextUtil;
//import me.s3b4s5.summonlib.systems.shared.SummonCombatFollowShared;
//import me.s3b4s5.summonlib.tags.SummonTag;
//import me.s3b4s5.summonlib.tags.WormTag;
//
//import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
//
//import java.util.*;
//import java.util.concurrent.ConcurrentHashMap;
//
//public class SummonWormHeadCombatSystem extends EntityTickingSystem<EntityStore> {
//
//    Logger logger = new Logger("[SummonWormHeadCombatSystem]");
//
//    private static final boolean DEBUG = false;
//
//    // --- Anim ---
//    private static final String ANIM_IDLE = "Idle";
//    private static final String ANIM_MOVE = "Move";
//
//    private static final AnimationSlot SLOT_BASE =
//            SummonCombatFollowShared.resolveSlot("Movement", "Idle", "Passive");
//
//    private final DefaultSummonAnimator animatorBase = new DefaultSummonAnimator(SLOT_BASE);
//    private final ConcurrentHashMap<UUID, Float> baseKeepAlive = new ConcurrentHashMap<>();
//
//    private final ComponentType<EntityStore, SummonTag> summonTagType;
//    private final ComponentType<EntityStore, WormTag> wormTagType;
//
//    private final SummonMovement MOVE_WORM = new NpcLeashMovement();
//
//    // Distance
//    private final double NPC_IDLE_DIST_SQ = 0.25;
//
//    public SummonWormHeadCombatSystem(
//            ComponentType<EntityStore, SummonTag> summonTagType,
//            ComponentType<EntityStore, WormTag> wormTagType
//    ) {
//        this.summonTagType = summonTagType;
//        this.wormTagType = wormTagType;
//    }
//
//    @NonNullDecl
//    @Override
//    public Query<EntityStore> getQuery() {
//        return Query.and(
//                summonTagType,
//                wormTagType,
//                NPCEntity.getComponentType(),
//                TransformComponent.getComponentType(),
//                UUIDComponent.getComponentType(),
//                NetworkId.getComponentType()
//        );
//    }
//
//    @Override
//    public void tick(
//            float dt,
//            int index,
//            @NonNullDecl ArchetypeChunk<EntityStore> chunk,
//            @NonNullDecl Store<EntityStore> store,
//            @NonNullDecl CommandBuffer<EntityStore> cb
//    ) {
//
//        ContextUtil.SummonCtx ctx = ContextUtil.getSummonCtxOrNull(
//                index, chunk, store, cb,
//                summonTagType, wormTagType,
//                null, true,
//                logger, DEBUG
//        );
//        if (ctx == null) return;
//
//        var tag = ctx.tag();
//        var wTag = ctx.wormTag();
//        var wormUuid = ctx.wormUuid();
//        var wormRef = ctx.wormRef();
//        var wormTransformComponent = ctx.wormT();
//
//        var owner = ctx.owner();
//        var ownerRef = ctx.ownerRef();
//        var world = ctx.world();
//
//        var def = ctx.def();
//        var tuning = def.tuning;
//
//        FollowOwnerController controller = new FollowOwnerController();
//
//        Transform ownerTranform = owner.getTransform();
//        Vector3d ownerPos = ownerTranform.getPosition();
//
//        Vector3d homeRaw = controller.computeHome(ownerPos, 0, 0, 1);
//        Vector3d home = SummonCombatFollowShared.applyOwnerHoverYOffset(ownerPos, homeRaw, tuning.hoverAboveOwner, tuning.maxAboveOwner);
//
//
//        Vector3d currentPos = wormTransformComponent.getPosition();
//
//        double distHomeXZSq = SummonCombatFollowShared.distSqXZ(currentPos, home);
//        boolean wantsToGoHome = distHomeXZSq > NPC_IDLE_DIST_SQ;
//
//        Vector3d desired = wantsToGoHome ? home : currentPos;
//
//        MOVE_WORM.setDesiredPointIfSupported(store, cb, wormRef, desired, 0, 0);
//    }
//}
