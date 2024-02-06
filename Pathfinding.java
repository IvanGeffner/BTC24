package kuma;

import battlecode.common.MapLocation;

/**
 * Pathfinding manager. In this game this class is unnecessary I pretty much only use bugnav.
 */
public class Pathfinding {

    BugPath bugPath;


    Pathfinding(){
        bugPath = new BugPath();
    }

    void moveTo(MapLocation target){
        bugPath.moveTo(target);
    }

}
