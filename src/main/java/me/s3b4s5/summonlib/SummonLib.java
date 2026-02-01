package me.s3b4s5.summonlib;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.event.events.player.DrainPlayerFromWorldEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.s3b4s5.summonlib.assets.EmbeddedAssetPackExporter;
import me.s3b4s5.summonlib.assets.config.ModelSummonConfig;
import me.s3b4s5.summonlib.assets.config.NpcSummonConfig;
import me.s3b4s5.summonlib.assets.config.SummonConfig;
import me.s3b4s5.summonlib.assets.config.WormSummonConfig;
import me.s3b4s5.summonlib.assets.config.follow.Follow;
import me.s3b4s5.summonlib.assets.config.follow.OrbitFollowConfig;
import me.s3b4s5.summonlib.assets.config.follow.WingFollowConfig;
import me.s3b4s5.summonlib.assets.store.SummonConfigStore;
import me.s3b4s5.summonlib.assets.store.follow.FollowConfigStore;
import me.s3b4s5.summonlib.cleanup.SummonOwnerCleanupEvents;
import me.s3b4s5.summonlib.interactions.SummonCastInteraction;
import me.s3b4s5.summonlib.interactions.SummonClearSummonsInteraction;
import me.s3b4s5.summonlib.interactions.SummonRemoveLastInteraction;
import me.s3b4s5.summonlib.systems.SummonAggroFromDamageSystem;
import me.s3b4s5.summonlib.systems.SummonCombatFollowSystem;
import me.s3b4s5.summonlib.systems.SummonNpcTargetSystem;
import me.s3b4s5.summonlib.tags.SummonTag;
import me.s3b4s5.summonlib.tags.WormTag;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public final class SummonLib extends JavaPlugin {


    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static volatile ComponentType<EntityStore, SummonTag> SUMMON_TAG_TYPE;
    private static volatile ComponentType<EntityStore, WormTag> WORM_TAG_TYPE;

    public SummonLib(@NonNullDecl JavaPluginInit init) {
        super(init);

        try {
            EmbeddedAssetPackExporter.exportEmbeddedAssetPackToModsFolder(this);
        } catch (Throwable t) {
            LOGGER.atWarning().withCause(t).log("[SummonLib] Failed exporting embedded asset pack");
        }

        LOGGER.atInfo().log("[SummonLib] Loaded %s v%s", getName(), getManifest().getVersion());
    }

    public static ComponentType<EntityStore, SummonTag> summonTagType() {
        ComponentType<EntityStore, SummonTag> t = SUMMON_TAG_TYPE;
        if (t == null) throw new IllegalStateException("SummonLib not initialized yet (SUMMON_TAG_TYPE is null).");
        return t;
    }

    public static ComponentType<EntityStore, WormTag> wormTagType() {
        ComponentType<EntityStore, WormTag> t = WORM_TAG_TYPE;
        if (t == null) throw new IllegalStateException("SummonLib not initialized yet (WORM_TAG_TYPE is null).");
        return t;
    }

    @Override
    protected void setup() {
        // -----------------------
        // Codecs (Type-based)
        // -----------------------

        // 1) Register SummonConfig subtypes (this is the "Interaction-style" part)
        Follow.CODEC.register("Orbit", OrbitFollowConfig.class, OrbitFollowConfig.ABSTRACT_CODEC);

        Follow.CODEC.register("Wing", WingFollowConfig.class, WingFollowConfig.ABSTRACT_CODEC);

        SummonConfig.CODEC
                .register("Model", ModelSummonConfig.class, ModelSummonConfig.ABSTRACT_CODEC);
        SummonConfig.CODEC
                .register("Npc", NpcSummonConfig.class, NpcSummonConfig.ABSTRACT_CODEC);
        SummonConfig.CODEC
                .register("Worm", WormSummonConfig.class, WormSummonConfig.ABSTRACT_CODEC);


        // -----------------------
        // Asset stores
        // -----------------------

        // Single folder for all summon configs:
        //   Entity/SummonLib/Summons/<id>.json
        // and inside each JSON, "Type": "Model" (for now).
        getAssetRegistry().register(SummonConfigStore.create());
        getAssetRegistry().register(FollowConfigStore.create());
        // -----------------------
        // Components / systems
        // -----------------------
        SUMMON_TAG_TYPE = getEntityStoreRegistry().registerComponent(SummonTag.class, SummonTag::new);
        WORM_TAG_TYPE = getEntityStoreRegistry().registerComponent(WormTag.class, WormTag::new);

        getEntityStoreRegistry().registerSystem(new SummonCombatFollowSystem(SUMMON_TAG_TYPE, WORM_TAG_TYPE));
        getEntityStoreRegistry().registerSystem(new SummonNpcTargetSystem(SUMMON_TAG_TYPE, WORM_TAG_TYPE));
        //getEntityStoreRegistry().registerSystem(new SummonWormFollowSystem(SUMMON_TAG_TYPE, WORM_TAG_TYPE));
        getEntityStoreRegistry().registerSystem(new SummonAggroFromDamageSystem(SUMMON_TAG_TYPE));
        //getEntityStoreRegistry().registerSystem(new SummonWormHeadCombatSystem(SUMMON_TAG_TYPE, WORM_TAG_TYPE));

        // -----------------------
        // Interactions
        // -----------------------
        getCodecRegistry(Interaction.CODEC)
                .register("SummonCast", SummonCastInteraction.class, SummonCastInteraction.CODEC);
        getCodecRegistry(Interaction.CODEC)
                .register("SummonRemoveLast", SummonRemoveLastInteraction.class, SummonRemoveLastInteraction.CODEC);
        getCodecRegistry(Interaction.CODEC)
                .register("SummonClearSummons", SummonClearSummonsInteraction.class, SummonClearSummonsInteraction.CODEC);

        // -----------------------
        // Cleanup
        // -----------------------
        SummonOwnerCleanupEvents cleanup = new SummonOwnerCleanupEvents(SUMMON_TAG_TYPE, true);
        getEventRegistry().registerGlobal(DrainPlayerFromWorldEvent.class, cleanup::onPlayerLeave);
        getEventRegistry().registerGlobal(PlayerDisconnectEvent.class, cleanup::onPlayerDisconnect);
    }
}
