package me.s3b4s5.summonlib.api.follow;

/**
 * Optional public API mix-in for follow behaviors that need to clamp owner pitch.
 */
public interface OwnerPitchClamp {
    /** @return clamped pitch in radians */
    double clampOwnerPitch(double pitchRad);
}


