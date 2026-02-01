package me.s3b4s5.summonlib.tags;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Marca una entidad como segmento de un "worm".
 *
 * segmentIndex:
 *  0 = Head (NPC)
 *  1..N = Body (Model)
 *  last = Tail (Model)  -> (para el sistema da igual, solo es el último index)
 */
public class WormTag implements Component<EntityStore> {

    public UUID chainId;
    public int segmentIndex;

    /** Distancia deseada al segmento anterior (por pieza, para ajustar tamaños distintos) */
    public double spacing;

    /** Multiplicador extra de velocidad para este segmento */
    public double followSpeedMul;

    /**
     * Anti-estiramiento: si se aleja más de spacing*snapDistanceMul,
     * hacemos snap al lugar correcto.
     */
    public double snapDistanceMul;

    public WormTag() {
        chainId = new UUID(0L, 0L);
        segmentIndex = 0;
        spacing = 1.20;
        followSpeedMul = 1.0;
        snapDistanceMul = 3.0;
    }

    public WormTag(UUID chainId, int segmentIndex, double spacing) {
        this();
        this.chainId = chainId;
        this.segmentIndex = segmentIndex;
        this.spacing = spacing;
    }

    public WormTag(WormTag o) {
        this.chainId = o.chainId;
        this.segmentIndex = o.segmentIndex;
        this.spacing = o.spacing;
        this.followSpeedMul = o.followSpeedMul;
        this.snapDistanceMul = o.snapDistanceMul;
    }

    @Nullable
    @Override
    public Component<EntityStore> clone() {
        return new WormTag(this);
    }
}
