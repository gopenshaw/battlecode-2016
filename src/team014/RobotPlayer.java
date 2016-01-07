package team014;

import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class RobotPlayer {

    public static void run(RobotController rc) {
        RobotType type = rc.getType();
        if (type == RobotType.ARCHON) {
            new Archon(rc).run(rc);
        } else if (type == RobotType.SOLDIER) {
            new Soldier(rc).run(rc);
        } else if (type == RobotType.TURRET) {
            new Turret(rc).run(rc);
        } else if (type == RobotType.GUARD) {
            new Guard(rc).run(rc);
        } else if (type == RobotType.SCOUT) {
            new Scout(rc).run(rc);
        } else if (type == RobotType.VIPER) {
            new Viper(rc).run(rc);
        }
    }
}
