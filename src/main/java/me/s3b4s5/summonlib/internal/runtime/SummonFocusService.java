package me.s3b4s5.summonlib.internal.runtime;

import me.s3b4s5.summonlib.internal.runtime.service.SummonRuntimeServices;

import javax.annotation.Nullable;
import java.util.UUID;

public final class SummonFocusService {

    private SummonFocusService() {}

    public static void setFocus(UUID ownerUuid, @Nullable UUID targetUuid) {
        SummonRuntimeServices.targets().setManualFocus(ownerUuid, targetUuid);
    }

    @Nullable
    public static UUID getFocus(UUID ownerUuid) {
        return SummonRuntimeServices.targets().getManualFocus(ownerUuid);
    }

    public static void clear(UUID ownerUuid) {
        SummonRuntimeServices.targets().setManualFocus(ownerUuid, null);
    }
}


