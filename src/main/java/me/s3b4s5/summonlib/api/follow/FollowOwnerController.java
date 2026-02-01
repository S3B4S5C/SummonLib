package me.s3b4s5.summonlib.api.follow;

import com.hypixel.hytale.math.vector.Vector3d;

/**
 * Simplest controller: home is just owner position + constant offset.
 * Useful for NPC-like summons where formation is not needed.
 */
public final class FollowOwnerController implements ModelFollowController {

    private final Vector3d offsetHome;
    private final Vector3d offsetAttack;

    public FollowOwnerController() {
        this(new Vector3d(0, 0, 0), new Vector3d(0, 0, 0));
    }

    public FollowOwnerController(Vector3d offsetHome, Vector3d offsetAttack) {
        this.offsetHome = (offsetHome != null) ? offsetHome : new Vector3d(0, 0, 0);
        this.offsetAttack = (offsetAttack != null) ? offsetAttack : new Vector3d(0, 0, 0);
    }

    @Override
    public Vector3d computeHome(Vector3d ownerPos, double yawRad, int groupIndex, int groupTotal) {
        return new Vector3d(
                ownerPos.x + offsetHome.x,
                ownerPos.y + offsetHome.y,
                ownerPos.z + offsetHome.z
        );
    }

    @Override
    public Vector3d computeAttackAnchor(Vector3d targetPos, int globalIndex, int globalTotal) {
        return new Vector3d(
                targetPos.x + offsetAttack.x,
                targetPos.y + offsetAttack.y,
                targetPos.z + offsetAttack.z
        );
    }
}
