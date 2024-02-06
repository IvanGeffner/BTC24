package kuma;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;

import java.util.Random;

/**
 * Spawn manager.
 */
public class SpawnManager {

    static final Random rng = new Random(345345);


    /**
     * Spawn at emergency locations if possible. Otherwise spawn at random.
     */
    static void spawn() throws GameActionException {
        MapLocation emergency1 = null, emergency2 = null, emergency3 = null;
        int code1 = Comm.getFlagCode(0), code2 = Comm.getFlagCode(1), code3 = Comm.getFlagCode(2);
        if (code1 >= 0 && (code1 & 1) == 1){
            code1 >>>= 1;
            emergency1 = new MapLocation(code1/64, code1%64);
        }
        if (code2 >= 0 && (code2 & 1) == 1){
            code2 >>>= 1;
            emergency2 = new MapLocation(code2/64, code2%64);
        }
        if (code3 >= 0 && (code3 & 1) == 1){
            code3 >>>= 1;
            emergency3 = new MapLocation(code3/64, code3%64);
        }

        MapLocation[] spawnLocs = Robot.rc.getAllySpawnLocations();
        int bestDist = 0;
        MapLocation bestLocation = null;

        int start = rng.nextInt(spawnLocs.length);
        for (int i = 0; i < spawnLocs.length; ++i){
            MapLocation m = spawnLocs[(start+i)% spawnLocs.length];
            if (!Robot.rc.canSpawn(m)) continue;
            int dist = Constants.INF;
            if (emergency1 != null && m.distanceSquaredTo(emergency1) < dist) dist = m.distanceSquaredTo(emergency1);
            if (emergency2 != null && m.distanceSquaredTo(emergency2) < dist) dist = m.distanceSquaredTo(emergency2);
            if (emergency3 != null && m.distanceSquaredTo(emergency3) < dist) dist = m.distanceSquaredTo(emergency3);
            if (bestLocation == null || dist < bestDist){
                bestLocation = m;
                bestDist = dist;
            }
        }
        if (bestLocation != null){
            Robot.rc.spawn(bestLocation);
            Robot.birthLoc = bestLocation;
        }
    }
}
