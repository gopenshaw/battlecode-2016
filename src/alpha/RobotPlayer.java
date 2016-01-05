package alpha;

import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class RobotPlayer {

    public static void run(RobotController rc) {
        if (rc.getType() == RobotType.ARCHON) {
            new Archon(rc).run(rc);
        }
        else if (rc.getType() == RobotType.SOLDIER) {
            new Soldier(rc).run(rc);
        }
    }
}
