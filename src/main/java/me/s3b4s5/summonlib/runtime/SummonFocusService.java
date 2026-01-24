package me.s3b4s5.summonlib.runtime;

import javax.annotation.Nullable;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SummonFocusService {

    private static final ConcurrentHashMap<UUID, UUID> focusTargetByOwner = new ConcurrentHashMap<>();

    private SummonFocusService() {}

    public static void setFocus(UUID ownerUuid, @Nullable UUID targetUuid) {
        if (targetUuid == null) focusTargetByOwner.remove(ownerUuid);
        else focusTargetByOwner.put(ownerUuid, targetUuid);
    }

    @Nullable
    public static UUID getFocus(UUID ownerUuid) {
        return focusTargetByOwner.get(ownerUuid);
    }

    public static void clear(UUID ownerUuid) {
        focusTargetByOwner.remove(ownerUuid);
    }
}
