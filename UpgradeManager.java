package kuma;

import battlecode.common.GameActionException;
import battlecode.common.GlobalUpgrade;

/**
 * Upgrade manager.
 * 1 - attack
 * 2 - heal
 * 3 - flag
 */
public class UpgradeManager {

    static void run() throws GameActionException {
        if(Robot.rc.canBuyGlobal(GlobalUpgrade.ACTION)) Robot.rc.buyGlobal(GlobalUpgrade.ACTION);
        if(Robot.rc.canBuyGlobal(GlobalUpgrade.HEALING)) Robot.rc.buyGlobal(GlobalUpgrade.HEALING);
        if(Robot.rc.canBuyGlobal(GlobalUpgrade.CAPTURING)) Robot.rc.buyGlobal(GlobalUpgrade.CAPTURING);
    }
}
