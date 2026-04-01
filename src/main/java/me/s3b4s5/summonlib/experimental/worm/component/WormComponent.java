package me.s3b4s5.summonlib.experimental.worm.component;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.UUID;

/**
 * Experimental ECS component used for worm-chain segment bookkeeping.
 */
public final class WormComponent implements Component<EntityStore> {

    public UUID chainId;
    public int segmentIndex;
    public double spacing;
    public double followSpeedMul;
    public double snapDistanceMul;

    public WormComponent() {
        chainId = new UUID(0L, 0L);
        segmentIndex = 0;
        spacing = 1.20;
        followSpeedMul = 1.0;
        snapDistanceMul = 3.0;
    }

    public WormComponent(WormComponent o) {
        this.chainId = o.chainId;
        this.segmentIndex = o.segmentIndex;
        this.spacing = o.spacing;
        this.followSpeedMul = o.followSpeedMul;
        this.snapDistanceMul = o.snapDistanceMul;
    }

    @NonNullDecl
    @Override
    public Component<EntityStore> clone() {
        return new WormComponent(this);
    }
}


