package kuma;

import battlecode.common.*;

/**
 * Class with random helper methods
 */
public class Util {

    static MapLocation getClosestVisibleEnemy() throws GameActionException {
        MapLocation loc = null;
        RobotInfo[] enemies = Robot.rc.senseNearbyRobots(GameConstants.VISION_RADIUS_SQUARED, Robot.rc.getTeam().opponent());
        for (RobotInfo r : enemies){
            if (loc == null || Robot.rc.getLocation().distanceSquaredTo(loc) > Robot.rc.getLocation().distanceSquaredTo(r.getLocation())) loc = r.getLocation();
        }
        return loc;
    }

    static MapLocation getClosestSpawnZone() throws GameActionException {
        MapLocation ans = null;
        MapLocation myLoc = Robot.rc.getLocation();
        MapLocation[] locs = Robot.rc.getAllySpawnLocations();
        for (MapLocation loc : locs){
            if (ans == null || myLoc.distanceSquaredTo(ans) > myLoc.distanceSquaredTo(loc)) ans = loc;
        }
        return ans;
    }

    static MapLocation getClosestFlag() throws GameActionException {
        FlagInfo[] flags = Robot.rc.senseNearbyFlags(GameConstants.VISION_RADIUS_SQUARED);
        MapLocation myLoc = Robot.rc.getLocation();
        MapLocation ans = null;
        for (FlagInfo f : flags){
            if (f.getTeam() !=  Robot.rc.getTeam() && !f.isPickedUp()) {
                Robot.comm.report(f);
                if (ans == null || myLoc.distanceSquaredTo(f.getLocation()) < myLoc.distanceSquaredTo(ans))
                    ans = f.getLocation();
            }
        }
        if (ans != null) return ans;

        MapLocation[] locs =  Robot.rc.senseBroadcastFlagLocations();
        for (MapLocation loc : locs){
            loc = adjust(loc);
            if (Robot.rc.getLocation().distanceSquaredTo(loc) <= GameConstants.VISION_RADIUS_SQUARED){
                continue;
            }
            if (ans == null ||  Robot.rc.getLocation().distanceSquaredTo(loc) <  Robot.rc.getLocation().distanceSquaredTo(ans)) ans = loc;
        }
        return ans;
    }

    static MapLocation adjust(MapLocation loc) throws GameActionException {
        if (loc == null) return null;
        MapLocation ans = null;
        MapLocation loc1 = Robot.comm.getEnemyFlag(0);
        if (loc1 != null) ans = loc1;
        MapLocation loc2 = Robot.comm.getEnemyFlag(1);
        if (loc2 != null){
            if (ans == null) ans = loc2;
            else {
                if (loc2.distanceSquaredTo(loc) < ans.distanceSquaredTo(loc)) ans = loc2;
            }
        }
        MapLocation loc3 = Robot.comm.getEnemyFlag(2);
        if (loc3 != null){
            if (ans == null) ans = loc3;
            else {
                if (loc3.distanceSquaredTo(loc) < ans.distanceSquaredTo(loc)) ans = loc3;
            }
        }
        if (ans != null && loc.distanceSquaredTo(ans) <= 100) return ans;
        return loc;
    }

    static MapLocation getClosestFlagOriginal() throws GameActionException {
        MapLocation myLoc = Robot.rc.getLocation();
        MapLocation ans = null;

        MapLocation[] locs =  Robot.originalLocs;
        for (MapLocation loc : locs){
            if (ans == null ||  myLoc.distanceSquaredTo(loc) <  myLoc.distanceSquaredTo(ans)) ans = loc;
        }
        return ans;
    }

    static MapLocation getClosestAlliedFlag() throws GameActionException {
        FlagInfo[] flags = Robot.rc.senseNearbyFlags(GameConstants.VISION_RADIUS_SQUARED);
        MapLocation myLoc = Robot.rc.getLocation();
        MapLocation ans = null;
        for (FlagInfo f : flags){
            if (f.getTeam() ==  Robot.rc.getTeam() && !f.isPickedUp()) {
                if (ans == null || myLoc.distanceSquaredTo(f.getLocation()) < myLoc.distanceSquaredTo(ans))
                    ans = f.getLocation();
            }
        }
        return ans;
    }

    static boolean shouldFill(MapLocation loc) throws GameActionException {
        if (Robot.visibleEnemies > 0 && Robot.adjacentToDam && Robot.rc.getRoundNum() > 150 && Robot.rc.getRoundNum() < 190 && Robot.rc.getCrumbs() > 1000) return true;
        if ((loc.x+loc.y)%2 == 0) return true;
        if (Robot.rc.onTheMap(loc) && Robot.rc.senseMapInfo(loc).getCrumbs() > 0) return true;
        return false;
    }
}
