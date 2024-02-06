package kuma;

import battlecode.common.*;

/**
 * Trap manager. Places traps if there are enough enemies nearby and if it is close enough to an enemy.
 */
public class TrapManager {

    static final int MAX_ENEMY_DISTANCE = 5;
    static final Direction[] directions = {
            Direction.NORTH,
            Direction.NORTHEAST,
            Direction.EAST,
            Direction.SOUTHEAST,
            Direction.SOUTH,
            Direction.SOUTHWEST,
            Direction.WEST,
            Direction.NORTHWEST,
            Direction.CENTER
    };

    static int getMinEnemiesExpl(){
        if (Robot.rc.getCrumbs() > 1000) return 3;
        return 5;
    }

    static int getMinEnemiesStun(){
        if (Robot.rc.getCrumbs() > 1000) return 2;
        if (Robot.rc.getCrumbs() > 400) return 3;
        return 4;
    }

    static void placeExplosion() throws GameActionException {
        int minEnemies = getMinEnemiesExpl();
        if (Robot.visibleEnemies < minEnemies) return;
        if (Robot.rc.getCrumbs() < 1250) return;
        MapLocation myLoc = Robot.rc.getLocation();
        MicroManager.MicroInfo bestMicro = null;
        for (Direction dir : directions){
            MapLocation newLoc = myLoc.add(dir);
            if (Robot.rc.onTheMap(newLoc) && Robot.rc.senseMapInfo(newLoc).isWater()) continue;
            if (!Robot.rc.canBuild(TrapType.EXPLOSIVE, myLoc.add(dir))) continue;
            MicroManager.MicroInfo m = Robot.microManager.microInfo[dir.ordinal()];
            if (m.minDistanceToEnemy > MAX_ENEMY_DISTANCE) continue;
            if (m.alliesDist10 >= m.enemiesInMoveRange) continue;
            bestMicro = compare(bestMicro, m);
        }
        if (bestMicro != null) Robot.rc.build(TrapType.EXPLOSIVE, myLoc.add(bestMicro.dir));
    }

    static void placeStun() throws GameActionException {
        int minEnemies = getMinEnemiesStun();
        if (Robot.flagDefender) minEnemies = 1;
        if (Robot.visibleEnemies < minEnemies) return;
        MapLocation myLoc = Robot.rc.getLocation();
        MicroManager.MicroInfo bestMicro = null;
        int maxEnemyDistance = 5;
        if (Robot.rc.getCrumbs() >= 1500) maxEnemyDistance = 8;
        int minDistTrap = 5;
        if (Robot.rc.getCrumbs() >= 500) minDistTrap = 4;
        if (Robot.rc.getCrumbs() >= 800 || Robot.flagDefender) minDistTrap = 2;
        for (Direction dir : directions){
            if (!Robot.rc.canBuild(TrapType.STUN, myLoc.add(dir))) continue;
            MicroManager.MicroInfo m = Robot.microManager.microInfo[dir.ordinal()];
            if (m.minDistanceToEnemy > maxEnemyDistance) continue;
            //if (m.enemiesInMoveRange < MIN_ENEMIES_IN_MOVE_RANGE_STUN) continue;
            if (m.minDistTrap < minDistTrap) continue;
            bestMicro = compare(bestMicro, m);
        }
        if (bestMicro != null) Robot.rc.build(TrapType.STUN, myLoc.add(bestMicro.dir));
    }

    static void placeWater() throws GameActionException {
        MapLocation myLoc = Robot.rc.getLocation();
        MicroManager.MicroInfo bestMicro = null;
        int maxEnemyDistance = 5;
        if (Robot.rc.getCrumbs() >= 1500) maxEnemyDistance = 8;
        int minDistTrap = 9;
        for (Direction dir : directions){
            if (!Robot.rc.canBuild(TrapType.WATER, myLoc.add(dir))) continue;
            MapLocation newLoc = myLoc.add(dir);
            if (Robot.rc.onTheMap(newLoc) && Robot.rc.senseMapInfo(newLoc).getTeamTerritory() != Robot.rc.getTeam().opponent()) continue;
            MicroManager.MicroInfo m = Robot.microManager.microInfo[dir.ordinal()];
            if (m.minDistanceToEnemy > maxEnemyDistance) continue;
            if (m.minDistTrapWater < minDistTrap) continue;
            bestMicro = compare(bestMicro, m);
        }
        if (bestMicro != null) Robot.rc.build(TrapType.WATER, myLoc.add(bestMicro.dir));
    }

    static void placeTraps() throws GameActionException {
        if (Robot.rc.getRoundNum() <= Constants.ATTACK_ROUND) return; //Do not place traps before the attack phase.
        //Do not place traps if you're not a builder, you're not defending a flag, and if there are less than 3000 crumbs
        if (!isTrapper(Robot.internalID) && !shouldDefend() && Robot.rc.getCrumbs() < 3000) return;
        //If you're not a builder, place traps only if you can act later as well.
        if (!isTrapper(Robot.internalID) && Robot.rc.getActionCooldownTurns() >= 5) return;
        placeStun();
        if (isTrapper(Robot.internalID)) placeExplosion();
    }

    //Trap placement comparator. (Uses array from micro)
    static MicroManager.MicroInfo compare(MicroManager.MicroInfo A, MicroManager.MicroInfo B){
        if (A == null) return B;
        if (B == null) return A;
        if (A.minDistanceToEnemy < B.minDistanceToEnemy) return A;
        if (B.minDistanceToEnemy < A.minDistanceToEnemy) return B;
        if (A.enemiesInRange > B.enemiesInRange) return A;
        if (A.enemiesInRange < B.enemiesInRange) return B;
        if (A.enemiesInMoveRange > B.enemiesInMoveRange) return A;
        if (A.enemiesInMoveRange < B.enemiesInMoveRange) return B;
        return A;
    }

    static boolean isTrapper(int id){
        switch (id){
            case 17:
            case 35:
            case 19: return true;
        }
        return false;
    }

    static boolean shouldDefend(){
        return Robot.flagDefender && Robot.rc.getCrumbs() > 400;
    }

    static void digLikeCrazy() throws GameActionException {
        if (!isTrapper(Robot.internalID)) return;
        if (Robot.rc.getRoundNum() < 5) return;
        MapLocation myLoc = Robot.rc.getLocation();
        for (Direction dir : directions){
            MapLocation newLoc = myLoc.add(dir);
            if (Util.shouldFill(newLoc)) continue;
            if (Robot.rc.getLevel(SkillType.BUILD) >= 6) return;
            if (Robot.rc.canDig(newLoc)) Robot.rc.dig(newLoc);
        }
    }
}
