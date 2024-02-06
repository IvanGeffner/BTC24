package kuma;

import battlecode.common.*;

/**
 * Class to report location of enemy flags, of our own flags (this wouldn't have been necessary if I had read the specs carefully in advance), and if some of our flags is being attacked.
 */
public class Comm {

    static final int ENEMY_CHANNEL = 5;
    static final int ENEMY_CHANNEL_SIZE = 15;

    final static int MAX_MAP_SIZE = 64;
    final static int MAP_SZ_SQ = MAX_MAP_SIZE * MAX_MAP_SIZE;

    final static int FLAG_INDEX_CHANNEL = 21;

    final static int[] FLAG_INFO = new int[]{2,3,4};

    static int[] enemies = new int[MAP_SZ_SQ];

    static MapLocation closestEnemy = null;

    final static int ENEMY_FLAG = 50;




    static void checkClosestEnemy() throws GameActionException {
        closestEnemy = null;
        MapLocation myLoc = Robot.rc.getLocation();
        for (int i = 0; i < ENEMY_CHANNEL_SIZE; ++i){
            int code = Robot.rc.readSharedArray(ENEMY_CHANNEL + i);
            if (code == 0) continue;
            --code;
            int diff = (Robot.rc.getRoundNum()&0xF) - (code & 0xF);
            if (diff > 4 || (diff < 0 && diff > -12)){
                Robot.rc.writeSharedArray(ENEMY_CHANNEL + i, 0);
                continue;
            }
            code >>>= 4;
            enemies[code] = Robot.rc.getRoundNum();
            MapLocation locCode = new MapLocation(code/MAX_MAP_SIZE, code%MAX_MAP_SIZE);
            if (closestEnemy == null || myLoc.distanceSquaredTo(locCode) < myLoc.distanceSquaredTo(closestEnemy)) closestEnemy = locCode;
        }
        //if (closestEnemy != null) Robot.rc.setIndicatorDot(closestEnemy, 255, 0, 0);
    }

    static void reportEnemies() throws GameActionException {
        MapLocation loc = Util.getClosestVisibleEnemy();
        if (loc == null) return;
        if (enemies[loc.x*64 + loc.y] >= Robot.rc.getRoundNum()) return;
        enemies[loc.x*64 + loc.y] = Robot.rc.getRoundNum();
        int index = Robot.rc.readSharedArray(ENEMY_CHANNEL + ENEMY_CHANNEL_SIZE);
        int code = ((loc.x*MAX_MAP_SIZE + loc.y) << 4) | (Robot.rc.getRoundNum() & 0xF);
        Robot.rc.writeSharedArray(ENEMY_CHANNEL + index, code + 1);
        index = (index + 1)%ENEMY_CHANNEL_SIZE;
        Robot.rc.writeSharedArray(ENEMY_CHANNEL + ENEMY_CHANNEL_SIZE, index);
    }

    static int getFlag() throws GameActionException {
        return Robot.rc.readSharedArray(FLAG_INDEX_CHANNEL);
    }

    static void reportFlag() throws GameActionException {
        int x = getFlag();
        ++x;
        Robot.rc.writeSharedArray(FLAG_INDEX_CHANNEL, x);
    }

    static int getFlagCode(int flagID) throws GameActionException {
        return Robot.rc.readSharedArray(FLAG_INFO[flagID]) - 1;
    }

    static void reportEmergency(int flagID, MapLocation flagPos) throws GameActionException {
        Robot.rc.writeSharedArray(FLAG_INFO[flagID], (((flagPos.x*64 + flagPos.y) << 1) | 1) + 1);
    }

    static void reportFlagInfo(int flagID, MapLocation flagPos) throws GameActionException {
        RobotInfo[] enemies = Robot.rc.senseNearbyRobots(GameConstants.VISION_RADIUS_SQUARED, Robot.rc.getTeam().opponent());
        int code = ((flagPos.x*64 + flagPos.y) << 1);
        if (enemies.length > 0){
            Robot.rc.writeSharedArray(FLAG_INFO[flagID], (code | 1) + 1);
        }
        else {
            Robot.rc.writeSharedArray(FLAG_INFO[flagID], code + 1);
        }
    }

    static MapLocation getFlagPos(int flagID) throws GameActionException {
        int code = Robot.rc.readSharedArray(FLAG_INFO[flagID]) - 1;
        if (code < 0) return null;
        code >>>= 1;
        return new MapLocation(code / 64, code%64);
    }

    static MapLocation getClosestFlag() throws GameActionException {
        MapLocation myLoc = Robot.rc.getLocation();
        MapLocation ans = null;
        MapLocation loc1 = getFlagPos(0);
        if (loc1 != null) ans = loc1;
        MapLocation loc2 = getFlagPos(1);
        if (loc2 != null){
            if (ans == null || myLoc.distanceSquaredTo(loc2) < myLoc.distanceSquaredTo(ans)) ans = loc2;
        }
        MapLocation loc3 = getFlagPos(2);
        if (loc3 != null){
            if (ans == null || myLoc.distanceSquaredTo(loc3) < myLoc.distanceSquaredTo(ans)) ans = loc3;
        }
        return ans;
    }

    public void report(FlagInfo f) throws GameActionException {
        int id = f.getID();
        int code1 = Robot.rc.readSharedArray(ENEMY_FLAG) - 1;
        int code2 = Robot.rc.readSharedArray(ENEMY_FLAG+2) - 1;
        int code3 = Robot.rc.readSharedArray(ENEMY_FLAG+4) - 1;
        if (code1 == id || code2 == id || code3 == id) return;
        int code = ((f.getLocation().x << 7) | (f.getLocation().y << 1) | 1);
        if (code1 < 0){
            Robot.rc.writeSharedArray(ENEMY_FLAG, id + 1);
            Robot.rc.writeSharedArray(ENEMY_FLAG + 1, code);
            return;
        }
        if (code2 < 0){
            Robot.rc.writeSharedArray(ENEMY_FLAG+2, id + 1);
            Robot.rc.writeSharedArray(ENEMY_FLAG + 3, code);
            return;
        }
        if (code3 < 0){
            Robot.rc.writeSharedArray(ENEMY_FLAG+4, id + 1);
            Robot.rc.writeSharedArray(ENEMY_FLAG + 5, code);
            Robot.rc.writeSharedArray(ENEMY_FLAG + 6, 1);
        }
    }

    public void reportFinished(FlagInfo f) throws GameActionException {
        int id = f.getID();
        int code1 = Robot.rc.readSharedArray(ENEMY_FLAG) - 1;
        int code2 = Robot.rc.readSharedArray(ENEMY_FLAG+2) - 1;
        int code3 = Robot.rc.readSharedArray(ENEMY_FLAG+4) - 1;
        if (code1 == id){
            Robot.rc.writeSharedArray(ENEMY_FLAG + 1, 0);
            return;
        }
        if (code2 == id){
            Robot.rc.writeSharedArray(ENEMY_FLAG + 3, 0);
            return;
        }
        if (code3 == id){
            Robot.rc.writeSharedArray(ENEMY_FLAG + 5, 0);
        }
    }

    public MapLocation getEnemyFlag(int i) throws GameActionException {
        int code = Robot.rc.readSharedArray(ENEMY_FLAG + 2*i + 1);
        if ((code & 1) == 0) return null;
        int x = (code >>> 7);
        int y = (code >>> 1)&0x3F;
        return new MapLocation(x,y);
    }
}
