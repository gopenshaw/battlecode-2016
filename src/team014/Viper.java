package team014;

import battlecode.common.*;
import team014.message.MessageParser;
import team014.util.RobotUtil;

public class Viper extends Robot {
    private Signal[] roundSignals;
    private RobotInfo[] attackableEnemies;
    private MapLocation enemyLocation;
    private int hasAdvantageRound;

    public Viper(RobotController rc) {
        super(rc);
    }

    @Override
    protected void doTurn() throws GameActionException {
        readBroadcasts();
        senseRobots();
        shootEnemies();
        moveTowardEnemy();
        moveAwayFromArchon();
    }

    private void moveTowardEnemy() throws GameActionException {
        setIndicatorString(0, "enemy location is " + enemyLocation);
        if (enemyLocation == null
                || attackableEnemies.length > 0
                || !rc.isCoreReady()) {
            return;
        }

        int distanceToEnemy = currentLocation.distanceSquaredTo(enemyLocation);
        setIndicatorString(0, "distance to enemy " + distanceToEnemy);
        if (shouldEngage()
                || distanceToEnemy > Config.SAFE_DISTANCE_FROM_ENEMY_BASE) {
            tryMoveToward(enemyLocation);
        }
    }

    private boolean shouldEngage() {
        if (hasAdvantageRound == 0
                && enemyLocation != null
                && rc.getTeamParts() < 60) {
            hasAdvantageRound = roundNumber;
        }

        setIndicatorString(1, "advantage round " + hasAdvantageRound);
        return hasAdvantageRound != 0
                && roundNumber - Config.ENGAGE_DELAY > hasAdvantageRound;
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
