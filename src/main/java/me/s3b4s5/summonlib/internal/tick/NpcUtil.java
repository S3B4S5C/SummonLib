package me.s3b4s5.summonlib.internal.tick;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

public final class NpcUtil {

    private NpcUtil() {}

    public static double computeHeadOffset(Store<EntityStore> store, Ref<EntityStore> targetRef) {
        double offset = 0.85;

        ModelComponent mc = store.getComponent(targetRef, ModelComponent.getComponentType());
        if (mc == null) return offset;

        Model model = mc.getModel();
        if (model == null) return offset;

        try {
            float eye = model.getEyeHeight(targetRef, store);
            if (eye > 0.05f) {
                // clamp razonable para no apuntar a 10m en mobs raros
                offset = Math.max(0.35, Math.min(eye, 2.2));
            }
        } catch (Throwable ignored) {}

        return offset;
    }

    public static void setLeashToPoint(
            Ref<EntityStore> selfRef,
            Ref<EntityStore> lookRef,
            Store<EntityStore> store,
            CommandBuffer<EntityStore> cb,
            Vector3d leashPoint
    ) {
        ComponentType<EntityStore, NPCEntity> npcType = NPCEntity.getComponentType();
        if (npcType == null) return;

        NPCEntity npc = cb.getComponent(selfRef, npcType);
        if (npc == null) npc = store.getComponent(selfRef, npcType);
        if (npc == null) return;

        npc.getLeashPoint().assign(leashPoint);

        TransformComponent lt = store.getComponent(lookRef, TransformComponent.getComponentType());
        if (lt != null) {
            Vector3f rot = lt.getRotation();
            npc.setLeashHeading(rot.getYaw());
            npc.setLeashPitch(rot.getPitch());
        }
    }
}
