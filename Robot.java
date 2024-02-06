package kuma;

import battlecode.common.*;

public class Robot {

    /**
     * Core robot class. Contains all necessary info for other classes and all high level instructions.
     */
    static final Direction[] directions = {
            Direction.NORTH,
            Direction.NORTHEAST,
            Direction.EAST,
            Direction.SOUTHEAST,
            Direction.SOUTH,
            Direction.SOUTHWEST,
            Direction.WEST,
            Direction.NORTHWEST,
    };

    //static final Random rng = new Random(6147);

    static RobotController rc;
    static Pathfinding path;
    static Explore explore;

    static MicroManager microManager;

    static ObjectiveManager objectiveManager;
    static int H, W;

    static int internalID;
    static final int ID_CHANNEL = 0;
    static String bytecodeDebug;

    static Comm comm;

    MapLocation flagPos = null;
    int flagID = -1;


    static MapLocation[] originalLocs = null;

    static MapLocation birthLoc;


    static boolean flagDefender;

    static boolean adjacentToDam = false;

    static int visibleEnemies = 0;

    Robot(RobotController rc) throws GameActionException {
        this.rc = rc;
        H = rc.getMapHeight(); W = rc.getMapWidth();
        path = new Pathfinding();
        explore = new Explore();
        objectiveManager = new ObjectiveManager();
        microManager = new MicroManager();
        comm = new Comm();
        getInternalID();
        originalLocs = rc.senseBroadcastFlagLocations();
        BFS.initialize();
    }

    void getInternalID() throws GameActionException {
        internalID = rc.readSharedArray(ID_CHANNEL);
        rc.writeSharedArray(ID_CHANNEL,internalID + 1);
    }

    void checkDefender() throws GameActionException {
        flagDefender = false;
        MapLocation loc = Util.getClosestAlliedFlag();
        if (loc != null && rc.getLocation().distanceSquaredTo(loc) <= 2){
            flagDefender = true;
        }

    }

    //instructions run at the beginning of each turn
    void beginTurn() throws GameActionException {
        DebugString.s = new String();
        //try to spawn if possible
        if (!rc.isSpawned()){
            SpawnManager.spawn();
        }
        if (rc.isSpawned()) {
            //Update which enemy units are stunned
            StunManager.checkStuns();

            //Get info about adjacent tiles
            CheckAdjacent();

            //Try to upgrade stuff if possible
            UpgradeManager.run();

            //Update the movement manager's info
            MovementManager.update();

            //Update the micro info
            microManager.computeMicroArray(true);

            //Update the persistent map info
            explore.mapData.checkAll();

            //Update the comm info (+emergency signals)
            Comm.checkClosestEnemy();

            //Check if I can "defend" a flag and stay in that position forever.
            MapLocation flag = Util.getClosestAlliedFlag();
            if (Comm.getFlag() < GameConstants.NUMBER_FLAGS && flagPos == null && flag != null && rc.getLocation().distanceSquaredTo(flag) == 0){
                flagID = Comm.getFlag();
                flagPos = rc.getLocation();
                Comm.reportFlag();
            }

            //Check if I'm defending a flag or not.
            checkDefender();
        }
    }

    //Instructions at the end of each turn
    void endTurn() throws GameActionException {
        //If I'm a dead defender. There is definitely an emergency at my flag's location.
        if (!rc.isSpawned()){
            if (flagPos != null) Comm.reportEmergency(flagID, flagPos);
            return;
        }
        //Report flag status
        if (flagPos != null) Comm.reportFlagInfo(flagID, flagPos);

        //Update the info about nearby stun traps
        StunManager.updateTraps();

        //Report nearby enemies.
        Comm.reportEnemies();

        //Keep running the BFS with the remaining bytecode.
        BFS.runBFS();
    }

    //Core turn method
    void runTurn() throws GameActionException {
        if (!rc.isSpawned()) return; //Not alive? -> return

        //Try to pick nearby flags
        MapLocation closestFlag = Util.getClosestFlag();
        if (closestFlag != null && rc.canPickupFlag(closestFlag)) rc.pickupFlag(closestFlag);
        if (rc.hasFlag()){
            actFlag(); // <-- turn method for units that snatched enemy flags.
            if (rc.hasFlag()) return;
        }

        //Place traps if possible
        TrapManager.placeTraps();

        //Attack if possible
        AttackManager.attack();

        //Move (first check if I should do a micro movement. Otherwise go to objective).
        if (!canMoveAdjacentToFlag() && !microManager.doMicro()) {
            MapLocation loc;
            if (flagPos == null) loc = objectiveManager.getTarget();
            else loc = flagPos;
            if (loc != null) rc.setIndicatorDot(loc, 255, 100, 50);
            path.moveTo(loc);
        }
        //Try digging (does nothing if not a builder or if build lvl is 6)
        TrapManager.digLikeCrazy();
        //Attack if possible
        AttackManager.attack();
        //Heal if possible
        HealManager.heal();
    }

    //Method for robots with enemy flag.
    void actFlag() throws GameActionException {
        FlagInfo[] flags = rc.senseNearbyFlags(0);
        FlagInfo myFlag = null;
        if (flags.length != 0){
            myFlag = flags[0];
        }
        boolean moved = false;

        //Go to best location according to BFS (I think this is bugged and does nothing)
        if (BFS.dists != null) {
            MapLocation myLoc = rc.getLocation();
            Direction bestDir = null;
            int bestDist = BFS.dists[myLoc.x][myLoc.y];
            for (Direction dir : directions) {
                if (!rc.canMove(dir)) continue;
                if (BFS.dists == null) continue;
                MapLocation newLoc = rc.getLocation().add(dir);
                int d = BFS.dists[newLoc.x][newLoc.y];
                if (d == 0 || d >= bestDist) continue;
                bestDir = dir;
                bestDist = BFS.dists[newLoc.x][newLoc.y];
            }
            if (bestDir != null) {
                rc.setIndicatorString("Distance " + bestDist);
                rc.move(bestDir);
                moved = true;
                MapInfo m = rc.senseMapInfo(rc.getLocation());
                if (myFlag != null && m.isSpawnZone() && m.getTeamTerritory() == rc.getTeam()) comm.reportFinished(myFlag);
            }
        }

        //Bugnav. Report if flag is successfully retrieved.
        if (!moved){
            MapLocation target = Util.getClosestSpawnZone();
            path.moveTo(target);
            MapInfo m = rc.senseMapInfo(rc.getLocation());
            if (myFlag != null && m.isSpawnZone() && m.getTeamTerritory() == rc.getTeam()) comm.reportFinished(myFlag);
        }

        //Drop flag in the desired direction if I'm about to die.
        if (gonnaDie()) dropFlag();
    }

    void CheckAdjacent() throws GameActionException {
        adjacentToDam = false;
        MapInfo[] infos = rc.senseNearbyMapInfos(2);
        for (MapInfo m : infos) if (m.isDam()) adjacentToDam = true;
        visibleEnemies = Robot.rc.senseNearbyRobots(GameConstants.VISION_RADIUS_SQUARED, rc.getTeam().opponent()).length;
    }

    boolean gonnaDie(){
        int atk = 150;
        if (Robot.rc.getRoundNum() > 600) atk = 250;
        return (atk*microManager.microInfo[8].enemiesInMoveRange >= Robot.rc.getHealth());
    }

    void dropFlag() throws GameActionException {
        boolean dropped = false;
        MapLocation myLoc = rc.getLocation();
        if (BFS.dists != null) {
            Direction bestDir = null;
            int bestDist = BFS.dists[myLoc.x][myLoc.y];
            for (Direction dir : directions) {
                if (!rc.canDropFlag(myLoc.add(dir))) continue;
                if (BFS.dists == null) continue;
                MapLocation newLoc = rc.getLocation().add(dir);
                int d = BFS.dists[newLoc.x][newLoc.y];
                if (d == 0 || d >= bestDist) continue;
                bestDir = dir;
                bestDist = BFS.dists[newLoc.x][newLoc.y];
            }
            if (bestDir != null) {
                rc.setIndicatorString("Distance " + bestDist);
                rc.dropFlag(myLoc.add(bestDir));
                dropped = true;
            }
        }
        if (!dropped){
            MapLocation target = Util.getClosestSpawnZone();
            Direction bestDir = null;
            int bestDist = rc.getLocation().distanceSquaredTo(target);
            for (Direction dir : directions) {
                if (!rc.canDropFlag(myLoc.add(dir))) continue;
                MapLocation newLoc = rc.getLocation().add(dir);
                if (newLoc.distanceSquaredTo(target) < bestDist){
                    bestDir = dir;
                    bestDist = newLoc.distanceSquaredTo(target);
                }
            }
            if (bestDir != null){
                rc.dropFlag(myLoc.add(bestDir));
            }
        }
    }

    boolean canMoveAdjacentToFlag() throws GameActionException {
        MapLocation fl = Util.getClosestFlag();
        if (fl == null) return false;
        if (rc.getLocation().distanceSquaredTo(fl) <= 2) return true;
        MicroManager.MicroInfo bestMic = null;
        for (Direction dir : directions){
            if (!rc.canMove(dir)) continue;
            MapLocation loc = rc.getLocation().add(dir);
            if (loc.distanceSquaredTo(fl) <= 2){
                if (bestMic == null || microManager.microInfo[dir.ordinal()].isBetter(bestMic)) bestMic = microManager.microInfo[dir.ordinal()];
            }
        }
        if (bestMic != null){
            rc.move(bestMic.dir);
            return true;
        }
        return false;
    }
}
