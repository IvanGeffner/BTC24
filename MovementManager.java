package kuma;


import battlecode.common.*;

/**
 * Movement manager. This class stores info about adjacent tiles. There are three possible cases: PASSAABLE, IMPASSABLE, or FILL+PASSABLE.
 * When bugnaving, Fill+Passable is treated as passable if there are enough resources (this class also has its custom "move" method, which attempts to fill if necessary).
 */
public class MovementManager {

    static final int PASSABLE = 0;
    static final int FILL_PASSABLE = 1;
    static final int IMPASSABLE = 2;

    static final int DAM = 3;

    static int[] movementTypes;


    static void update() throws GameActionException {
        movementTypes = new int[9];
        for (Direction dir : Robot.directions){
            if (Robot.rc.canMove(dir)) movementTypes[dir.ordinal()] = PASSABLE;
            else {
                MapLocation newLoc = Robot.rc.getLocation().add(dir);
                if (!Robot.rc.onTheMap(newLoc)){
                    movementTypes[dir.ordinal()] = IMPASSABLE;
                    continue;
                }
                int rd = Robot.rc.getRoundNum();
                if (rd > Constants.ATTACK_ROUND && rd <= GameConstants.SETUP_ROUNDS && Robot.rc.senseMapInfo(newLoc).isDam()){
                    movementTypes[dir.ordinal()] = DAM;
                    continue;
                }
                if (!Util.shouldFill(newLoc) || !Robot.rc.canFill(newLoc)){
                    movementTypes[dir.ordinal()] = IMPASSABLE;
                    continue;
                }
                RobotInfo r = Robot.rc.senseRobotAtLocation(newLoc);
                if (r == null) movementTypes[dir.ordinal()] = FILL_PASSABLE;
                else movementTypes[dir.ordinal()] = IMPASSABLE;
            }
        }
        movementTypes[Direction.CENTER.ordinal()] = 0;
    }

    static boolean canMove(Direction dir){
        return movementTypes[dir.ordinal()] != IMPASSABLE;
    }

    static void move(Direction dir) throws GameActionException {
        if (dir == Direction.CENTER) return;
        int m = movementTypes[dir.ordinal()];
        if (m == PASSABLE) {
            Robot.rc.move(dir);
            //maybe update
        }
        else if (m == FILL_PASSABLE){
            if (!Robot.rc.canFill(Robot.rc.getLocation().add(dir))){
                Robot.rc.setIndicatorDot(Robot.rc.getLocation().add(dir), 255, 0, 0);
                return;
            }
            Robot.rc.fill(Robot.rc.getLocation().add(dir));
            //Robot.rc.move(dir);
        }
    }
}
