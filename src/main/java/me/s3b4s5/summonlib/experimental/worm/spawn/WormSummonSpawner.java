package me.s3b4s5.summonlib.experimental.worm.spawn;

import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.s3b4s5.summonlib.SummonLib;
import me.s3b4s5.summonlib.experimental.worm.component.WormComponent;
import me.s3b4s5.summonlib.internal.component.SummonComponent;
import me.s3b4s5.summonlib.internal.spawn.ModelSummonSpawnFactory;
import me.s3b4s5.summonlib.internal.spawn.NpcRoleSummonSpawnFactory;

import javax.annotation.Nullable;
import java.util.*;

public final class WormSummonSpawner {

    private WormSummonSpawner() {}

    public static final int WORM_HEAD_INDEX = 0;
    public static final int WORM_FIRST_BODY_INDEX = 1;
    public static final int WORM_TAIL_INDEX = Integer.MAX_VALUE;

    public record WormSpawnConfig(String summonId, NpcRoleSummonSpawnFactory headFactory,
                                  ModelSummonSpawnFactory bodyFactory, ModelSummonSpawnFactory tailFactory,
                                  double spacingHeadToBody, double spacingBodyToBody, double spacingBodyToTail) {
    }

    public static List<Holder<EntityStore>> buildSpawnPlan(
            Store<EntityStore> store,
            UUID ownerUuid,
            Transform ownerTransform,
            Vector3d defaultSpawnPos,
            long spawnSeq,
            int variantIndex,
            WormSpawnConfig cfg
    ) {
        ExistingWorm existing = findExistingWorm(store, ownerUuid, cfg.summonId);

        if (existing == null) {
            UUID chainId = UUID.randomUUID();

            Holder<EntityStore> head = cfg.headFactory.create(store, ownerUuid, ownerTransform, defaultSpawnPos, spawnSeq, variantIndex);
            if (head == null) return List.of();

            Holder<EntityStore> body1 = cfg.bodyFactory.create(store, ownerUuid, ownerTransform, defaultSpawnPos, spawnSeq, variantIndex);
            if (body1 == null) return List.of();

            Holder<EntityStore> tail = cfg.tailFactory.create(store, ownerUuid, ownerTransform, defaultSpawnPos, spawnSeq, variantIndex);
            if (tail == null) return List.of();

            attachTags(head, ownerUuid, cfg.summonId, 0, Math.max(0L, spawnSeq - 2), variantIndex, chainId, WORM_HEAD_INDEX, cfg.spacingHeadToBody);
            attachTags(body1, ownerUuid, cfg.summonId, 1, spawnSeq,              variantIndex, chainId, WORM_FIRST_BODY_INDEX, cfg.spacingBodyToBody);
            attachTags(tail, ownerUuid, cfg.summonId, 0, Math.max(0L, spawnSeq - 1), variantIndex, chainId, WORM_TAIL_INDEX, cfg.spacingBodyToTail);

            ensureNetworkId(store, head);

            return List.of(head, body1, tail);
        }

        int nextBodyIndex = existing.maxBodyIndex + 1;
        Vector3d spawnPos = existing.tailPos != null ? existing.tailPos : defaultSpawnPos;

        Holder<EntityStore> body = cfg.bodyFactory.create(store, ownerUuid, ownerTransform, spawnPos, spawnSeq, variantIndex);
        if (body == null) return List.of();

        attachTags(body, ownerUuid, cfg.summonId, 1, spawnSeq, variantIndex, existing.chainId, nextBodyIndex, cfg.spacingBodyToBody);

        return List.of(body);
    }

    private static void attachTags(
            Holder<EntityStore> holder,
            UUID ownerUuid,
            String summonId,
            int slotCost,
            long spawnSeq,
            int variantIndex,
            UUID chainId,
            int segmentIndex,
            double spacingToPrev
    ) {
        holder.putComponent(SummonLib.summonComponentType(), new SummonComponent(ownerUuid, summonId, slotCost, spawnSeq, variantIndex));

        WormComponent wt = new WormComponent();
        wt.chainId = chainId;
        wt.segmentIndex = segmentIndex;
        wt.spacing = spacingToPrev;

        holder.putComponent(SummonLib.wormComponentType(), wt);
    }

    private static void ensureNetworkId(Store<EntityStore> store, Holder<EntityStore> holder) {
        try {
            NetworkId existing = holder.getComponent(NetworkId.getComponentType());
            if (existing != null) return;
        } catch (Throwable ignored) {
        }

        holder.putComponent(NetworkId.getComponentType(), new NetworkId(store.getExternalData().takeNextNetworkId()));
    }

    private record ExistingWorm(UUID chainId, int maxBodyIndex, @Nullable Vector3d tailPos) {
    }

    @Nullable
    private static ExistingWorm findExistingWorm(Store<EntityStore> store, UUID ownerUuid, String summonId) {
        Query<EntityStore> q = Query.and(
                SummonLib.summonComponentType(),
                SummonLib.wormComponentType(),
                UUIDComponent.getComponentType(),
                TransformComponent.getComponentType()
        );

        final UUID[] chain = new UUID[1];
        final int[] maxBody = new int[]{0};
        final Vector3d[] tailPos = new Vector3d[1];

        store.forEachChunk(q, (chunk, _) -> {
            for (int i = 0; i < chunk.size(); i++) {
                SummonComponent st = chunk.getComponent(i, SummonLib.summonComponentType());
                WormComponent wt = chunk.getComponent(i, SummonLib.wormComponentType());
                if (st == null || wt == null) continue;

                if (!ownerUuid.equals(st.owner)) continue;
                if (st.summonId == null || !st.summonId.equals(summonId)) continue;
                if (wt.chainId == null) continue;

                if (chain[0] != null && !chain[0].equals(wt.chainId)) continue;
                if (chain[0] == null) chain[0] = wt.chainId;

                int idx = wt.segmentIndex;

                if (idx > 0 && idx < WORM_TAIL_INDEX) {
                    if (idx > maxBody[0]) maxBody[0] = idx;
                }

                if (idx == WORM_TAIL_INDEX) {
                    TransformComponent tc = chunk.getComponent(i, TransformComponent.getComponentType());
                    if (tc != null) tailPos[0] = tc.getPosition();
                }
            }
        });

        if (chain[0] == null) return null;
        if (maxBody[0] < WORM_FIRST_BODY_INDEX) maxBody[0] = WORM_FIRST_BODY_INDEX;

        return new ExistingWorm(chain[0], maxBody[0], tailPos[0]);
    }
}



