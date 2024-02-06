package kuma;

import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;


/**
 * Core loop
 */
public strictfp class RobotPlayer {
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {
        Robot u = new Robot(rc);

        while(true){
            u.beginTurn();
            u.runTurn();
            u.endTurn();
            Clock.yield();
        }
    }
}
