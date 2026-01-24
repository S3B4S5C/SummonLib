package me.s3b4s5.summonlib.api.follow;

import com.hypixel.hytale.math.vector.Vector3d;

public interface ModelFollowController {

    Vector3d computeHome(Vector3d ownerPos, double ownerYawRad, int groupIndex, int groupTotal);

    Vector3d computeAttackAnchor(Vector3d targetPos, int globalIndex, int globalTotal);
}
