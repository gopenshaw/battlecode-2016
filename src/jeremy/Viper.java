package jeremy;

import battlecode.common.*;
import jeremy.message.MessageParser;
import jeremy.util.BoundedQueue;
import jeremy.util.RobotUtil;

public class Viper extends Robot {
    private static final int SAFE_DISTANCE_FROM_ENEMY_BASE = 100;
    private Signal[] roundSignals;
    private RobotInfo[] attackableZombies;
    BoundedQueue<Integer> zombieMemory = new BoundedQueue<Integer>(3);
    private RobotInfo[] attackableEnemies;
    private MapLocation enemyLocation;

    public Viper(RobotController rc) {
        super(rc);
    }

    @Override
    protected void doTurn() throws GameActionException {
        readBroadcasts();
        senseRobots();
        shootEnemies();
        goToSpecialPlace();
        moveTowardEnemy();
        moveAwayFromArchon();
    }

    private void moveTowardEnemy() throws GameActionException {
        if (enemyLocation == null
                || attackableEnemies.length > 0
                || !rc.isCoreReady()) {
            return;
        }

        if (roundNumber > Config.KILL_ROUND
                || currentLocation.distanceSquaredTo(enemyLocation) > SAFE_DISTANCE_FROM_ENEMY_BASE) {
            tryMoveToward(enemyLocation);
        }
    }

    private void shootEnemies() throws GameActionException {
        if (!rc.isWeaponReady()
                || attackableEnemies.length == 0) {
            return;
        }

        RobotInfo lowestHealthNonInfected = RobotUtil.getLowestHealthNonInfectedRobot(attackableEnemies);
        if (lowestHealthNonInfected == null) {
            return;
        }

        rc.attackLocation(lowestHealthNonInfected.location);
    }

    private void readBroadcasts() {
        roundSignals = rc.emptySignalQueue();

        if (enemyLocation == null) {
            enemyLocation = getEnemyLocation();
        }
    }

    private void senseRobots() {
        attackableEnemies = senseAttackableEnemies();
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

    private MapLocation getEnemyLocation() {
        for (Signal s : roundSignals) {
            if (s.getTeam() != team) continue;

            int[] message = s.getMessage();
            if (message == null) continue;

            MessageParser parser = new MessageParser(message[0], message[1], currentLocation);
            if (parser.getMessageType() == MessageType.ENEMY) {
                return parser.getRobotData().location;
            }
        }

        return null;
    }
}
