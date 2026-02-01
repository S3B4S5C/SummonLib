package me.s3b4s5.summonlib.api.follow;

public interface HomeRotationOffsets {
    default double homeYawOffsetRad(int groupIndex, int groupTotal)  { return 0.0; }
    default double homePitchOffsetRad(int groupIndex, int groupTotal){ return 0.0; }
    default double homeRollOffsetRad(int groupIndex, int groupTotal) { return 0.0; }
}
