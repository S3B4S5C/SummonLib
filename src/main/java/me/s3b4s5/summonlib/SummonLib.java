package me.s3b4s5.summonlib;

import com.hypixel.hytale.builtin.npceditor.NPCRoleAssetTypeHandler;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.event.events.player.DrainPlayerFromWorldEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.flock.Flock;
import com.hypixel.hytale.server.flock.FlockSystems;
import com.hypixel.hytale.server.npc.navigation.PathFollower;
import me.s3b4s5.summonlib.cleanup.SummonOwnerCleanupEvents;
import me.s3b4s5.summonlib.interactions.SummonCastInteraction;
import me.s3b4s5.summonlib.systems.SummonCombatFollowSystem;
import me.s3b4s5.summonlib.tags.SummonTag;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public class SummonLib extends JavaPlugin {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    // Single global ComponentType for all mods using the library.
    private static volatile ComponentType<EntityStore, SummonTag> SUMMON_TAG_TYPE;

    public SummonLib(@NonNullDecl JavaPluginInit init) {
        super(init);
        LOGGER.atInfo().log("[SummonLib] Loaded %s v%s", this.getName(), this.getManifest().getVersion());
    }

    public static ComponentType<EntityStore, SummonTag> summonTagType() {
        ComponentType<EntityStore, SummonTag> t = SUMMON_TAG_TYPE;
        if (t == null) {
            throw new IllegalStateException("SummonLib not initialized yet (SUMMON_TAG_TYPE is null).");
        }
        return t;
    }

    @Override
    protected void setup() {
        // Register the component ONCE (this plugin is the owner of the type).
        SUMMON_TAG_TYPE = this.getEntityStoreRegistry()
                .registerComponent(SummonTag.class, SummonTag::new);

        // Register custom Interaction type (assets: { "Type": "SummonCast", ... }).
        this.getCodecRegistry(Interaction.CODEC)
                .register("SummonCast", SummonCastInteraction.class, SummonCastInteraction.CODEC);

        SummonOwnerCleanupEvents cleanup = new SummonOwnerCleanupEvents(SUMMON_TAG_TYPE, true);
        this.getEventRegistry().registerGlobal(DrainPlayerFromWorldEvent.class, cleanup::onPlayerLeave);
        this.getEventRegistry().registerGlobal(PlayerDisconnectEvent.class, cleanup::onPlayerDisconnect);

        // Register the summon AI/combat system.
        this.getEntityStoreRegistry().registerSystem(new SummonCombatFollowSystem(SUMMON_TAG_TYPE));

        LOGGER.atInfo().log("[SummonLib] Registered SummonTag + SummonCombatFollowSystem + SummonCast interaction.");

        int idx = EntityStatType.getAssetMap().getIndex("MaxMinions");
        LOGGER.atInfo().log("MaxMinions stat idx = " + idx);

    }

    // Optional instance accessor (same as the static getter).
    public ComponentType<EntityStore, SummonTag> getSummonTagType() {
        return summonTagType();
    }
}
