package jeremy;

import battlecode.common.*;
import jeremy.util.RobotUtil;
import jeremy.message.MessageParser;

public class Soldier extends Robot {
    private Signal[] roundSignals;

    public Soldier(RobotController rc) {
        super(rc);
    }

    @Override
    protected void doTurn() throws GameActionException {
        roundSignals = rc.emptySignalQueue();
        shootZombies();
        moveTowardZombies();
        moveAwayFromArchon();
    }

    private void moveAwayFromArchon() throws GameActionException {
        if (!rc.isCoreReady()) {
            return;
        }

        RobotInfo[] adjacentTeammates = rc.senseNearbyRobots(2, team);
        RobotInfo archon = RobotUtil.getRobotOfType(adjacentTeammates, RobotType.ARCHON);
        if (archon != null) {
            tryMove(archon.location.directionTo(currentLocation));
        }
    }

    private void shootZombies() throws GameActionException {
        if (!rc.isWeaponReady()) {
            return;
        }

        RobotInfo[] attackableZombies = senseAttackableZombies();
        if (attackableZombies.length == 0) {
            return;
        }

        rc.attackLocation(attackableZombies[0].location);
    }

    private void moveTowardZombies() throws GameActionException {
        if (!rc.isCoreReady()) {
            return;
        }

        RobotData zombie = getBroadcastZombie();
        if (zombie == null) {
            return;
        }

        tryMoveToward(zombie.location);
    }


    private RobotData getBroadcastZombie() {
        for (Signal s : roundSignals) {
            if (s.getTeam() != team) continue;

            int[] message = s.getMessage();
            if (message == null) continue;

            MessageParser parser = new MessageParser(message[0], message[1], currentLocation);
            if (parser.getMessageType() == MessageType.ZOMBIE) {
                return parser.getRobotData();
            }
        }

        return null;
    }
}
