package kuma;

import battlecode.common.*;

/**
 * Class that tells each unit where to go.
 */
public class ObjectiveManager {

    RobotController rc;

    static final int MAX_DIST_ENEMY = 26;

    ObjectiveManager(){
        rc = Robot.rc;
    }

    MapLocation getTarget() throws GameActionException {

        //Early phase, just explore and get crumbs. If I'm a builder, find a place where I can dig
        if (rc.getRoundNum() < Constants.ATTACK_ROUND) {

            if (TrapManager.isTrapper(Robot.internalID) && Robot.rc.getLevel(SkillType.BUILD) < 6){
                MapLocation ans = getFillableLoc();
                if (ans != null) return ans;
            }

            if (Comm.getFlag() < GameConstants.NUMBER_FLAGS){
                MapLocation ans = Util.getClosestAlliedFlag();
                if (ans == null) return Robot.explore.getExploreTarget();
                MapLocation loc = Comm.getFlagPos(0);
                if (loc != null && loc.distanceSquaredTo(ans) == 0) return Robot.explore.getExploreTarget();
                loc = Comm.getFlagPos(1);
                if (loc != null && loc.distanceSquaredTo(ans) == 0) return Robot.explore.getExploreTarget();
                return ans;
            }

            MapLocation ans = getClosestCrumb();
            if (ans == null) ans = Robot.explore.mapData.getClosestCrumb();
            if (ans != null) return ans;
            return Robot.explore.getExploreTarget();
        }

        //Post-Dam phase

        //If I have a flag -> go to spawn zone
        if (rc.hasFlag()){
            MapLocation ans = Util.getClosestSpawnZone();
            if (ans != null) return ans;
            return Robot.explore.getExploreTarget();
        }

        //If I'm hurt -> go to closest ally (to heal or be healed).
        if (Robot.rc.getHealth() < Constants.HURT_HP){
            MapLocation loc = getClosestAlly();
            if(loc != null) return loc;
        }

        //Go to closest crumb if any
        MapLocation ans = getClosestCrumb();
        if (ans != null){
            return ans;
        }

        //Go to closest flag if it is close enough.
        MapLocation fl = Util.getClosestFlag();
        if (fl != null && rc.getLocation().distanceSquaredTo(fl) <= GameConstants.VISION_RADIUS_SQUARED){
            rc.setIndicatorString("Going to Flag");
            return fl;
        }

        //Go to closest enemy if close enough
        ans = getEnemyTarget();
        if (ans != null){
            return ans;
        }

        //Go to closest flag
        if (fl != null){
            return fl;
        }

        //Explore
        return Robot.explore.getExploreTarget();
    }

    MapLocation getEnemyTarget() throws GameActionException {
        MapLocation ans = Util.getClosestVisibleEnemy();
        if (ans != null) return ans;
        if (Comm.closestEnemy != null && rc.getLocation().distanceSquaredTo(Comm.closestEnemy) <= MAX_DIST_ENEMY) return Comm.closestEnemy;
        return null;
    }

    MapLocation getClosestAlly() throws GameActionException {
        MapLocation ans = null;
        MapLocation myLoc = Robot.rc.getLocation();
        RobotInfo[] allies = Robot.rc.senseNearbyRobots(GameConstants.VISION_RADIUS_SQUARED, Robot.rc.getTeam());
        for (RobotInfo r : allies){
            if (ans == null || myLoc.distanceSquaredTo(r.getLocation()) > myLoc.distanceSquaredTo(r.getLocation())) ans = r.getLocation();
        }
        if (ans == null) return null;
        if (ans.distanceSquaredTo(myLoc) <= 2) return myLoc;
        return ans;
    }



    MapLocation getClosestCrumb(){
        return Robot.explore.mapData.closestCrumb;
    }

    MapLocation fillableLoc = null;

    MapLocation getFillableLoc() throws GameActionException {
        MapInfo[] mis = Robot.rc.senseNearbyMapInfos();
        MapLocation myLoc = Robot.rc.getLocation();
        if (fillableLoc != null){
            if (myLoc.distanceSquaredTo(fillableLoc) == 0 || (rc.canSenseLocation(fillableLoc) && rc.senseMapInfo(fillableLoc).isWater())) fillableLoc = null;
        }
        MapLocation ans = fillableLoc;
        int x = SpawnManager.rng.nextInt(mis.length);
        for (int i = 0; i < mis.length; ++i){
            MapInfo m = mis[(i + x)%mis.length];
            if (m.isSpawnZone()) continue;
            if (m.isWater()) continue;
            if (m.isWall()) continue;
            MapLocation loc = m.getMapLocation();
            if ((loc.x + loc.y)%2 == 1){
                int d = loc.distanceSquaredTo(myLoc);
                if (d == 0) continue;
                if (ans == null || ans.distanceSquaredTo(Robot.birthLoc) > loc.distanceSquaredTo(Robot.birthLoc)) ans = loc;
            }
        }
        fillableLoc = ans;
        return ans;
    }
}
