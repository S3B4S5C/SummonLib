package me.s3b4s5.summonlib;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.event.events.player.DrainPlayerFromWorldEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.s3b4s5.summonlib.assets.SummonConfig;
import me.s3b4s5.summonlib.assets.SummonConfigStore;
import me.s3b4s5.summonlib.cleanup.SummonOwnerCleanupEvents;
import me.s3b4s5.summonlib.interactions.SummonCastInteraction;
import me.s3b4s5.summonlib.interactions.SummonClearSummonsInteraction;
import me.s3b4s5.summonlib.interactions.SummonRemoveLastInteraction;
import me.s3b4s5.summonlib.systems.SummonCombatFollowSystem;
import me.s3b4s5.summonlib.tags.SummonTag;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public final class SummonLib extends JavaPlugin {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static volatile ComponentType<EntityStore, SummonTag> SUMMON_TAG_TYPE;

    public SummonLib(@NonNullDecl JavaPluginInit init) {
        super(init);
        LOGGER.atInfo().log("[SummonLib] Loaded %s v%s", getName(), getManifest().getVersion());
    }

    public static ComponentType<EntityStore, SummonTag> summonTagType() {
        ComponentType<EntityStore, SummonTag> t = SUMMON_TAG_TYPE;
        if (t == null) throw new IllegalStateException("SummonLib not initialized yet (SUMMON_TAG_TYPE is null).");
        return t;
    }

    public ComponentType<EntityStore, SummonTag> getSummonTagType() {
        return summonTagType();
    }

    @Override
    protected void setup() {
        getCodecRegistry(SummonConfig.CODEC)
                .register("SummonConfig", SummonConfig.class, SummonConfig.ABSTRACT_CODEC);

        getAssetRegistry().register(SummonConfigStore.create());

        SUMMON_TAG_TYPE = getEntityStoreRegistry().registerComponent(SummonTag.class, SummonTag::new);

        getCodecRegistry(Interaction.CODEC)
                .register("SummonCast", SummonCastInteraction.class, SummonCastInteraction.CODEC);

        getCodecRegistry(Interaction.CODEC)
                .register("SummonRemoveLast", SummonRemoveLastInteraction.class, SummonRemoveLastInteraction.CODEC);

        getCodecRegistry(Interaction.CODEC)
                .register("SummonClearSummons", SummonClearSummonsInteraction.class, SummonClearSummonsInteraction.CODEC);

        SummonOwnerCleanupEvents cleanup = new SummonOwnerCleanupEvents(SUMMON_TAG_TYPE, true);
        getEventRegistry().registerGlobal(DrainPlayerFromWorldEvent.class, cleanup::onPlayerLeave);
        getEventRegistry().registerGlobal(PlayerDisconnectEvent.class, cleanup::onPlayerDisconnect);

        getEntityStoreRegistry().registerSystem(new SummonCombatFollowSystem(SUMMON_TAG_TYPE));

        LOGGER.atInfo().log("[SummonLib] Registered SummonTag + SummonCombatFollowSystem + SummonCast interactions.");

        int idx = EntityStatType.getAssetMap().getIndex("MaxMinions");
        LOGGER.atInfo().log("MaxMinions stat idx = %d", idx);
    }
}
