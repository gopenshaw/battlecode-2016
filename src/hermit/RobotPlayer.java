package hermit;

import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class RobotPlayer {

    public static void run(RobotController rc) {
        RobotType type = rc.getType();
        if (type == RobotType.ARCHON) {
            new Archon(rc).run();
        } else if (type == RobotType.GUARD) {
            new Guard(rc).run();
        }
    }
}
