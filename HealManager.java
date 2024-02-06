package kuma;

import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.RobotInfo;

/**
 * Heal manager
 */
public class HealManager {

    static void heal() throws GameActionException {
        if (!Robot.rc.isActionReady()) return;
        if (!shouldHeal()) return;
        RobotInfo target = null;
        RobotInfo[] robots = Robot.rc.senseNearbyRobots(GameConstants.HEAL_RADIUS_SQUARED, Robot.rc.getTeam());
        for (RobotInfo r : robots){
            if (!Robot.rc.canHeal(r.getLocation())) continue;
            if (r.getHealth() == GameConstants.DEFAULT_HEALTH) continue;
            target = compare(target, r);
        }
        if (target == null) return;
        if (Robot.rc.canHeal(target.getLocation())){
            Robot.rc.heal(target.getLocation());
        }
    }

    //Heal comparator. It first prioritizes hurt builders, and then it heals the low hp used.
    static RobotInfo compare(RobotInfo A, RobotInfo B){
        if (A == null) return B;
        if (B == null) return A;

        if (A.hasFlag() && !B.hasFlag()) return A;
        if (!A.hasFlag() && B.hasFlag()) return B;

        if (isHurtBuilder(A) && !isHurtBuilder(B)) return A;
        if (isHurtBuilder(B) && !isHurtBuilder(A)) return B;

        if (A.getHealth() < B.getHealth()) return A;
        return B;
    }

    //Only heal if you're hurt (and fleeing), if you're out of move+attack range, or if you haven't attacked for several turns.
    static boolean shouldHeal() throws GameActionException {
        if (Robot.rc.getHealth() <= Constants.HURT_HP) return true;
        int enemies = Robot.rc.senseNearbyRobots(GameConstants.VISION_RADIUS_SQUARED, Robot.rc.getTeam().opponent()).length;
        if (enemies > 0){
            if (Robot.rc.getRoundNum() - AttackManager.attackRound < 5) return false;
        }
        return true;
    }

    static boolean isHurtBuilder(RobotInfo u){
        if (u.getBuildLevel() < 3) return false;
        return u.getHealth() < 800;
    }

}
