package me.s3b4s5.summonlib.internal.runtime.service;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Owns owner-scoped runtime bookkeeping such as summon sequence allocation and
 * owner maintenance cooldowns.
 */
public final class SummonOwnerStateService {

    private final ConcurrentHashMap<UUID, AtomicLong> spawnSeqByOwner = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<CooldownKey, Float> maintenanceCooldowns = new ConcurrentHashMap<>();

    public long nextSpawnSequence(UUID ownerUuid) {
        AtomicLong seq = spawnSeqByOwner.computeIfAbsent(ownerUuid, ignored -> new AtomicLong(System.nanoTime()));
        return seq.getAndIncrement();
    }

    public boolean tryRunMaintenance(String scope, UUID ownerUuid, float dt, float cooldownSec) {
        CooldownKey key = new CooldownKey(scope, ownerUuid);
        float remaining = maintenanceCooldowns.getOrDefault(key, 0f);
        remaining = Math.max(0f, remaining - dt);
        if (remaining > 0f) {
            maintenanceCooldowns.put(key, remaining);
            return false;
        }

        maintenanceCooldowns.put(key, Math.max(0f, cooldownSec));
        return true;
    }

    public void clearOwner(UUID ownerUuid) {
        if (ownerUuid == null) return;
        spawnSeqByOwner.remove(ownerUuid);
        maintenanceCooldowns.keySet().removeIf(key -> ownerUuid.equals(key.ownerUuid()));
    }

    private record CooldownKey(String scope, UUID ownerUuid) {}
}


