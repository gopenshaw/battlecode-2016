package melody;

import battlecode.common.*;
import melody.message.MessageParser;
import melody.util.ZombieUtil;

public class Turret extends Robot {
    private static final int RANDOM_MOVE_DELAY = 20;
    private Signal[] roundSignals;
    private RobotInfo[] nearbyZombies;
    private RobotInfo[] nearbyEnemies;
    private RobotData attackTarget;
    private MapLocation enemyLocation;
    private int delayRound;

    public Turret(RobotController rc) {
        super(rc);
    }

    @Override
    protected void doTurn() throws GameActionException {
        roundSignals = rc.emptySignalQueue();
        senseRobots();
        getTarget();
        getEnemyLocation();
        attackTargets();
        attackEnemiesAndZombies();
        moveToTarget();
        moveToEnemy();
        moveRandom();
        attackTarget = null;
    }

    private void moveToEnemy() throws GameActionException {
        if (enemyLocation == null
                || !rc.isCoreReady()) {
            return;
        }

        tryMoveToward(enemyLocation);
    }

    private void senseRobots() {
        nearbyZombies = senseNearbyZombies();
        nearbyEnemies = senseNearbyEnemies();
        setIndicatorString(0, "nearby zombies: " + nearbyZombies.length);
        setIndicatorString(0, "nearby enemies: " + nearbyEnemies.length);
    }

    private void moveToTarget() throws GameActionException {
        if (attackTarget == null
                || !rc.isCoreReady()) {
            return;
        }

        if (currentLocation.distanceSquaredTo(attackTarget.location) > RobotType.TURRET.attackRadiusSquared) {
            if (rc.getType() == RobotType.TTM) {
                tryMoveToward(attackTarget.location);
            }
            else {
                rc.pack();
                return;
            }
        }
        else if (rc.getType() == RobotType.TTM) {
            rc.unpack();
            return;
        }
    }

    private void getTarget() {
        MessageParser message = getParserForFirstMessageOfType(roundSignals, MessageType.TARGET);
        if (message == null) {
            setIndicatorString(2, "i have no target");
            return;
        }

        attackTarget = message.getRobotData();
        setIndicatorString(2, "my target is " + attackTarget);
    }

    private void attackTargets() throws GameActionException {
        if (attackTarget == null
                || rc.getType() == RobotType.TTM
                || !rc.isWeaponReady()) {
            return;
        }

        if (rc.canAttackLocation(attackTarget.location)) {
            rc.attackLocation(attackTarget.location);
        }
        else {
            setIndicatorString(0, "received target " + attackTarget + " but can't attack!");
        }
    }

    private void moveRandom() throws GameActionException {
        if (attackTarget != null) {
            delayRound = roundNumber;
            return;
        }

        if (rc.getType() == RobotType.TURRET
                && nearbyZombies.length == 0
                && nearbyEnemies.length == 0
                && rc.isCoreReady()
                && roundNumber - RANDOM_MOVE_DELAY > delayRound) {
            setIndicatorString(1, "packing for random move");
            rc.pack();
            return;
        }

        if (rc.getType() == RobotType.TTM
                && nearbyEnemies.length > 0
                || nearbyZombies.length > 0) {
            setIndicatorString(1, "unpacking in random move");
            rc.unpack();
            return;
        }

        if (!rc.isCoreReady()) {
            return;
        }

        tryMove(getRandomDirection());
    }

    private void attackEnemiesAndZombies() throws GameActionException {
        if (rc.getType() == RobotType.TTM
                || !rc.isWeaponReady()) {
            return;
        }

        RobotInfo zombieToAttack = getPriorityAttackableZombie(nearbyZombies);
        if (zombieToAttack != null) {
            rc.attackLocation(zombieToAttack.location);
            return;
        }

        if (nearbyEnemies.length > 0) {
            if (rc.canAttackLocation(nearbyEnemies[0].location)) {
                rc.attackLocation(nearbyEnemies[0].location);
                return;
            }
        }
    }

    private RobotInfo getPriorityAttackableZombie(RobotInfo[] zombies) {
        RobotInfo zombieToAttack = null;
        int highestPriority = -1;
        for (RobotInfo r : zombies) {
            int priority = ZombieUtil.getAttackPriority(r.type);
            if (priority > highestPriority
                    && rc.canAttackLocation(r.location)) {
                zombieToAttack = r;
                highestPriority = priority;
            }
        }

        return zombieToAttack;
    }

    public void getEnemyLocation() {
        MessageParser enemy = getParserForFirstMessageOfType(roundSignals, MessageType.ENEMY);
        if (enemy != null) {
            enemyLocation = enemy.getRobotData().location;
        }
    }
}
