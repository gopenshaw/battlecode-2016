package francis;

import battlecode.common.*;
import francis.message.MessageParser;
import francis.util.DirectionUtil;
import francis.util.RobotUtil;

public class Viper extends Robot {
    private MapLocation enemyLocation;
    private RobotInfo[] attackableEnemies;

    public Viper(RobotController rc) {
        super(rc);
    }

    @Override
    public void doTurn() throws GameActionException {
        senseRobots();
        readSignals();
        attackEnemies();
        tryMoveTowardEnemies();
    }

    private void senseRobots() {
        attackableEnemies = senseAttackableEnemies();
    }

    private void tryMoveTowardEnemies() throws GameActionException {
        if (attackableEnemies.length > 0
                || !rc.isCoreReady()) {
            return;
        }

        if (enemyLocation != null) {
            if (rc.canSense(enemyLocation)
                    && rc.senseRobotAtLocation(enemyLocation) == null) {
                enemyLocation = null;
            }
            else {
                tryMoveToward(enemyLocation);
            }
        }
    }

    private void attackEnemies() throws GameActionException {
        if (!rc.isWeaponReady()) {
            return;
        }

        RobotInfo robotToAttack = RobotUtil.getLowestHealthNonInfectedRobot(attackableEnemies);
        if (robotToAttack == null) {
            robotToAttack = RobotUtil.getLowestHealthRobot(attackableEnemies);
        }

        if (robotToAttack == null) {
            return;
        }

        rc.attackLocation(robotToAttack.location);
    }

    private void readSignals() {
        Signal[] signals = rc.emptySignalQueue();
        for (Signal s : signals) {
            if (s.getTeam() == team) {
                int[] message = s.getMessage();
                if (message == null) continue;
                MessageParser parser = new MessageParser(message[0], message[1], currentLocation);
                setIndicatorString(0, "message type " + parser.getMessageType());
                if (parser.getMessageType() == MessageType.ZOMBIE
                        || parser.getMessageType() == MessageType.ENEMY) {
                    enemyLocation = parser.getRobotData().location;
                    setIndicatorString(1, "id " + parser.getRobotData().id);
                    setIndicatorString(2, "health " + parser.getRobotData().health);
                }
            }
        }
    }
}
