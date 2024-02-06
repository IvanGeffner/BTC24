package kuma;

import battlecode.common.*;

/**
 * Micro manager
 */
public class MicroManager {

    Direction[] dirs = Direction.values();

    final int INF = 1000000;
    boolean shouldPlaySafe = false;
    boolean alwaysInRange = false;
    boolean hurt = false;
    static int myRange;
    static int myVisionRange;
    boolean severelyHurt = false;

    static final int MAX_MICRO_BYTECODE_REMAINING = 5000;

    RobotController rc;
    int rangeExtended = 10;

    MicroManager(){
        this.rc = Robot.rc;
        myRange = GameConstants.ATTACK_RADIUS_SQUARED;
        myVisionRange = GameConstants.VISION_RADIUS_SQUARED;
    }

    static boolean canAttack;
    MicroInfo[] microInfo;

    boolean hurt(){
        return rc.getHealth() <= Constants.HURT_HP;
    }

    /**
     * Moves according to the best micro direction when there are enemies close enough.
     */
    boolean doMicro(){
        try {
            if (rc.getRoundNum() < Constants.ATTACK_ROUND) return false;
            if (!rc.isMovementReady()) return false;
            shouldPlaySafe = false;
            severelyHurt = hurt();

            if (!severelyHurt && rc.getActionCooldownTurns() < 20){
                RobotInfo[] enemiesShort = rc.senseNearbyRobots(rangeExtended, rc.getTeam().opponent());
                if (enemiesShort.length == 0) return false;
            }

            RobotInfo[] units = rc.senseNearbyRobots(myVisionRange, rc.getTeam().opponent());
            if (units.length == 0) return false;

            alwaysInRange = false;
            if (!rc.isActionReady()) alwaysInRange = true;
            if (severelyHurt) alwaysInRange = true;

            MicroInfo bestMicro = microInfo[8];

            for (int i = 7; i >= 0; --i) {
                if (microInfo[i].isBetter(bestMicro)) bestMicro = microInfo[i];
            }

            if (bestMicro.dir == Direction.CENTER) return true;

            if (rc.canMove(bestMicro.dir)) {
                rc.move(bestMicro.dir);
                return true;
            }

        } catch (Exception e){
            e.printStackTrace();
        }
        return false;
    }

    boolean stunned;

    /**
     * Computes an array with the information at the 9 adjacent tiles about enemies, allies and traps nearby.
     */
    void computeMicroArray(boolean allies) throws GameActionException{
        RobotInfo[] units = rc.senseNearbyRobots(myVisionRange, rc.getTeam().opponent());

        canAttack = rc.isActionReady();

        microInfo = new MicroInfo[9];
        for (int i = 0; i < 9; ++i) microInfo[i] = new MicroInfo(dirs[i]);

        for (RobotInfo unit : units) {
            if (Clock.getBytecodesLeft() < MAX_MICRO_BYTECODE_REMAINING) break;
            stunned = false;
            if (StunManager.StunRounds[unit.getID()%4096] >= Robot.rc.getRoundNum() - 1) stunned = true;
            if (stunned){
                Robot.rc.setIndicatorDot(unit.getLocation(), 0, 255, 0);
                microInfo[0].updateStunned(unit);
                microInfo[1].updateStunned(unit);
                microInfo[2].updateStunned(unit);
                microInfo[3].updateStunned(unit);
                microInfo[4].updateStunned(unit);
                microInfo[5].updateStunned(unit);
                microInfo[6].updateStunned(unit);
                microInfo[7].updateStunned(unit);
                microInfo[8].updateStunned(unit);
                //continue;
            }
            else {
                microInfo[0].updateEnemy(unit);
                microInfo[1].updateEnemy(unit);
                microInfo[2].updateEnemy(unit);
                microInfo[3].updateEnemy(unit);
                microInfo[4].updateEnemy(unit);
                microInfo[5].updateEnemy(unit);
                microInfo[6].updateEnemy(unit);
                microInfo[7].updateEnemy(unit);
                microInfo[8].updateEnemy(unit);
            }
        }

        if (!allies || units.length == 0) return;

        units = rc.senseNearbyRobots(myVisionRange, rc.getTeam());
        for (RobotInfo unit : units) {
            if (Clock.getBytecodesLeft() < MAX_MICRO_BYTECODE_REMAINING) break;
            microInfo[0].updateAlly(unit);
            microInfo[1].updateAlly(unit);
            microInfo[2].updateAlly(unit);
            microInfo[3].updateAlly(unit);
            microInfo[4].updateAlly(unit);
            microInfo[5].updateAlly(unit);
            microInfo[6].updateAlly(unit);
            microInfo[7].updateAlly(unit);
            microInfo[8].updateAlly(unit);
        }
    }

    void updateStunTrap(MapLocation loc){
        microInfo[0].updateTrap(loc);
        microInfo[1].updateTrap(loc);
        microInfo[2].updateTrap(loc);
        microInfo[3].updateTrap(loc);
        microInfo[4].updateTrap(loc);
        microInfo[5].updateTrap(loc);
        microInfo[6].updateTrap(loc);
        microInfo[7].updateTrap(loc);
        microInfo[8].updateTrap(loc);
    }

    void updateWaterTrap(MapLocation loc){
        microInfo[0].updateWater(loc);
        microInfo[1].updateWater(loc);
        microInfo[2].updateWater(loc);
        microInfo[3].updateWater(loc);
        microInfo[4].updateWater(loc);
        microInfo[5].updateWater(loc);
        microInfo[6].updateWater(loc);
        microInfo[7].updateWater(loc);
        microInfo[8].updateWater(loc);
    }

    class MicroInfo{
        Direction dir;
        MapLocation location;
        int minDistanceToEnemy = INF;
        int enemiesInRange = 0;
        int enemiesInMoveRange = 0;
        int alliesDist2 = 1;
        int alliesDist10 = 1;

        int minDistTrap = INF;

        int minDistTrapWater = INF;

        int minDistNoStun = INF;

        int enemiesInRangeNoStun = 0;

        int enemiesInMoveRangeNoStun = 0;

        boolean canMove = true;

        public MicroInfo(Direction dir){
            this.dir = dir;
            this.location = rc.getLocation().add(dir);
            if (dir != Direction.CENTER && !rc.canMove(dir)) canMove = false;
        }

        void updateEnemy(RobotInfo unit){
            int dist = unit.getLocation().distanceSquaredTo(location);
            if (dist < minDistNoStun) {
                minDistNoStun = dist;
                if (dist < minDistanceToEnemy) minDistanceToEnemy = dist;
            }
            if (dist <= myRange) {
                ++enemiesInRange;
                ++enemiesInRangeNoStun;
            }
            if (dist <= rangeExtended){
                ++enemiesInMoveRange;
                ++enemiesInMoveRangeNoStun;
            }
        }

        void updateStunned(RobotInfo unit){
            int dist = unit.getLocation().distanceSquaredTo(location);
            if (dist < minDistanceToEnemy) minDistanceToEnemy = dist;
            if (dist <= myRange) {
                ++enemiesInRange;
            }
            if (dist <= rangeExtended){
                ++enemiesInMoveRange;
            }
        }

        void updateAlly(RobotInfo unit){
            int dist = unit.getLocation().distanceSquaredTo(location);
            if (dist <= 2)  ++alliesDist2;
            if (dist <= 5) ++alliesDist10;
        }

        void updateTrap(MapLocation loc){
            int dist = location.distanceSquaredTo(loc);
            if (dist < minDistTrap) minDistTrap = dist;
        }

        void updateWater(MapLocation loc){
            int dist = location.distanceSquaredTo(loc);
            if (dist < minDistTrapWater) minDistTrapWater = dist;
        }

        int safe(){
            if (!canMove) return -1;
            if (alliesDist2 <= 1 && enemiesInRangeNoStun > 1) return 0;
            if (enemiesInRangeNoStun > 2) return 0;
            if (enemiesInMoveRangeNoStun > alliesDist10) return 1;
            return 2;
        }

        boolean inRange(){
            if (alwaysInRange) return true;
            return minDistanceToEnemy <= myRange;
        }

        /**
         * Comparator to choose which direction to move during micro.
         */
        boolean isBetter(MicroInfo M){

            if (safe() > M.safe()) return true;
            if (safe() < M.safe()) return false;

            if (inRange() && !M.inRange()) return true;
            if (!inRange() && M.inRange()) return false;

            if (!inRange()){
                if (minDistanceToEnemy < M.minDistanceToEnemy) return true;
                if (minDistanceToEnemy > M.minDistanceToEnemy) return false;
            }

            if (enemiesInRangeNoStun < M.enemiesInRangeNoStun) return true;
            if (enemiesInRangeNoStun > M.enemiesInRangeNoStun) return false;

            if (enemiesInMoveRangeNoStun <  M.enemiesInMoveRangeNoStun) return true;
            if (enemiesInMoveRangeNoStun > M.enemiesInMoveRangeNoStun) return false;

            if (minDistanceToEnemy < M.minDistanceToEnemy) return true;
            if (minDistanceToEnemy > M.minDistanceToEnemy) return false;

            if (dir == Direction.CENTER) return true;
            if (M.dir == Direction.CENTER) return false;

            return true;
        }
    }
}