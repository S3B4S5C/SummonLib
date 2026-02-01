package me.s3b4s5.summonlib.assets.codec;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import me.s3b4s5.summonlib.assets.config.NpcSummonConfig;
import me.s3b4s5.summonlib.internal.impl.definition.SummonTuning;

public final class NpcSummonConfigCodec {

    private NpcSummonConfigCodec() {}

    public static BuilderCodec<NpcSummonConfig> create() {
        var b = BuilderCodec.builder(NpcSummonConfig.class, NpcSummonConfig::new);

        // Common: Id/Unknown + slotCost/damage/detect/LoS/tuning...
        SummonCommonCodec.appendCommon(b);

        // Only required field for NPC
        b.appendInherited(new KeyedCodec<>("NpcRoleId", Codec.STRING),
                (o, v) -> o.npcRoleId = (v == null ? "" : v),
                (o) -> o.npcRoleId,
                (o, p) -> o.npcRoleId = p.npcRoleId
        ).add();

        b.afterDecode((o, extra) -> {
            o.tuning = SummonTuning.orDefault(o.tuning);
            if (o.id == null) o.id = "";
            if (o.npcRoleId == null) o.npcRoleId = "";

            // Defaults (even if someone edits Java-side)
            if (o.despawnTimerSeconds < 0) o.despawnTimerSeconds = 0;
        });

        return b.build();
    }
}
