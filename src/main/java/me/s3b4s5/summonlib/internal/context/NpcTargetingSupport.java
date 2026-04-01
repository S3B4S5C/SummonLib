package me.s3b4s5.summonlib.internal.context;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * NPC targeting helpers unrelated to leash state.
 */
public final class NpcTargetingSupport {

    private NpcTargetingSupport() {}

    public static double computeHeadOffset(Store<EntityStore> store, Ref<EntityStore> targetRef) {
        double offset = 0.85;

        ModelComponent modelComponent = store.getComponent(targetRef, ModelComponent.getComponentType());
        if (modelComponent == null) return offset;

        Model model = modelComponent.getModel();
        if (model == null) return offset;

        try {
            float eye = model.getEyeHeight(targetRef, store);
            if (eye > 0.05f) {
                offset = Math.max(0.35, Math.min(eye, 2.2));
            }
        } catch (Throwable ignored) {
        }

        return offset;
    }
}


