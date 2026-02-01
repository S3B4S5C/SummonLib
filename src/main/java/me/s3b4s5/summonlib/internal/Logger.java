package me.s3b4s5.summonlib.internal;

import com.hypixel.hytale.logger.HytaleLogger;

import javax.annotation.Nullable;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class Logger {
    private final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final String prefix;

    public Logger(String prefix) {
        this.prefix = prefix;
    }

    public boolean shouldLog(
            boolean DEBUG,
            @Nullable String DEBUG_ONLY_SUMMON_ID,
            float DEBUG_LOG_PERIOD_SEC,
            ConcurrentHashMap<UUID, Float> debugCdBySummon,
            float dt,
            UUID summonUuid,
            @Nullable String summonId
    ) {
        if (!DEBUG) return false;
        if (DEBUG_ONLY_SUMMON_ID != null && summonId != null && !DEBUG_ONLY_SUMMON_ID.equals(summonId)) return false;

        float cd = debugCdBySummon.getOrDefault(summonUuid, 0f);
        cd = Math.max(0f, cd - dt);
        if (cd > 0f) {
            debugCdBySummon.put(summonUuid, cd);
            return false;
        }
        debugCdBySummon.put(summonUuid, DEBUG_LOG_PERIOD_SEC);
        return true;
    }

    public void dbg(
            boolean DEBUG,
            String msg
    ) {
        if (!DEBUG) return;
        ((HytaleLogger.Api) this.LOGGER.atInfo()).log("%s %s", this.prefix, msg);
    }

    public void dbgSummons(
            boolean DEBUG,
            @Nullable String DEBUG_ONLY_SUMMON_ID,
            UUID summonUuid,
            @Nullable String summonId,
            String msg
    ) {
        if (!DEBUG) return;
        if (DEBUG_ONLY_SUMMON_ID != null && summonId != null && !DEBUG_ONLY_SUMMON_ID.equals(summonId)) return;

        ((HytaleLogger.Api) this.LOGGER.atInfo()).log("%s [%s] %s", this.prefix, shortId(summonUuid), msg);
    }

    public static String shortId(UUID u) {
        String s = String.valueOf(u);
        return (s.length() <= 8) ? s : s.substring(0, 8);
    }
}
