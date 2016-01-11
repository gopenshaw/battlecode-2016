package bernie;

import battlecode.common.*;
import battlecode.common.Signal;
import bernie.message.MessageParser;
import bernie.util.DirectionUtil;
import bernie.util.RobotUtil;

public class Soldier extends Robot {
    private MapLocation enemyLocation;

    public Soldier(RobotController rc) {
        super(rc);
    }

    @Override
    public void doTurn() throws GameActionException {
        readSignals();

        tryAttackAndKite();

        tryMoveTowardEnemies();
    }

    private void tryMoveTowardEnemies() throws GameActionException {
        if (!rc.isCoreReady()) {
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

    private void tryAttackAndKite() throws GameActionException {
        RobotInfo[] attackableZombies = senseAttackableZombies();
        if (attackableZombies.length > 0) {
            if (rc.isWeaponReady()) {
                RobotInfo robotToAttack = RobotUtil.getLowestHealthRobot(attackableZombies);
                rc.attackLocation(robotToAttack.location);
            }
        }

        RobotInfo[] attackableEnemies = senseAttackableEnemies();
        if (attackableEnemies.length > 0) {
            if (rc.isWeaponReady()) {
                RobotInfo robotToAttack = RobotUtil.getLowestHealthRobot(attackableEnemies);
                rc.attackLocation(robotToAttack.location);
            }
        }

        if (!rc.isCoreReady()) {
            return;
        }

        if (RobotUtil.anyCanAttack(attackableZombies, currentLocation)) {
            tryMove(DirectionUtil.getDirectionAwayFrom(attackableZombies, rc));
        }
    }

    private void readSignals() {
        Signal[] signals = rc.emptySignalQueue();
        for (Signal s : signals) {
            if (s.getTeam() == team) {
                int[] message = s.getMessage();
                if (message == null) continue;
                MessageParser parser = new MessageParser(message[0], message[1], currentLocation);
                setIndicatorString(0, "message type " + parser.getMessageType());
                if (parser.getMessageType() == MessageType.ZOMBIE) {
                    enemyLocation = parser.getRobotData().location;
                    setIndicatorString(1, "id " + parser.getRobotData().id);
                    setIndicatorString(2, "health " + parser.getRobotData().health);
                }
            }
        }
    }
}
