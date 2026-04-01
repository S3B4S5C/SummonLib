package me.s3b4s5.summonlib.internal.bootstrap;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import me.s3b4s5.summonlib.lifecycle.SummonOwnerLifecycle;
import me.s3b4s5.summonlib.systems.model.SummonCombatFollowSystem;
import me.s3b4s5.summonlib.systems.npc.SummonAggroFromDamageSystem;
import me.s3b4s5.summonlib.systems.npc.SummonAggroRedirectSystem;
import me.s3b4s5.summonlib.systems.npc.SummonNpcTargetSystem;
import me.s3b4s5.summonlib.systems.shared.DespawnSummonsOnTeleportSystem;
import me.s3b4s5.summonlib.systems.shared.SummonDamageOverrideSystem;

public final class SummonLibSystemBootstrap {

    private SummonLibSystemBootstrap() {}

    public static void registerSystems(
            JavaPlugin plugin,
            SummonLibComponentBootstrap.ComponentBindings bindings,
            SummonOwnerLifecycle ownerLifecycle
    ) {
        plugin.getEntityStoreRegistry().registerSystem(
                new SummonCombatFollowSystem(bindings.summonComponentType(), bindings.wormComponentType())
        );
        plugin.getEntityStoreRegistry().registerSystem(
                new SummonNpcTargetSystem(bindings.summonComponentType(), bindings.wormComponentType())
        );
        plugin.getEntityStoreRegistry().registerSystem(
                new SummonAggroFromDamageSystem(bindings.summonComponentType())
        );
        plugin.getEntityStoreRegistry().registerSystem(
                new SummonDamageOverrideSystem(bindings.summonComponentType())
        );
        plugin.getEntityStoreRegistry().registerSystem(
                new SummonAggroRedirectSystem(bindings.summonComponentType())
        );
        plugin.getEntityStoreRegistry().registerSystem(new DespawnSummonsOnTeleportSystem(ownerLifecycle));
    }
}
