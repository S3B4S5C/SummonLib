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
import me.s3b4s5.summonlib.assets.SummonConfig;
import me.s3b4s5.summonlib.assets.SummonConfigStore;
import me.s3b4s5.summonlib.cleanup.SummonOwnerCleanupEvents;
import me.s3b4s5.summonlib.interactions.SummonCastInteraction;
import me.s3b4s5.summonlib.interactions.SummonClearSummonsInteraction;
import me.s3b4s5.summonlib.interactions.SummonRemoveLastInteraction;
import me.s3b4s5.summonlib.systems.SummonCombatFollowSystem;
import me.s3b4s5.summonlib.systems.SummonNpcCombatFollowSystem;
import me.s3b4s5.summonlib.systems.SummonNpcLeashSystem;
import me.s3b4s5.summonlib.tags.SummonTag;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public final class SummonLib extends JavaPlugin {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static volatile ComponentType<EntityStore, SummonTag> SUMMON_TAG_TYPE;

    public SummonLib(@NonNullDecl JavaPluginInit init) {
        super(init);

        // Must run before AssetModule scans mods/ so our pack exists as a folder-pack
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

    @Override
    protected void setup() {
        // Codecs / assets
        getCodecRegistry(SummonConfig.CODEC)
                .register("SummonConfig", SummonConfig.class, SummonConfig.ABSTRACT_CODEC);
        getAssetRegistry().register(SummonConfigStore.create());

        // Components / systems
        SUMMON_TAG_TYPE = getEntityStoreRegistry().registerComponent(SummonTag.class, SummonTag::new);
        getEntityStoreRegistry().registerSystem(new SummonCombatFollowSystem(SUMMON_TAG_TYPE));
        getEntityStoreRegistry().registerSystem(new SummonNpcCombatFollowSystem(SUMMON_TAG_TYPE));
        //getEntityStoreRegistry().registerSystem(new SummonNpcLeashSystem(SUMMON_TAG_TYPE));
        // Interactions
        getCodecRegistry(Interaction.CODEC)
                .register("SummonCast", SummonCastInteraction.class, SummonCastInteraction.CODEC);
        getCodecRegistry(Interaction.CODEC)
                .register("SummonRemoveLast", SummonRemoveLastInteraction.class, SummonRemoveLastInteraction.CODEC);
        getCodecRegistry(Interaction.CODEC)
                .register("SummonClearSummons", SummonClearSummonsInteraction.class, SummonClearSummonsInteraction.CODEC);

        // Cleanup
        SummonOwnerCleanupEvents cleanup = new SummonOwnerCleanupEvents(SUMMON_TAG_TYPE, true);
        getEventRegistry().registerGlobal(DrainPlayerFromWorldEvent.class, cleanup::onPlayerLeave);
        getEventRegistry().registerGlobal(PlayerDisconnectEvent.class, cleanup::onPlayerDisconnect);
    }
}