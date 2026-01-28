package me.s3b4s5.summonlib.internal.animation;

import com.hypixel.hytale.builtin.npceditor.NPCEditorPlugin;
import com.hypixel.hytale.builtin.npceditor.NPCRoleAssetTypeHandler;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.modules.physics.component.Velocity;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.npc.asset.builder.BuilderManager;
import com.hypixel.hytale.server.npc.corecomponents.BodyMotionBase;
import com.hypixel.hytale.server.npc.corecomponents.audiovisual.SensorAnimation;
import com.hypixel.hytale.server.npc.corecomponents.audiovisual.builders.BuilderSensorAnimation;
import com.hypixel.hytale.server.npc.corecomponents.combat.BodyMotionAimCharge;
import com.hypixel.hytale.server.npc.corecomponents.statemachine.ActionState;
import com.hypixel.hytale.server.npc.corecomponents.statemachine.SensorState;
import com.hypixel.hytale.server.npc.instructions.Instruction;
import com.hypixel.hytale.server.npc.instructions.Sensor;
import com.hypixel.hytale.server.npc.movement.controllers.MotionControllerBase;
import com.hypixel.hytale.server.npc.movement.controllers.MotionControllerDive;
import com.hypixel.hytale.server.npc.movement.controllers.MotionControllerFly;
import com.hypixel.hytale.server.npc.movement.controllers.MotionControllerWalk;
import com.hypixel.hytale.server.npc.role.RoleUtils;
import com.hypixel.hytale.server.npc.role.builders.BuilderRoleAbstract;
import com.hypixel.hytale.server.npc.role.support.RoleStats;
import com.hypixel.hytale.server.npc.role.support.StateSupport;
import com.hypixel.hytale.server.npc.systems.RoleBuilderSystem;
import com.hypixel.hytale.server.npc.systems.RoleSystems;

import java.util.UUID;

public interface SummonAnimator {
    void setBaseAnim(
            UUID summonUuid,
            Ref<EntityStore> summonRef,
            String animSetId,
            boolean loop,
            Store<EntityStore> store,
            boolean forceReplay
    );
}
