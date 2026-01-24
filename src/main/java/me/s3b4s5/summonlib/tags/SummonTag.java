package me.s3b4s5.summonlib.tags;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nullable;
import java.util.UUID;

public class SummonTag implements Component<EntityStore> {

    public UUID owner;
    public String summonId;
    public int slotCost;

    public long spawnSeq;

    public int groupIndex;
    public int groupTotal;

    public int globalIndex;
    public int globalTotal;

    public int variantIndex;

    public UUID getOwnerUuid() { return owner; }
    public String getSummonId() { return summonId; }
    public int getSlotCost() { return slotCost; }
    public long getSpawnSeq() { return spawnSeq; }

    public int getGroupIndex() { return groupIndex; }
    public int getGroupTotal() { return groupTotal; }
    public int getGlobalIndex() { return globalIndex; }
    public int getGlobalTotal() { return globalTotal; }
    public int getVariantIndex() { return variantIndex; }

    public void setGroupIndex(int v) { groupIndex = v; }
    public void setGroupTotal(int v) { groupTotal = v; }
    public void setGlobalIndex(int v) { globalIndex = v; }
    public void setGlobalTotal(int v) { globalTotal = v; }
    public void setVariantIndex(int v) { variantIndex = v; }
    public void setSpawnSeq(long v) { spawnSeq = v; }

    public SummonTag() {
        owner = new UUID(0L, 0L);
        summonId = "";
        slotCost = 1;
        spawnSeq = 0L;
        groupIndex = 0;
        groupTotal = 1;
        globalIndex = 0;
        globalTotal = 1;
        variantIndex = 0;
    }

    public SummonTag(UUID owner, String summonId, int slotCost, long spawnSeq, int variantIndex) {
        this();
        this.owner = owner;
        this.summonId = summonId;
        this.slotCost = slotCost;
        this.spawnSeq = spawnSeq;
        this.variantIndex = variantIndex;
    }

    public SummonTag(SummonTag o) {
        this.owner = o.owner;
        this.summonId = o.summonId;
        this.slotCost = o.slotCost;
        this.spawnSeq = o.spawnSeq;
        this.groupIndex = o.groupIndex;
        this.groupTotal = o.groupTotal;
        this.globalIndex = o.globalIndex;
        this.globalTotal = o.globalTotal;
        this.variantIndex = o.variantIndex;
    }

    @Nullable
    @Override
    public Component<EntityStore> clone() {
        return new SummonTag(this);
    }
}
