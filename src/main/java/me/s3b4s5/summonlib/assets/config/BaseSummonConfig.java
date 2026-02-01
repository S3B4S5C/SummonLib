package me.s3b4s5.summonlib.assets.config;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import me.s3b4s5.summonlib.internal.impl.definition.SummonTuning;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class BaseSummonConfig {
    public String id = "";
    public AssetExtraInfo.Data data;
    public boolean unknown;

    // Common fields (shared by all summon types)
    public int slotCost = 1;
    public float damage = 0f;
    public double detectRadius = 0.0;
    public boolean requireOwnerLoS = true;
    public boolean requireSummonLoS = true;

    @Nullable
    public SummonTuning tuning = null;

    @Nonnull
    public String getId() {
        return id == null ? "" : id;
    }

    public boolean isUnknown() {
        return unknown;
    }
}
