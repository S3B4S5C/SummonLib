package me.s3b4s5.summonlib.systems.shared;

import com.hypixel.hytale.protocol.AnimationSlot;

/**
 * Animation slot lookup helpers.
 */
public final class SummonAnimationSlots {

    private SummonAnimationSlots() {}

    public static AnimationSlot resolveSlot(String... names) {
        for (String name : names) {
            try {
                return AnimationSlot.valueOf(name);
            } catch (Throwable ignored) {
            }
        }
        return AnimationSlot.values()[0];
    }
}


