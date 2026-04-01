package me.s3b4s5.summonlib.assets.config;

import com.hypixel.hytale.assetstore.AssetExtraInfo;

import javax.annotation.Nonnull;

public abstract class BaseSummonConfig {
    public String id = "";
    public AssetExtraInfo.Data data;
    public boolean unknown;

    public int slotCost = 1;
    public float damage = 0f;
    public double detectRadius = 0.0;
    public boolean requireOwnerLoS = true;
    public boolean requireSummonLoS = true;

    public double leashSummonToOwner = 10.0;
    public double leashTargetToOwner = 8.0;
    public float ownerMaintenanceCooldownSec = 0.35f;

    @Nonnull
    public String getId() {
        return id == null ? "" : id;
    }

    public boolean isUnknown() {
        return unknown;
    }
}


