package me.s3b4s5.summonlib.api.follow;

/**
 * Optional public API mix-in for follow behaviors that expose home rotation offsets.
 */
public interface HomeRotationOffsets {
    default double homeYawOffsetRad(int groupIndex, int groupTotal)  { return 0.0; }
    default double homePitchOffsetRad(int groupIndex, int groupTotal){ return 0.0; }
    default double homeRollOffsetRad(int groupIndex, int groupTotal) { return 0.0; }
}


