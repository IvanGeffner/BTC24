package kuma;

import battlecode.common.MapInfo;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.TrapType;

/**
 * Class that stores persistent info about the map. It is updated every round.
 */
public class MapData {

    RobotController rc;

    MapData(){
        rc = Robot.rc;
    }

    final static int MAX_MAP_SIZE = 64;
    final static int MAP_SZ_SQ = MAX_MAP_SIZE * MAX_MAP_SIZE;

    int crumbListSize = 0;

    int crumbIterator = 0;

    MapLocation targetCrumb = null;
    MapLocation closestCrumb = null;

    final int CRUMB_CHECK = 20;

    int[] mapData = new int[MAP_SZ_SQ];
    //first bit = seen / not seen
    //second bit crumbs / no crumbs
    //third bit crumb list;

    void checkAll(){
        closestCrumb = null;
        MapInfo[] locs = rc.senseNearbyMapInfos();
        for (MapInfo mi : locs){
            MapLocation mloc = mi.getMapLocation();
            if (mi.getTrapType() == TrapType.STUN) Robot.microManager.updateStunTrap(mloc);
            else if (mi.getTrapType() == TrapType.WATER) Robot.microManager.updateWaterTrap(mloc);
            int code = mloc.x*MAX_MAP_SIZE + mloc.y;
            int c = (mapData[code] | 1);
            int cr = (c & 2);
            if (mi.getCrumbs() > 0){
                if (cr == 0){
                    mapData[crumbListSize++] |= (code << 3);
                    c |= 2;
                }
                if (closestCrumb == null || rc.getLocation().distanceSquaredTo(closestCrumb) > rc.getLocation().distanceSquaredTo(mloc)) closestCrumb = mloc;
            }
            else if (mi.getCrumbs() == 0 && cr > 0) c ^= 2;
            if (mi.isWall()) c |= 4;
            mapData[code] = c;
        }
    }

    boolean visited(int x, int y){
        return ((mapData[x*MAX_MAP_SIZE +y] & 1) == 1);
    }

    void checkCurrentCrumb(){
        if (targetCrumb == null) return;
        int code = targetCrumb.x*MAX_MAP_SIZE + targetCrumb.y;
        if ((mapData[code] & 2) == 0) targetCrumb = null;
    }

    MapLocation getClosestCrumb() {
        if (crumbListSize == 0) return null;
        checkCurrentCrumb();
        if (targetCrumb != null) return targetCrumb;
        int CH = CRUMB_CHECK;
        if (CH > crumbListSize) CH = crumbListSize;
        MapLocation newTarget = null;
        for (int i = 0; i < CRUMB_CHECK; ++i){
            int ind = (crumbIterator + i)%crumbListSize;
            int code = (mapData[ind] >>> 3);
            if ((mapData[code] & 2) == 0) continue;
            int x = code / MAX_MAP_SIZE; int y = code % MAX_MAP_SIZE;
            MapLocation newLoc = new MapLocation(x,y);
            if (newTarget == null || rc.getLocation().distanceSquaredTo(newLoc) < rc.getLocation().distanceSquaredTo(newLoc)){
                newTarget = newLoc;
            }
        }
        targetCrumb = newTarget;
        return targetCrumb;
    }



}
