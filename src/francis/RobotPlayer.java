package francis;

import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class RobotPlayer {

    public static void run(RobotController rc) {
        RobotType type = rc.getType();
        if (type == RobotType.ARCHON) {
            new Archon(rc).run();
        } else if (type == RobotType.SOLDIER) {
            new Soldier(rc).run();
        } else if (type == RobotType.VIPER) {
            new Viper(rc).run();
        } else if (type == RobotType.SCOUT) {
            new Scout(rc).run();
        }

        while (true);
    }
}
