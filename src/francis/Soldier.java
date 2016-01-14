package francis;

import battlecode.common.*;
import francis.message.MessageParser;
import francis.util.EnemyStatus;
import francis.util.RobotUtil;

public class Soldier extends Robot {
    private final EnemyStatus enemyStatus;
    private RobotData enemyToAttack;
    private RobotInfo[] attackableEnemies;
    private RobotInfo[] nearbyEnemies;
    private RobotInfo[] nearbyZombies;

    public Soldier(RobotController rc) {
        super(rc);
        enemyStatus = new EnemyStatus();
    }

    @Override
    public void doTurn() throws GameActionException {
        senseRobots();
        readSignals();
        checkIfEnemyOvertakenByZombies();
        attackEnemyRobots();
        tryMoveTowardEnemies();
    }

    private void senseRobots() {
        attackableEnemies = senseAttackableEnemies();
        nearbyEnemies = senseNearbyEnemies();
        nearbyZombies = senseNearbyZombies();
        setIndicatorString(0, "nr enemies: " + nearbyEnemies.length
                + " nr zom: " + nearbyZombies.length
                + " atk enemies: " + attackableEnemies.length);
    }

    private void checkIfEnemyOvertakenByZombies() throws GameActionException {
        if (enemyOvertaken(nearbyEnemies, nearbyZombies)) {
            setIndicatorString(1, "enemy overtaken");
        }

    }

    private void ignoreEnemies(RobotInfo[] nearbyEnemies, int numberOfRounds) {
        for (RobotInfo robot : nearbyEnemies) {
            enemyStatus.ignoreRobot(robot, roundNumber, numberOfRounds);
        }
    }

    private boolean enemyOvertaken(RobotInfo[] nearbyEnemies, RobotInfo[] nearbyZombies) {
        if (nearbyZombies.length == 0) {
            return false;
        }

        for (RobotInfo enemy : nearbyEnemies) {
            for (RobotInfo zombie : nearbyZombies) {
                if (RobotUtil.robotCanAttackZombie(enemy, zombie)) {
                    return false;
                }
            }
        }

        return true;
    }

    private void tryMoveTowardEnemies() throws GameActionException {
        if (attackableEnemies.length > 0
                || !rc.isCoreReady()) {
            return;
        }

        if (enemyToAttack != null
                && !enemyStatus.ignoring(enemyToAttack, roundNumber)) {
            MapLocation enemyLocation = enemyToAttack.location;
            if (rc.canSense(enemyLocation)
                    && rc.senseRobotAtLocation(enemyLocation) == null) {
                enemyToAttack = null;
            }
            else {
                tryMoveToward(enemyLocation);
            }
        }
    }

    private void attackEnemyRobots() throws GameActionException {
        if (attackableEnemies.length > 0) {
            if (rc.isWeaponReady()) {
                RobotInfo robotToAttack = RobotUtil.getLowestHealthRobot(attackableEnemies);
                rc.attackLocation(robotToAttack.location);
            }
        }

        if (!rc.isCoreReady()) {
            return;
        }
    }

    private void readSignals() {
        Signal[] signals = rc.emptySignalQueue();
        for (Signal s : signals) {
            if (s.getTeam() == team) {
                int[] message = s.getMessage();
                if (message == null) continue;
                MessageParser parser = new MessageParser(message[0], message[1], currentLocation);
                if (parser.getMessageType() == MessageType.ENEMY) {
                    enemyToAttack = parser.getRobotData();
                }
            }
        }
    }
}
