package kuma;

import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.RobotInfo;

/**
 * Attack manager
 */
public class AttackManager {

    static int attackRound = 0; //last round I attacked

    static void attack() throws GameActionException {
        if (!Robot.rc.isActionReady()) return;
        RobotInfo target = null;
        RobotInfo[] enemies = Robot.rc.senseNearbyRobots(GameConstants.ATTACK_RADIUS_SQUARED, Robot.rc.getTeam().opponent());
        for (RobotInfo r : enemies){
            if (!Robot.rc.canAttack(r.getLocation())) continue;
            target = compare(target, r);
        }
        if (target == null) return;
        if (Robot.rc.canAttack(target.getLocation())){
            Robot.rc.attack(target.getLocation());
            attackRound = Robot.rc.getRoundNum();
            attack();
        }
    }

    //attack comparator. Prioritize attacking flagbearers and then the one with the least health.
    static RobotInfo compare(RobotInfo A, RobotInfo B){
        if (A == null) return B;
        if (B == null) return A;

        if (A.hasFlag() && !B.hasFlag()) return A;
        if (!A.hasFlag() && B.hasFlag()) return B;

        if (A.getHealth() < B.getHealth()) return A;
        return B;
    }
}
