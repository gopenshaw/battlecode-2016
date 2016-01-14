package hermit;

import battlecode.common.*;
import hermit.message.MessageParser;

public class Guard extends Robot {
    private RobotInfo[] attackableZombies;
    private RobotInfo[] nearbyZombies;
    private Signal[] roundSignals;
    private RobotInfo[] adjacentFriends;

    public Guard(RobotController rc) {
        super(rc);
    }

    @Override
    protected void doTurn() throws GameActionException {
        roundSignals = rc.emptySignalQueue();
        senseRobots();
        attackZombies();
        moveTowardZombies();
        spreadOut();
    }

    private void spreadOut() throws GameActionException {
        if (!rc.isCoreReady()) {
            return;
        }

        RobotData robotRequestedSpread = getRobotRequestedSpread();
        if (robotRequestedSpread != null) {
            tryMove(robotRequestedSpread.location.directionTo(currentLocation));
        }
    }

    private RobotData getRobotRequestedSpread() {
        for (Signal s : roundSignals) {
            if (s.getTeam() != team) continue;

            int[] message = s.getMessage();
            if (message == null) continue;

            MessageParser parser = new MessageParser(message[0], message[1], currentLocation);
            if (parser.getMessageType() == MessageType.SPREAD) {
                return parser.getRobotData();
            }
        }

        return null;
    }

    private void senseRobots() {
        adjacentFriends = rc.senseNearbyRobots(2, team);
        attackableZombies = senseAttackableZombies();
        nearbyZombies = senseNearbyZombies();
    }

    private void attackZombies() throws GameActionException {
        if (!rc.isWeaponReady()) {
            return;
        }

        if (attackableZombies.length > 0) {
            rc.attackLocation(attackableZombies[0].location);
        }
    }

    private void moveTowardZombies() throws GameActionException {
        if (!rc.isCoreReady()) {
            return;
        }

        if (attackableZombies.length == 0
                && nearbyZombies.length > 0) {
            tryMoveToward(nearbyZombies[0].location);
        }
    }
}
