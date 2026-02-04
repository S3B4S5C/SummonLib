package me.s3b4s5.summonlib.systems.shared;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.AnimationSlot;

import javax.annotation.Nullable;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SummonCombatFollowShared {

    private SummonCombatFollowShared() {}

    public static boolean isWithin(Vector3d a, Vector3d b, double radius) {
        return distSq(a, b) <= radius * radius;
    }

    public static double distSq(Vector3d a, Vector3d b) {
        double dx = b.x - a.x, dy = b.y - a.y, dz = b.z - a.z;
        return dx * dx + dy * dy + dz * dz;
    }

    public static double distSqXZ(Vector3d a, Vector3d b) {
        double dx = b.x - a.x;
        double dz = b.z - a.z;
        return dx * dx + dz * dz;
    }

    public static float yawRadTo(Vector3d from, Vector3d to) {
        double vx = to.x - from.x;
        double vz = to.z - from.z;
        return (float) Math.atan2(-vx, -vz);
    }

    public static float pitchRadTo(Vector3d from, Vector3d to) {
        double dx = to.x - from.x;
        double dy = to.y - from.y;
        double dz = to.z - from.z;
        double h = Math.sqrt(dx * dx + dz * dz);
        if (h < 1e-6) return 0f;

        float p = (float) Math.atan2(dy, h);
        if (p < -1.2f) p = -1.2f;
        if (p > 1.2f) p = 1.2f;
        return p;
    }

    public static float computeStartStagger(int idx, int count, float attackInterval) {
        float step = attackInterval / Math.max(1, count);
        return step * Math.max(0, idx);
    }

    public static AnimationSlot resolveSlot(String... names) {
        for (String n : names) {
            try { return AnimationSlot.valueOf(n); } catch (Throwable ignored) {}
        }
        return AnimationSlot.values()[0];
    }

    public static boolean shouldLog(
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

    public static void dbg(
            HytaleLogger logger,
            boolean DEBUG,
            @Nullable String DEBUG_ONLY_SUMMON_ID,
            String logPrefix,
            UUID summonUuid,
            @Nullable String summonId,
            String msg
    ) {
        if (!DEBUG) return;
        if (DEBUG_ONLY_SUMMON_ID != null && summonId != null && !DEBUG_ONLY_SUMMON_ID.equals(summonId)) return;

        ((HytaleLogger.Api) logger.atInfo()).log("%s [%s] %s", logPrefix, shortId(summonUuid), msg);
    }

    public static void dbgOncePerSummon(
            HytaleLogger logger,
            boolean DEBUG,
            @Nullable String DEBUG_ONLY_SUMMON_ID,
            float DEBUG_LOG_PERIOD_SEC,
            ConcurrentHashMap<UUID, Float> debugCdBySummon,
            float dt,
            String logPrefix,
            UUID summonUuid,
            @Nullable String summonId,
            String msg
    ) {
        if (!DEBUG) return;
        if (shouldLog(DEBUG, DEBUG_ONLY_SUMMON_ID, DEBUG_LOG_PERIOD_SEC, debugCdBySummon, dt, summonUuid, summonId)) {
            dbg(logger, DEBUG, DEBUG_ONLY_SUMMON_ID, logPrefix, summonUuid, summonId, msg);
        }
    }

    public static String shortId(UUID u) {
        String s = String.valueOf(u);
        return (s.length() <= 8) ? s : s.substring(0, 8);
    }
}
