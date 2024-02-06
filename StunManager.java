package kuma;

import battlecode.common.*;


/**
 * Class that tracks which enemy units have been stunned. This is used for the micro.
 */
public class StunManager {

    static int[] StunRounds = new int[4096];
    static MapLocation[] lastRoundTraps = new MapLocation[200];
    static int lastRoundTrapsSize = 0;
    static MapLocation[] stunLocs = new MapLocation[200];
    static int stunLocsSize = 0;

    static MapLocation updatedLoc;

    /**
     * Updates the location of my traps at the end of turn
     */
    static void updateTraps(){
        lastRoundTrapsSize = 0;
        updatedLoc = Robot.rc.getLocation();
        MapInfo[] mis= Robot.rc.senseNearbyMapInfos();
        for (MapInfo m : mis){
            if (m.getTrapType() == TrapType.STUN) lastRoundTraps[lastRoundTrapsSize++] = m.getMapLocation();
        }
    }

    /**
     * If a stun trap is missing -> all nearby units are considered to be stunned. (We keep track, for each ID, when is the last turn that it has been stunned).
     */
    static void checkStuns() throws GameActionException {
        stunLocsSize = 0;
        if (updatedLoc != Robot.rc.getLocation()) return;
        for (int i = 0; i < lastRoundTrapsSize; ++i){
            MapLocation m = lastRoundTraps[i];
            MapInfo mi = Robot.rc.senseMapInfo(m);
            if (mi.getTrapType() != TrapType.STUN){
                stunLocs[stunLocsSize++] = m;
                Robot.rc.setIndicatorDot(m, 0, 0, 255);
            }
        }
        RobotInfo[] enemies = Robot.rc.senseNearbyRobots(GameConstants.VISION_RADIUS_SQUARED, Robot.rc.getTeam().opponent());
        for (RobotInfo r : enemies){
            for (int i = 0; i < stunLocsSize; ++i){
                if (stunLocs[i].distanceSquaredTo(r.getLocation()) <= 13){
                    StunRounds[r.getID()%4096] = Robot.rc.getRoundNum();
                }
            }
        }
        Robot.rc.setIndicatorString("Bytecode used: " + Clock.getBytecodeNum());
    }
}
