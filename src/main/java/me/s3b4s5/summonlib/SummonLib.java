package me.s3b4s5.summonlib;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.s3b4s5.summonlib.internal.component.SummonComponent;
import me.s3b4s5.summonlib.internal.bootstrap.SummonLibAssetExportBootstrap;
import me.s3b4s5.summonlib.internal.bootstrap.SummonLibAssetStoreBootstrap;
import me.s3b4s5.summonlib.internal.bootstrap.SummonLibCodecBootstrap;
import me.s3b4s5.summonlib.internal.bootstrap.SummonLibComponentBootstrap;
import me.s3b4s5.summonlib.internal.bootstrap.SummonLibInteractionBootstrap;
import me.s3b4s5.summonlib.internal.bootstrap.SummonLibLifecycleBootstrap;
import me.s3b4s5.summonlib.internal.bootstrap.SummonLibSystemBootstrap;
import me.s3b4s5.summonlib.lifecycle.SummonOwnerLifecycle;
import me.s3b4s5.summonlib.experimental.worm.component.WormComponent;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public final class SummonLib extends JavaPlugin {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static volatile ComponentType<EntityStore, SummonComponent> SUMMON_COMPONENT_TYPE;
    private static volatile ComponentType<EntityStore, WormComponent> WORM_COMPONENT_TYPE;

    public SummonLib(@NonNullDecl JavaPluginInit init) {
        super(init);

        try {
            SummonLibAssetExportBootstrap.exportEmbeddedAssets(this);
        } catch (Throwable t) {
            LOGGER.atWarning().withCause(t).log("[SummonLib] Failed exporting embedded asset pack");
        }

        LOGGER.atInfo().log("[SummonLib] Loaded %s v%s", getName(), getManifest().getVersion());
    }

    public static ComponentType<EntityStore, SummonComponent> summonComponentType() {
        ComponentType<EntityStore, SummonComponent> t = SUMMON_COMPONENT_TYPE;
        if (t == null) throw new IllegalStateException("SummonLib not initialized yet (SUMMON_COMPONENT_TYPE is null).");
        return t;
    }

    public static ComponentType<EntityStore, WormComponent> wormComponentType() {
        ComponentType<EntityStore, WormComponent> t = WORM_COMPONENT_TYPE;
        if (t == null) throw new IllegalStateException("SummonLib not initialized yet (WORM_COMPONENT_TYPE is null).");
        return t;
    }

    @Override
    protected void setup() {
        SummonLibCodecBootstrap.registerCodecs(this);
        SummonLibAssetStoreBootstrap.registerAssetStores(this);

        SummonLibComponentBootstrap.ComponentBindings componentBindings =
                SummonLibComponentBootstrap.registerComponents(this);
        SUMMON_COMPONENT_TYPE = componentBindings.summonComponentType();
        WORM_COMPONENT_TYPE = componentBindings.wormComponentType();

        SummonOwnerLifecycle ownerLifecycle =
                SummonLibLifecycleBootstrap.createOwnerLifecycle(componentBindings);

        SummonLibSystemBootstrap.registerSystems(this, componentBindings, ownerLifecycle);
        SummonLibInteractionBootstrap.registerInteractions(this);
        SummonLibLifecycleBootstrap.registerLifecycle(this, ownerLifecycle);
    }
}


