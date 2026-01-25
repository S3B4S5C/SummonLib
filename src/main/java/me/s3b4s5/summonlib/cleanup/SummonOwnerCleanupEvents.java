package me.s3b4s5.summonlib.cleanup;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.event.events.player.AddPlayerToWorldEvent;
import com.hypixel.hytale.server.core.event.events.player.DrainPlayerFromWorldEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.s3b4s5.summonlib.tags.SummonTag;

import java.util.UUID;

public final class SummonOwnerCleanupEvents {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final ComponentType<EntityStore, SummonTag> summonTagType;
    private final boolean debug;

    public SummonOwnerCleanupEvents(ComponentType<EntityStore, SummonTag> summonTagType, boolean debug) {
        this.summonTagType = summonTagType;
        this.debug = debug;
    }

    public void onPlayerLeave(DrainPlayerFromWorldEvent event) {
        Holder<EntityStore> holder = event.getHolder();
        if (holder == null) {
            if (debug) LOGGER.atInfo().log("[SummonCleanup] onPlayerLeave: holder=null");
            return;
        }

        PlayerRef pr = holder.getComponent(PlayerRef.getComponentType());
        if (pr == null) {
            if (debug) LOGGER.atInfo().log("[SummonCleanup] onPlayerLeave: PlayerRef missing");
            return;
        }

        UUID ownerUuid = pr.getUuid();
        UUID worldUuid = pr.getWorldUuid();

        World w = (worldUuid != null) ? Universe.get().getWorld(worldUuid) : null;
        if (w == null) w = event.getWorld();
        if (w == null) {
            if (debug) LOGGER.atWarning().log("[SummonCleanup] onPlayerLeave: no world. owner=%s worldUuid=%s",
                    String.valueOf(ownerUuid), String.valueOf(worldUuid));
            return;
        }

        final World fw = w; // <- final para lambda

        if (debug) LOGGER.atInfo().log("[SummonCleanup] onPlayerLeave schedule. owner=%s world=%s",
                String.valueOf(ownerUuid), fw.getName());

        fw.execute(() -> {
            EntityStore es = fw.getEntityStore();
            SummonOwnerCleanup.killAllSummonsOfOwnerDirect(es, summonTagType, ownerUuid, debug);
        });
    }

    public void onPlayerDisconnect(PlayerDisconnectEvent event) {
        PlayerRef pr = event.getPlayerRef();
        if (pr == null) return;

        UUID ownerUuid = pr.getUuid();
        UUID worldUuid = pr.getWorldUuid();

        World w = (worldUuid != null) ? Universe.get().getWorld(worldUuid) : null;
        if (w == null) {
            if (debug) LOGGER.atInfo().log("[SummonCleanup] onPlayerDisconnect: world not found. owner=%s worldUuid=%s",
                    String.valueOf(ownerUuid), String.valueOf(worldUuid));
            return;
        }

        final World fw = w; // <- final

        if (debug) LOGGER.atInfo().log("[SummonCleanup] onPlayerDisconnect schedule. owner=%s world=%s",
                String.valueOf(ownerUuid), fw.getName());

        fw.execute(() -> {
            EntityStore es = fw.getEntityStore();
            SummonOwnerCleanup.killAllSummonsOfOwnerDirect(es, summonTagType, ownerUuid, debug);
        });
    }

    /** Opcional: limpia summons “fantasma” en otros worlds al entrar */
    public void onPlayerAdded(AddPlayerToWorldEvent event) {
        Holder<EntityStore> holder = event.getHolder();
        if (holder == null) return;

        PlayerRef pr = holder.getComponent(PlayerRef.getComponentType());
        if (pr == null) return;

        UUID ownerUuid = pr.getUuid();
        UUID worldUuid = pr.getWorldUuid();
        World keepWorld = (worldUuid != null) ? Universe.get().getWorld(worldUuid) : event.getWorld();

        if (debug) LOGGER.atInfo().log("[SummonCleanup] onPlayerAdded cross-world cleanup. owner=%s keepWorld=%s",
                String.valueOf(ownerUuid),
                (keepWorld != null ? keepWorld.getName() : "null"));

        SummonOwnerCleanup.removeOwnerSummonsFromOtherWorlds(summonTagType, ownerUuid, keepWorld, debug);
    }
}
