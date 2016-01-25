package tarball;

import battlecode.common.*;
import tarball.message.MessageParser;
import tarball.util.ZombieUtil;

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
        getTurretBroadcasts(roundSignals);
        senseRobots();
        getTarget();
        getEnemyLocation();
        if (!rc.isCoreReady()
                && !rc.isWeaponReady()) {
            attackTarget = null;
            return;
        }

        attackTargets();
        attackEnemiesAndZombies();
        if (moveToTarget()) {
            attackTarget = null;
            return;
        }

        moveToEnemy();
        if (moveRandom()) {
            attackTarget = null;
            return;
        }

        attackTarget = null;
    }

    private void senseRobots() {
        nearbyZombies = senseNearbyZombies();
        nearbyEnemies = senseNearbyEnemies();
        //setIndicatorString(0, "nearby zombies: " + nearbyZombies.length);
        //setIndicatorString(0, "nearby enemies: " + nearbyEnemies.length);
    }

    private void getTarget() {
        int[] message = getFirstMessageOfType(roundSignals, MessageType.TARGET);
        if (message == null) {
            return;
        }

        attackTarget = MessageParser.getRobotData(message);
        //setIndicatorString(2, "my target is " + attackTarget);
    }

    public void getEnemyLocation() {
        int[] message = getFirstMessageOfType(roundSignals, MessageType.ENEMY);
        if (message != null) {
            enemyLocation = MessageParser.getRobotData(message).location;
        }
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
            //setIndicatorString(0, "received target " + attackTarget + " but can't attack!");
        }
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

    private void moveToEnemy() throws GameActionException {
        if (nearbyEnemies.length > 0
                || enemyLocation == null
                || rc.getType() == RobotType.TURRET
                || !rc.isCoreReady()) {
            return;
        }

        setIndicatorString(0, "enemy turrets: ");
        for (int i = 0; i < enemyTurretCount; i++) {
            setIndicatorString(0, " " + enemyTurrets[i].location);
        }

        if (enemyTurretCount == 0) {
            tryMoveToward(enemyLocation);
        }
        else {
            trySafeMoveToward(enemyLocation, enemyTurrets);
        }
    }

    private boolean moveToTarget() throws GameActionException {
        if (attackTarget == null
                || !rc.isCoreReady()) {
            return false;
        }

        if (currentLocation.distanceSquaredTo(attackTarget.location) > RobotType.TURRET.attackRadiusSquared) {
            if (rc.getType() == RobotType.TTM) {
                Direction direction = currentLocation.directionTo(attackTarget.location);
                trySafeMove(direction, enemyTurrets);
            }
            else {
                rc.pack();
                return true;
            }
        }
        else if (rc.getType() == RobotType.TTM) {
            rc.unpack();
            return true;
        }

        return false;
    }

    private boolean moveRandom() throws GameActionException {
        if (attackTarget != null) {
            delayRound = roundNumber;
            return false;
        }

        if (rc.getType() == RobotType.TURRET
                && nearbyZombies.length == 0
                && nearbyEnemies.length == 0
                && rc.isCoreReady()
                && roundNumber - RANDOM_MOVE_DELAY > delayRound) {
            setIndicatorString(1, "packing for random move");
            rc.pack();
            return true;
        }

        if (rc.getType() == RobotType.TTM
                && (nearbyEnemies.length > 0
                || nearbyZombies.length > 0)) {
            setIndicatorString(1, "unpacking in random move");
            rc.unpack();
            return true;
        }

        if (!rc.isCoreReady()) {
            return false;
        }

        tryMove(getRandomDirection());
        return false;
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
}
