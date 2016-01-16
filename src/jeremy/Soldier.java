package jeremy;

import battlecode.common.*;
import jeremy.util.BoundedQueue;
import jeremy.util.DirectionUtil;
import jeremy.util.RobotUtil;
import jeremy.message.MessageParser;

public class Soldier extends Robot {
    private Signal[] roundSignals;
    private RobotInfo[] attackableZombies;
    private RobotInfo[] nearbyZombies;
    BoundedQueue<Integer> zombieMemory = new BoundedQueue<Integer>(3);

    public Soldier(RobotController rc) {
        super(rc);
    }

    @Override
    protected void doTurn() throws GameActionException {
        roundSignals = rc.emptySignalQueue();
        senseZombies();
        shootZombies();
        microAwayFromZombies();
        moveTowardZombieNotGettingCloser();
        moveTowardBroadcastZombie();
        moveAwayFromArchon();
        updateZombieMemory();
    }

    private void updateZombieMemory() {
        forgetZombiesFromLastTurn();
        rememberZombiesFromThisTurn();
    }

    private void rememberZombiesFromThisTurn() {
        for (RobotInfo zombie : nearbyZombies) {
            zombieMemory.add(zombie.ID);
        }
    }

    private void forgetZombiesFromLastTurn() {
        zombieMemory.clear();
    }

    private void moveTowardZombieNotGettingCloser() throws GameActionException {
        if (nearbyZombies.length == 0
                || !rc.isCoreReady()) {
            return;
        }

        for (RobotInfo zombie : nearbyZombies) {
            if (zombie.type != RobotType.BIGZOMBIE
                    && sawZombieLastTurn(zombie)) {
                tryMoveToward(zombie.location);
                break;
            }
        }

    }

    private boolean sawZombieLastTurn(RobotInfo zombie) {
        return zombieMemory.contains(zombie.ID);
    }

    private void microAwayFromZombies() throws GameActionException {
        if (attackableZombies.length == 0
                || !rc.isCoreReady()) {
            return;
        }

        tryMove(DirectionUtil.getDirectionAwayFrom(attackableZombies, currentLocation));
    }

    private void senseZombies() {
        attackableZombies = senseAttackableZombies();
        nearbyZombies = senseNearbyZombies();
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

        RobotInfo lowestHealthZombie = RobotUtil.getLowestHealthRobot(attackableZombies);
        if (lowestHealthZombie == null) {
            return;
        }

        rc.attackLocation(lowestHealthZombie.location);
    }

    private void moveTowardBroadcastZombie() throws GameActionException {
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
